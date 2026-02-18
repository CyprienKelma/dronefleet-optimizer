# Optimiseur de trajectoire

Le **Path Optimizer** est l'intelligence centrale du projet — un service d'optimisation par lots qui résout le problème de tournées de véhicules avec ramassage et livraison (VRPPD). Implémenté en Python 3.11, il s'appuie sur **Google OR-Tools**.

### Modèle d'exécution

Le service fonctionne comme un **processus éphémère sans état** (stateless), déclenché toutes les 10 secondes via Cloud Run Jobs en production (ou via un conteneur en local). Chaque exécution constitue un cycle complet :

1. **Récupération** de l'état actuel (snapshot) auprès du Gestionnaire d'état via HTTP GET.
2. **Construction** du modèle mathématique du problème de tournées.
3. **Résolution** du modèle sous contraintes physiques et métier.
4. **Publication** des affectations de missions résultantes sur Pub/Sub.

L'optimiseur lit tout ce dont il a besoin dans le snapshot et produit ses sorties exclusivement via Pub/Sub — pas d'accès direct à la base de données.

```
API Ingestion --> Pub/Sub --> State Manager --> Firestore
                                    |
                          HTTP GET /snapshot
                                    |
                             Path Optimizer
                                    |
                          Pub/Sub (decisions)
                                    |
                             State Manager --> Firestore (missions)
```

### Classification du problème

Le problème résolu est un **Vehicle Routing Problem with Pickup and Delivery (VRPPD)** — variante NP-difficile classique du VRP. Dans notre contexte :

- Les **Véhicules** sont les drones (hétérogènes par leur batterie).
- Les **Ramassages** (Pickups) se font aux entrepôts.
- Les **Livraisons** (Deliveries) se font aux hôpitaux (destination finale).
- Chaque drone part et revient au même **dépôt**.

Contraintes additionnelles :
- **Fenêtres de temps** sur les livraisons (échéances prioritaires : CRITIQUE 15 min, HAUTE 30 min, STANDARD 60 min).
- **Contraintes de batterie** (proportionnelle à la distance, 2,5 % par km, réserve min 20 %).
- **Compatibilité Produit-Entrepôt** (l'entrepôt doit stocker le type de produit demandé).

### Construction du graphe VRP

Le graphe contient `2N + 1` nœuds pour N commandes :

```
Index 0          : Dépôt (départ/arrivée pour tous les véhicules)
Indices 1..N     : Nœuds de ramassage (un par commande, à l'entrepôt compatible le plus proche)
Indices N+1..2N  : Nœuds de livraison (un par commande, à la destination finale)
```

**Décision de conception critique — Nœuds de ramassage uniques par commande :** Chaque commande possède son propre nœud de ramassage dédié, même si plusieurs commandes sont collectées au même entrepôt physique. C'est impératif car `AddPickupAndDelivery(p, d)` dans OR-Tools exige une relation 1-à-1 entre ramassage et livraison. Partager un nœud entrepôt rendrait le problème infaisable.

Pour chaque commande, le constructeur choisit l'**entrepôt compatible le plus proche** (distance Haversine). Deux matrices `(2N+1) × (2N+1)` sont calculées :
- **Matrice de distance** (mètres) : distances Haversine entre tous les nœuds.
- **Matrice de temps** (secondes) : dérivée avec une vitesse constante de 50 km/h.

### Dimensions et contraintes du solveur

Le solveur utilise l'API `RoutingModel` d'OR-Tools, exprimant le problème via des **dimensions** (variables cumulatives le long d'un parcours) :

| Dimension | Callback de transit | Limite supérieure | Rôle |
|-----------|-----------------|-------------|---------|
| **Distance** | Mètres entre nœuds | 100 km | Minimise la distance totale (évaluateur de coût d'arc). Un coefficient global encourage une répartition équitable. |
| **Temps** | Secondes entre nœuds | 3 heures (10 800s) | Impose les fenêtres de temps. Permet une attente aux nœuds jusqu'à 30 min. Le cumul au départ est libre. |
| **Batterie** | `dist_km × 2.5 × 10` (scalé pour précision entière) | 1 000 unités (100%) | Limite par drone : `(batterie_initiale% - 20%) × 10`. Exemple : 85 % -> max 650 unités -> 26 km. |

Les **contraintes ramassage-livraison** assurent que la collecte et la livraison d'une commande sont faites par le même drone, dans le bon ordre. Les **disjonctions** (pénalité de 100 000 par nœud) permettent d'ignorer les commandes impossibles plutôt que de bloquer tout le cycle.

### Choix de l'algorithme : Stratégie en deux phases

![Optimizer Diagram](images/path_optimizer.png)

Le VRPPD étant **NP-difficile**, OR-Tools utilise des heuristiques :

#### Phase 1 : Heuristique constructive (PARALLEL_CHEAPEST_INSERTION)
Un algorithme glouton qui insère simultanément sur tous les véhicules au meilleur endroit pour minimiser l'augmentation du coût. Rapide (millisecondes).

#### Phase 2 : Amélioration par métaheuristique (GUIDED_LOCAL_SEARCH)
Améliore itérativement la solution (relocate, swap, move segments) tout en évitant les optima locaux. C'est un **algorithme anytime** : plus il tourne, meilleure est la solution. Limite : **10 secondes**.

#### Comparaison de complexité

| Approche | Complexité temporelle | Optimalité | Usage pratique |
|----------|----------------|------------|---------------|
| Exacte (Branch-and-Bound) | O(n! / symétries) | Prouvée optimale | Limité à n < 20-30 |
| MILP | Expo (pire cas) | Prouvée optimale | Limité à n < 50-100 |
| Heuristique Voisin Proche | O(n²) | 15-25% de l'optimal | Très rapide, basse qualité |
| PARALLEL_CHEAPEST_INSERTION | O(n² × V) | 10-20% de l'optimal | Rapide, qualité raisonnable |
| **Métaheuristique GLS (choix actuel)** | **O(n²) par itération, borné par le temps** | **1-5% de l'optimal** | **Meilleur compromis vitesse/qualité** |
| Algorithmes génétiques | O(P × n² × G) | Variable | Convergence plus lente pour le VRP |

### Extraction de la solution

Après résolution, le `SolutionExtractor` construit les messages `MissionAssignment` :
1. Parcourt la route de chaque véhicule du dépôt au retour au dépôt.
2. Classifie chaque nœud : `DEPOT_START`, `WAREHOUSE_PICKUP`, `HOSPITAL_DELIVERY`, `DEPOT_RETURN`.
3. Calcule les métriques agrégées (distance, temps, batterie).
4. Ignore les drones inactifs (trajet dépôt-dépôt uniquement).

Route typique pour commande simple : `DEPOT_START → WAREHOUSE_PICKUP → HOSPITAL_DELIVERY → DEPOT_RETURN`

Route multi-commandes :
```
DEPOT_START → WAREHOUSE_PICKUP → HOSPITAL_DELIVERY
            → WAREHOUSE_PICKUP → HOSPITAL_DELIVERY
            → DEPOT_RETURN
```

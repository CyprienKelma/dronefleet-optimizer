# Flux de données

### Flux d'ingestion de la télémétrie et des commandes

```
┌─────────────┐
│  Simulateur │ (Génère la télémétrie des drones + les commandes)
└──────┬──────┘
       │ HTTP POST
       v
┌─────────────────┐
│ API d'ingestion │ (FastAPI - Validation avec Pydantic)
│  Cloud Run      │
└────────┬────────┘
         │ Publication Pub/Sub
         ├──────────────────┬──────────────────┐
         v                  v                  v
    [télémétrie]       [commandes]       [décisions]
         │                  │                  │
         │                  │                  │
         v                  v                  v
┌────────────────────────────────────────────────┐
│   Gestionnaire d'état (Java/Spring Boot)       │
│  - TelemetryListener   - OrderListener         │
│  - DecisionListener    - API REST              │
└───────────────────┬────────────────────────────┘
                    │ Transactions Firestore
                    v
            ┌───────────────┐
            │  Firestore DB │
            │  Collections: │
            │  - drones     │
            │  - orders     │
            │  - missions   │
            │  - warehouses │
            └───────────────┘
```

#### Détails du flux de télémétrie

1. Le **Simulateur** génère la position du drone, le niveau de batterie et son statut.
2. L'**API d'ingestion** valide le contenu par rapport au schéma Pydantic.
3. **Pub/Sub** livre le message au topic `telemetry`.
4. Le `TelemetryListener` du **Gestionnaire d'état** consomme le message.
5. Une **Transaction Firestore** met à jour le document du drone avec :
   - Ordonnancement par horodatage (rejet des messages obsolètes).
   - Position update (GeoPoint).
   - Battery level update.
   - Validation de la transition de statut.

#### Détails du flux de commande

1. Le **Simulateur** génère une demande de livraison (type de produit, priorité, lieu).
2. L'**API d'ingestion** valide le contenu de la commande.
3. **Pub/Sub** livre le message au topic `orders`.
4. Le `OrderListener` du **Gestionnaire d'état** consomme le message.
5. Une **Transaction Firestore** crée/met à jour le document de la commande avec :
   - Protection d'idempotence (évite d'écraser des commandes déjà traitées).
   - Statut : `PENDING`
   - Calcul de l'échéance basé sur la priorité.

### Cycle d'optimisation

Le cycle logique global de la partie optimisation du système est représenté par :
<img src="images/optimization-cycle.png" alt="Cycle Logique d'Optimisation" width="1100" height="500" />

Le flux complet détaillé :
```
┌──────────────────┐
│ Cloud Scheduler  │ (Déclenchement toutes les 10 secondes)
└────────┬─────────┘
         │ HTTP POST (appel du Cloud Run Job)
         v
┌─────────────────────────────────────────────────┐
│     Optimiseur de trajectoire (Python/OR-Tools) │
│                                                 │
│  1. GET /api/v1/optimizer/snapshot              │
│     ├─ Récupère les drones IDLE (batterie > 20%)│
│     ├─ Récupère les commandes PENDING           │
│     ├─ Récupère les entrepôts + dépôt           │
│     └─ Retourne OptimizationSnapshot            │
│                                                 │
│  2. Construction du modèle VRP (builder.py)     │
│     ├─ Crée le graphe (dépôt, ramassages,       │
│     │  livraisons)                              │
│     ├─ Calcule matrices distance/temps          │
│     │  (Haversine)                              │
│     ├─ Associe commandes aux entrepôts compatibles│
│     └─ Définit fenêtres de temps par priorité   │
│                                                 │
│  3. Résolution du VRP (solver.py)               │
│     ├─ Dimension Distance (minimise le trajet)  │
│     ├─ Dimension Temps (respecte les échéances) │
│     ├─ Dimension Batterie (modèle consommation) │
│     ├─ Contraintes ramassage-livraison          │
│     └─ Disjonctions (permet d'ignorer les       │
│        commandes infaisables)                   │
│                                                 │
│  4. Extraction de la solution (extractor.py)    │
│     ├─ Parcourt l'itinéraire de chaque véhicule │
│     ├─ Classifie les points (START, PICKUP,     │
│     │  DELIVERY, RETURN)                        │
│     ├─ Calcule métriques (batterie, durée)     │
│     └─ Génère messages MissionAssignment        │
│                                                 │
│  5. Publication des décisions                   │
│     └─ Topic Pub/Sub : decisions                │
└─────────────────────────────────────────────────┘
         │
         v
   Topic Pub/Sub [decisions]
         │
         v
┌─────────────────────────────────────────────────┐
│      State Manager - DecisionListener           │
│                                                 │
│  DÉBUT Transaction Firestore :                  │
│    1. Lecture du drone (vérifie IDLE)           │
│    2. Lecture commandes (vérifie PENDING)       │
│    3. Validation via MissionAssignmentPolicy    │
│    4. Création du document Mission              │
│    5. Update drone.status = MOVING              │
│    6. Update orders.status = ASSIGNED           │
│  COMMIT (atomique, tout ou rien)                │
│                                                 │
│  Si échec validation (concurrence) :            │
│    - Transaction annulée                        │
│    - Business Rejection Exception levée         │
│    - Entités reprises au prochain cycle         │
└─────────────────────────────────────────────────┘
```

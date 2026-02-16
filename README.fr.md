# DroneFleet Optimizer

[![English](https://img.shields.io/badge/Language-English-gray?style=for-the-badge)](README.md)
[![Français](https://img.shields.io/badge/Langue-Français-blue?style=for-the-badge)](README.fr.md)

## Qu'est-ce que ce repo ?
Ce projet est un système complet de gestion cloud en temps réel pour des flottes de drones de livraison médicale d'urgence.

Il est basé sur une architecture pilotée par les événements (event-driven) déployée sur GCP. Il inclut un pipeline CI/CD complet, un simulateur de données et un pipeline ELT pour traiter et analyser les données via BigQuery.

Il s'agit d'un projet personnel réalisé lors de ma dernière année d'études d'ingénieur en informatique, visant à mettre en pratique les concepts technologiques qui me passionnent le plus.

Mon objectif ultime était de concevoir et d'implémenter une infrastructure de données de bout en bout : de la génération de données (simulation d'un système source) à l'ingestion, la résolution de problèmes de recherche opérationnelle, la gestion des flux en temps réel, jusqu'à une architecture "médaillon" pour le nettoyage, la transformation et l'analyse des données.

Ce projet m'a permis d'approfondir ma maîtrise de concepts tels que la gestion de la concurrence, la conteneurisation, les architectures événementielles, l'organisation en monorepo, le CI/CD et le déploiement cloud.

Vous pouvez tester le projet gratuitement en suivant les étapes de [Mise en Place](#mise-en-place), ou poursuivre votre lecture pour découvrir son fonctionnement, les choix techniques effectués et mes réflexions sur la conception d'un tel système.

<img src="docs/images/drone_map_gif_demo.gif" alt="Description" width="900" height="600" />

## Table des matières

- [Vue d'ensemble](#vue-densemble)
- [Architecture](#architecture)
- [Pile technologique](#pile-technologique)
- [Mise en place](#mise-en-place)
- [Flux de données](#flux-de-données)
  - [Flux d'ingestion de la télémétrie et des commandes](#flux-dingestion-de-la-télémétrie-et-des-commandes)
  - [Cycle d'optimisation](#cycle-doptimisation)
  - [Gestion de la concurrence et des conditions de concurrence (Race Conditions)](#gestion-de-la-concurrence-et-des-conditions-de-concurrence-race-conditions)
- [Système d'optimisation de trajectoire](#système-doptimisation-de-trajectoire)
- [Composants du système](#composants-du-système)
- [Structure du dépôt](#structure-du-dépôt)
- [Configuration](#configuration)
- [Développement](#développement)
- [Déploiement](#déploiement)
- [Tests](#tests)
- [Décisions de conception](#décisions-de-conception)
- [Travaux en cours](#travaux-en-cours)
- [Licence](#licence)

## Vue d'ensemble

DroneFleet Optimizer est une plateforme logistique autonome capable de livrer des fournitures médicales d'urgence (sang, vaccins, défibrillateurs) en moins de 15 minutes en zone urbaine, en coordonnant une flotte de drones via un algorithme d'optimisation centralisé.

### Contexte métier

Le système répond à des défis critiques de logistique médicale :

- Optimiser les itinéraires de livraison pour 50 à 100 drones simultanés.
- Respecter des SLAs stricts : commandes critiques livrées en moins de 15 minutes, haute priorité en moins de 30 minutes.
- Gérer les contraintes en temps réel : niveaux de batterie, fenêtres de temps, compatibilité entre entrepôts et produits.
- Garantir la cohérence des données et la tolérance aux pannes entre les composants distribués.

### Métriques clés

- **Latence** : < 500ms pour les mises à jour en temps réel.
- **Cycle d'optimisation** : Planification sur un horizon roulant de 10 secondes.
- **Échelle** : 50 à 100 drones actifs en phase MVP.
- **Fiabilité** : Garantie de livraison "au moins une fois" (at-least-once) sans perte de commande.
- **Optimisation des coûts** : Écritures par lots (batch) dans Firestore pour rester dans le niveau gratuit pendant le développement.

[↑ Retour en haut](#table-des-matières)

## Architecture

Le système implémente une **architecture de microservices polyglotte** suivant le modèle hexagonal pour l'indépendance de l'infrastructure :

![Diagramme d'architecture](docs/images/global_architecture_png.png)

### Principes architecturaux

- **Piloté par les événements (Event-Driven)** : Le bus de messages Pub/Sub découple les composants.
- **Polyglotte** : Python (FastAPI), Java (Spring Boot) et TypeScript (SolidJS) sont utilisés selon leur pertinence pour chaque tâche.
- **Architecture Hexagonale** : La logique métier est isolée de l'infrastructure via des ports et des adaptateurs.
- **Cloud-Native** : Conçu pour Google Cloud Platform avec support de l'émulation locale.
- **Infrastructure as Code** : Définition complète via Terraform pour des déploiements reproductibles.

### Environnements

```
LOCAL → DEV → PROD
```

- **LOCAL** : Docker Compose avec les émulateurs Pub/Sub et Firestore (coût GCP nul).
- **DEV** : Services GCP complets avec déploiement automatique lors d'un push sur la branche `main`.
- **PROD** : Environnement de production avec déploiement manuel via des tags de version.

## Pile technologique

| Composant | Technologie | Infrastructure | Rôle |
|-----------|-----------|----------------|---------|
| **API d'ingestion** | Python 3.11 / FastAPI | Cloud Run (Service) | Passerelle pour la télémétrie et les commandes, validation JSON, publication Pub/Sub |
| **Bus de messages** | Google Pub/Sub | Pub/Sub / Émulateur | Distribution asynchrone des événements avec gestion des messages non distribués (DLQ) |
| **Gestionnaire d'état** | Java 21 / Spring Boot 4 | Cloud Run (Service) | Cohérence de l'état, transactions Firestore, génération de snapshots |
| **Optimiseur** | Python 3.11 / OR-Tools | Cloud Run (Job) | Solveur de problème de tournées de véhicules (VRP) avec contraintes de ramassage et livraison |
| **Base de données** | Firestore Native | Firestore / Émulateur | Stockage à chaud pour l'état en temps réel (drones, commandes, missions) |
| **Frontend** | TypeScript / SolidJS | Cloud Run (Service) | Visualisation de la carte en temps réel (WebSocket) |
| **Analytique** | BigQuery | BigQuery | Entrepôt de données historiques (en cours de développement) |

### Couche de modèles partagés

Tous les composants partagent une source unique de vérité pour les modèles de données via **Protocol Buffers** :

- Définitions dans `shared/proto/dronefleet/v1/*.proto`.
- Code généré pour Java, Python et TypeScript.
- Validation via Buf (linting, détection de changements cassants).
- Synchronisation automatisée via des hooks de pré-commit et le CI/CD.

[↑ Retour en haut](#table-des-matières)

## Mise (mise en place)

### Prérequis

- **Docker** et Docker Compose.
- **Mise** (gestionnaire de versions d'outils polyglotte) - [Installation](https://mise.jdx.dev/).
- **uv** (gestionnaire de paquets Python) - Installé via mise.
- **Buf** (outillage Protobuf) - Installé via mise.
- **Java 21** (distribution Temurin).
- **Bun** (runtime TypeScript).

### Configuration locale

1. **Cloner le dépôt**

```bash
git clone https://github.com/votreutilisateur/drone-fleet-optimizer.git
cd drone-fleet-optimizer
```

2. **Installer les outils via mise**

```bash
mise install
```

3. **Générer les modèles partagés à partir des définitions Protobuf**

```bash
mise run //shared/proto:generate
```

4. **Lancer l'infrastructure avec Docker Compose**

```bash
cd infra/local
docker-compose up -d --build
```

Ceci démarre :
- L'émulateur Pub/Sub (port 8085)
- L'émulateur Firestore (port 8080)

5. **Créer les topics Pub/Sub**

```bash
mise run //infra/local:create-topics
```

6. **Démarrer les services (dans des terminaux séparés)**

```bash
# API d'ingestion
cd services/ingestion
mise run dev

# Gestionnaire d'état (State Manager)
cd services/state_manager
./gradlew bootRun --args='--spring.profiles.active=local'

# Optimiseur de trajectoire (déclenchement manuel pour test)
cd services/path_optimizer
mise run start

# Simulateur
cd services/simulators
mise run dev
```

7. **Vérifier que le système fonctionne**

Consultez l'interface de l'émulateur Firestore : http://localhost:4000
Consultez la documentation de l'API d'ingestion : http://localhost:8000/docs

[↑ Retour en haut](#table-des-matières)

## Flux de données

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
<img src="docs/images/optimization-cycle.png" alt="Cycle Logique d'Optimisation" width="1100" height="500" />

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

### Gestion de la concurrence et des conditions de concurrence (Race Conditions)

En tant que système distribué utilisant une messagerie asynchrone, DroneFleet Optimizer fait face à des défis de concurrence inhérents. Plusieurs composants interagissent simultanément : l'Optimiseur lance des cycles toutes les 10 secondes, le Gestionnaire d'état traite télémétrie, commandes et décisions de concert, Pub/Sub livre sans garantie d'ordre, et Firestore sert de source unique de vérité. Le système utilise une **cohérence éventuelle pour les lectures** combinée à une **cohérence forte lors de l'écriture** pour garantir l'exactitude sans sacrifier la performance.

#### Le défi majeur : Cycles d'optimisation concurrents

La principale condition de concurrence survient lorsque deux cycles d'optimisation se chevauchent et incluent le même drone ou la même commande dans leurs snapshots :

```
Chronologie :
=============

T0 : Le cycle A démarre, appelle getSnapshot()
    -> Le drone D1 est IDLE -> inclus dans le snapshot A

T1 : Le cycle B démarre, appelle getSnapshot()
    -> Le drone D1 est TOUJOURS IDLE -> inclus dans le snapshot B
    (A n'a pas encore fini, donc le statut de D1 est inchangé)

T2 : Le cycle A calcule la solution -> Assigne D1 à la commande O1
T3 : Le cycle B calcule la solution -> Assigne D1 à la commande O2

T4 : Le cycle A publie la décision (D1 -> O1)
T5 : Le Gestionnaire d'état traite la décision de A
    -> Transaction : D1 est IDLE ? OUI
    -> SUCCÈS : D1.status = MOVING, Mission M1 créée

T6 : Le cycle B publie la décision (D1 -> O2)
T7 : Le Gestionnaire d'état traite la décision de B
    -> Transaction : D1 est IDLE ? NON (il est MOVING)
    -> REJET : BusinessRejectionException levée
    -> La commande O2 reste PENDING pour le prochain cycle
```

Le système empêche correctement la double affectation grâce à la **validation au moment de l'écriture** au sein d'une transaction Firestore.

#### Stratégie "Le premier qui écrit gagne" (First-Write-Wins)

Le modèle de résolution de conflit est basé sur l'ordre de validation (commit), pas sur l'ordre de démarrage. C'est un choix de conception délibéré :

- **Mise en œuvre plus simple** : Pas de gestion de verrous distribués ni de système de réservation.
- **Aucun risque de deadlock** : En l'absence de verrouillage pessimiste, l'interblocage est impossible.
- **Gaspillage acceptable** : Les cycles tournant toutes les 10 secondes et la résolution prenant ~8 secondes, les chevauchements sont peu fréquents. Une décision occasionnellement rejetée est récupérée naturellement au cycle suivant.

Une approche alternative (verrouillage pessimiste via des états `RESERVED`/`SOLVING`) a été envisagée. Bien qu'elle réduirait les calculs inutiles, elle introduirait une complexité significative : gestion des verrous, nettoyage des sessions en cas de crash, etc.

#### Modèle de Transaction Firestore : Affectation de Mission (Chemin Critique)

L'affectation de mission est la transaction la plus complexe. Elle valide et applique les décisions de manière atomique sur plusieurs documents :

```java
firestore.runTransaction(transaction -> {
    // Lecture de TOUS les documents d'abord (requis par Firestore)
    DocumentSnapshot droneDoc = transaction.get(droneRef).get();
    List<DocumentSnapshot> orderDocs = /* lire toutes les commandes */;

    // Conversion en objets de domaine
    Drone drone = FirestoreMapper.toDrone(droneDoc);
    List<Order> orders = /* convertir toutes les commandes */;

    // Exécution de la logique métier (MissionAssignmentPolicy)
    //   - drone.status == IDLE (DronePolicy.canAcceptMission)
    //   - tous les status de commandes == PENDING
    //   - Si UNE SEULE validation échoue -> BusinessRejectionException

    // Écriture atomique de tous les changements
    transaction.set(missionRef, missionData);       // Création de la Mission
    transaction.update(droneRef, droneUpdates);      // drone.status = MOVING
    for (Order order : orders) {
        transaction.update(orderRef, orderUpdates);  // order.status = ASSIGNED
    }
    return result;
});
```

**Propriétés clés :**
- Toutes les lectures précèdent les écritures (requis par Firestore pour la concurrence optimiste).
- Si un document a été modifié entre la lecture et la validation, Firestore retente automatiquement toute la transaction.
- Soit tout réussit, soit rien ne s'applique (atomicité).
- Pour les missions multi-commandes, si la validation échoue pour une seule commande, toute la transaction est rejetée — le drone reste IDLE et toutes les commandes restent PENDING.

#### Verrouillage Optimiste : Gestion de la contention par Firestore

Firestore implémente nativement le **contrôle de concurrence optimiste**. Lorsque deux transactions tentent de modifier le même document :

1. La transaction A lit le drone D1 (statut = IDLE), la transaction B lit le drone D1 (statut = IDLE).
2. La transaction A valide d'abord → SUCCÈS : D1.status = MOVING.
3. La transaction B tente de valider.
4. Firestore détecte que D1 a été modifié depuis la lecture de B.
5. Firestore **retente automatiquement** la transaction B depuis le début.
6. La transaction B relit D1 (désormais statut = MOVING).
7. La validation `MissionAssignmentPolicy` échoue → `BusinessRejectionException`.
8. La décision rejetée est logguée et les entités concernées seront reprises au prochain cycle d'optimisation.

C'est une forme de **verrouillage optimiste** — il n'y a pas d'acquisition explicite de verrou. Les conflits sont détectés lors du commit et résolus par retry. La `MissionAssignmentPolicy` fait office de garde-fou métier.

#### Protection de l'ordre de la télémétrie

Les conditions réseau peuvent inverser l'ordre des messages de télémétrie. Le Gestionnaire d'état protège les données contre l'obsolescence via la comparaison des horodatages :

```java
firestore.runTransaction(transaction -> {
    DocumentSnapshot doc = transaction.get(droneRef).get();
    if (doc.exists()) {
        Instant existingTimestamp = /* récupérer lastUpdate depuis doc */;
        Instant incomingTimestamp = telemetry.getTimestamp();
        if (incomingTimestamp.isBefore(existingTimestamp)) {
            return null;  // Ignorer la télémétrie obsolète — ne pas appliquer de vieilles données
        }
    }
    Drone updated = DronePolicy.applyTelemetryUpdate(drone, telemetry);
    transaction.set(droneRef, FirestoreMapper.toMap(updated));
    return updated;
});
```

Si un message T1 (10:00:01) arrive après T2 (10:00:02), T1 est ignoré silencieusement. L'état du drone reflète toujours la donnée la plus récente.

#### Idempotence de l'ingestion de commandes

Pub/Sub garantit une livraison "au moins une fois", ce qui signifie qu'un même message peut être livré plusieurs fois. L'ingestion de commandes inclut un garde-fou d'idempotence :

```java
firestore.runTransaction(transaction -> {
    DocumentSnapshot doc = transaction.get(orderRef).get();
    if (doc.exists()) {
        OrderStatus currentStatus = /* statut du document */;
        if (currentStatus != PENDING && currentStatus != UNSPECIFIED) {
            return null;  // Ne pas écraser - commande déjà traitée
        }
    }
    Order order = /* construire commande avec statut PENDING */;
    transaction.set(orderRef, FirestoreMapper.toMap(order));
    return order;
});
```

Cela empêche un message redélivré de repasser une commande de `ASSIGNED` à `PENDING`, ce qui risquerait de créer des missions en double.

#### Résumé des mécanismes de protection

| Mécanisme | Emplacement | Protection fournie |
|-----------|----------|---------------------|
| Transaction Firestore | `FirestoreStateTransactionAdapter` | Écritures atomiques multi-documents |
| Concurrence Optimiste | Natif Firestore | Retry automatique en cas de conflit |
| Validation à l'écriture | `MissionAssignmentPolicy` | Vérification des statuts avant affectation |
| Garde Drone Status | `DronePolicy.canAcceptMission()` | Seuls les drones IDLE acceptent des missions |
| Garde Order Status | `MissionAssignmentPolicy` | Seules les commandes PENDING sont affectées |
| Ordre Temporel | `runTelemetryUpdateTransaction` | Rejet de télémétrie obsolète |
| Garde d'idempotence | `runOrderIngestionTransaction` | Empêche de réinitialiser une commande déjà traitée |

#### Algorithme d'optimisation : VRP avec Ramassage et Livraison

L'optimiseur résout un **problème de tournées de véhicules multi-voyages avec fenêtres de temps (VRPTW)** via Google OR-Tools :

**Caractéristiques du problème :**
- Complexité **NP-difficile** (pas de solution optimale en temps polynomial).
- Flotte hétérogène (drones avec différents niveaux de batterie).
- Couplage Ramassage-Livraison (Entrepôt → Hôpital pour chaque commande).
- Fenêtres de temps (échéances basées sur la priorité).
- Contraintes de batterie (modèle de consommation énergétique).

**Stratégie de résolution :**
- **Phase 1** : Heuristique constructive (Parallel Cheapest Insertion) - O(n² × V).
- **Phase 2** : Amélioration par métaheuristique (Guided Local Search) - Limite de 30 secondes.
- **Résultat** : Solutions quasi-optimales (généralement entre 1 et 5% de l'optimal théorique).

**Contraintes clés :**
- Chaque commande : ramassage à l'entrepôt compatible, livraison à l'hôpital, par le même drone.
- Batterie : 2,5 % de consommation par km, réserve minimale de 20 % au retour.
- Fenêtres de temps : CRITIQUE (15 min), HAUTE (30 min), STANDARD (60 min).
- Capacité : Colis unique (plusieurs cycles ramassage-livraison par mission).

[↑ Retour en haut](#table-des-matières)

## Système d'optimisation de trajectoire

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

Le VRPPD étant **NP-difficile**, OR-Tools utilise des heuristiques :

#### Phase 1 : Heuristique constructive (PARALLEL_CHEAPEST_INSERTION)
Un algorithme glouton qui insère simultanément sur tous les véhicules au meilleur endroit pour minimiser l'augmentation du coût. Rapide (millisecondes).

#### Phase 2 : Amélioration par métaheuristique (GUIDED_LOCAL_SEARCH)
Améliore itérativement la solution (relocate, swap, move segments) tout en évitant les optima locaux. C'est un **algorithme anytime** : plus il tourne, meilleure est la solution. Limite : **30 secondes**.

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

### Modèle de données

**Entrée (OptimizationSnapshot) :**

| Champ | Type | Description |
|-------|------|-------------|
| `session_id` | string | Identifiant unique du cycle d'optimisation |
| `timestamp` | Timestamp | Moment de création du snapshot |
| `depot` | Depot | Dépôt principal (départ/arrivée) |
| `drones` | List[Drone] | Drones IDLE disponibles avec position, batterie, taux de conso |
| `orders` | List[Order] | Commandes PENDING avec lieu, priorité, type de produit |
| `warehouses` | List[Warehouse] | Lieux de collecte avec types de produits autorisés |

**Sortie (MissionAssignment) :**

| Champ | Type | Description |
|-------|------|-------------|
| `drone_id` | string | Le drone affecté |
| `order_ids` | List[string] | Commandes traitées dans cette mission |
| `route` | List[Waypoint] | Séquence ordonnée de points de passage (type, position, refs) |
| `estimated_battery_consumption` | double | Consommation estimée (%) |
| `estimated_duration_minutes` | double | Durée de vol estimée |

### Performances pratiques

Avec les données initiales (5 drones, 18 commandes, 2 entrepôts = 37 nœuds), le solveur trouve une solution de haute qualité bien avant la limite des 30s. Le système est conçu pour passer à la cible MVP de 50-100 drones avec des centaines de commandes en ajustant la limite de temps ou par partition géographique.

[↑ Retour en haut](#table-des-matières)

## Composants du système

### API d'ingestion

**Localisation** : `services/ingestion/`

**Responsabilités :**
- Point d'entrée HTTP REST pour la télémétrie et les commandes.
- Validation Pydantic des payloads entrants.
- Publication sur Pub/Sub avec gestion d'erreurs.
- Point d'entrée Health Check.

**Technologie** : FastAPI (Python 3.11), serveur ASGI uvicorn.

**Fichiers clés :**
- `src/ingestion/api/v1/endpoints/telemetry.py` - Endpoint télémétrie.
- `src/ingestion/api/v1/endpoints/orders.py` - Endpoint commandes.
- `src/ingestion/services/` - Logique métier et éditeurs Pub/Sub.

### Gestionnaire d'état (State Manager)

**Localisation** : `services/state_manager/`

**Responsabilités :**
- Consommer les événements Pub/Sub (télémétrie, commandes, décisions).
- Maintenir la cohérence de l'état via des transactions Firestore.
- Fournir les snapshots pour l'optimisation.
- Implémenter les politiques métier (disponibilité, affectation de mission).

**Technologie** : Java 21, Spring Boot 4, SDK Google Cloud Firestore.

**Architecture** : Hexagonale (Ports et Adaptateurs).
- **Couche Domaine** : Logique métier et politiques.
- **Couche Application** : Cas d'utilisation et DTOs.
- **Couche Infrastructure** : Adaptateurs Firestore, listeners Pub/Sub, contrôleurs REST.

**Fichiers clés :**
- `domain/service/MissionAssignmentPolicy.java` - Logique de validation des missions.
- `domain/service/OptimizationSnapshotService.java` - Génération des snapshots.
- `infrastructure/adapter/in/messaging/pubsub/` - Listeners d'événements.
- `infrastructure/adapter/out/persistence/firestore/` - Adaptateurs de base de données.

**Gestion de la concurrence :**
- Transactions Firestore pour les écritures atomiques.
- Verrouillage optimiste avec retry automatique.
- Validation à l'écriture (first-write-wins).
- Ordonnancement temporel pour la télémétrie.
- Idempotence pour l'ingestion de commandes.

### Optimiseur de trajectoire (Path Optimizer)

**Localisation** : `services/path_optimizer/`

**Responsabilités :**
- Récupérer le snapshot d'état.
- Construire et résoudre le problème VRP.
- Publier les affectations de mission sur le topic `decisions`.

**Technologie** : Python 3.11, Google OR-Tools, calcul distance Haversine.

**Fichiers clés :**
- `main.py` - Point d'entrée et orchestration.
- `services/builder.py` - Construction du modèle VRP.
- `services/solver.py` - Configuration du solveur OR-Tools.
- `services/extractor.py` - Parsing de solution et classification des points.
- `clients/state_manager.py` - Client HTTP pour le snapshot.
- `clients/publisher.py` - Éditeur Pub/Sub pour les décisions.

**Modèle d'exécution** : Job Cloud Run sans état déclenché par Cloud Scheduler (Intervalle de 10s).

### Simulateur

**Localisation** : `services/simulators/`

**Responsabilités :**
- Générer des données de télémétrie synthétiques (mouvements drones).
- Générer des commandes de livraison fictives.
- Consommer les affectations de mission (en cours).
- Exécuter les missions en publiant la télémétrie le long du trajet.

**Technologie** : Python 3.11, asyncio pour la simulation concurrente.

**Statut** : Génération de télémétrie et commandes implémentée, consommation de missions en cours.

### Visualiseur Frontend

**Localisation** : `services/visualizer/`

**Responsabilités :**
- Affichage de la carte des drones en temps réel.
- Serveur WebSocket pour le streaming de télémétrie.
- Visualisation des trajets de mission.
- Dashboard de métriques (batterie, missions actives).

**Technologie** : TypeScript, SolidJS, Vite, Leaflet, runtime Bun.

**Statut** : Travaux en cours.

[↑ Retour en haut](#table-des-matières)

## Structure du dépôt

### Gestion du monorepo avec Mise

Le dépôt est organisé comme un **monorepo polyglotte** géré avec [**mise**](https://mise.jdx.dev/). Mise gère :

- **Versions des outils** : Python, Java, Node.js, Terraform, Buf, etc., fixées dans le `mise.toml` racine.
- **Variables d'environnement** : Chargement automatique des fichiers `.env` de `configs/`.
- **Orchestration des tâches** : Chaque service possède son propre `mise.toml` pour ses tâches (dev, lint, build), la racine proposant des agrégateurs (`test:all`, `lint:all`).
- **Auto-activation d'environnements virtuels** : Les services Python créent/activent automatiquement leurs `.venv` via `uv`.

```
mise.toml                          # Racine : outils, env vars, agrégateurs
├── services/ingestion/mise.toml   # Python : dev, lint, format, build
├── services/state_manager/mise.toml  # Java : dev, lint, format, build, test
├── services/path_optimizer/mise.toml # Python : start, lint
├── services/simulators/mise.toml  # Python : run, lint
├── services/visualizer/mise.toml  # TypeScript : dev, build
├── shared/proto/mise.toml         # Protobuf : lint, format, generate, breaking
└── infra/local/mise.toml          # Docker Compose : up, down, logs
```

Tâches invoquées via la syntaxe de chemin monorepo :
- Racine : `mise run <tâche>` (ex: `mise run lint:all`)
- Service : `mise //<chemin>:<tâche>` (ex: `mise //services/ingestion:lint`)

### Organisation des dossiers

```
dronefleet-optimizer/
├── services/                    # Microservices indépendants
│   ├── ingestion/               # Python/FastAPI — Passerelle HTTP
│   ├── state_manager/           # Java/Spring Boot — Persistance Firestore
│   ├── path_optimizer/          # Python/OR-Tools — Optimisation VRP
│   ├── simulators/              # Python — Génération de données
│   └── visualizer/              # TypeScript/SolidJS — Dashboard temps réel
│
├── shared/                      # Définitions partagées
│   ├── proto/                   # Source de vérité Protobuf (.proto)
│   ├── java/                    # Modèles Java générés
│   ├── python/                  # Modèles Python générés + utilitaires
│   └── ts/                      # Modèles TypeScript générés
│
├── libs/                        # Bibliothèques internes réutilisables
│   ├── python/
│   │   ├── config/              # Configuration partagée (pydantic-settings)
│   │   ├── logging/             # Setup logging structuré (JSON)
│   │   └── messaging/           # Abstraction de publication (Factory + Adapter)
│   ├── java/ ...
│   └── ts/ ...
│
├── configs/                     # Fichiers de configuration par environnement (.env)
│
├── infra/
│   ├── local/                   # Docker Compose pour les émulateurs locaux
│   └── terraform/               # IaC : modules Cloud Run, Pub/Sub, Firestore, IAM
│
├── tests/                       # Tests transverses (unit, intégration, e2e)
│
└── docs/                        # Documentation et diagrammes d'architecture
```

### Modèles partagés via Protocol Buffers + Buf

Tous les modèles de données partagés sont définis avec **Protocol Buffers** dans `shared/proto/dronefleet/v1/`. C'est l'unique source de vérité pour :
- Entités Drone, Commande, Mission, Entrepôt.
- Messages d'événements (télémétrie, décisions).
- Énumérations (DroneStatus, OrderStatus, etc.).

Le CLI [**Buf**](https://buf.build/) gère le workflow :
- **`buf lint`** : Impose un style cohérent.
- **`buf format`** : Formate les fichiers `.proto`.
- **`buf generate`** : Génère le code typé pour Java, Python et TS.
- **`buf breaking`** : Détecte les changements incompatibles.

Code généré placé dans `shared/java/`, `shared/python/` et `shared/ts/`. Cela garantit qu'aucun décalage n'existe entre un DTO Python et un DTO Java.

### Bibliothèque de messagerie : Pattern Factory + Adapter

La bibliothèque `libs/python/messaging/` abstrait le bus de messages via un design pattern **Factory + Adapter** :

- **`on_cloud`** : Utilise `PubSubPublisher` (connecte à GCP Pub/Sub ou l'émulateur via `PUBSUB_EMULATOR_HOST`).
- **`on_premise`** : Utilise `KafkaPublisher` (pour des déploiements hypothétiques sur site).

Cette séparation permet au système de fonctionner dans trois modes sans modifier le code, simplement en ajustant la variable d'environnement chargée par mise.

[↑ Retour en haut](#table-des-matières)

## Configuration

### Variables d'environnement

Chaque service lit sa configuration depuis les variables d'environnement chargées via les fichiers `.env` dans `configs/` :
- `configs/local.env` - Développement local avec émulateurs.
- `configs/dev.env` - Environnement de développement GCP.
- `configs/prod.env` - Environnement de production GCP.

| Variable | Description | Défaut (local) |
|----------|-------------|-----------------|
| `ENVIRONMENT` | Environnement de déploiement | `local` |
| `PROJECT_ID` | Identifiant du projet GCP | `local-emulator` |
| `PUBSUB_EMULATOR_HOST` | Adresse de l'émulateur Pub/Sub | `localhost:8085` |
| `FIRESTORE_EMULATOR_HOST` | Adresse de l'émulateur Firestore | `localhost:8080` |
| `STATE_MANAGER_URL` | URL de base du Gestionnaire d'état | `http://localhost:8080` |

### Configuration Terraform

L'infrastructure est gérée via Terraform avec un état séparé par environnement :

```
infra/terraform/
├── environments/
│   ├── dev/
│   └── prod/
└── modules/
    ├── cloud-run/
    ├── pubsub/
    ├── firestore/
    └── iam/
```

[↑ Retour en haut](#table-des-matières)

## Développement

### Standards de qualité de code

**Services Python :** Linting avec `ruff`, types avec `mypy` (mode strict), formatage `ruff format`, tests `pytest` avec couverture.

**Services Java :** Linting Checkstyle (Style Google), formatage Spotless, tests JUnit 5.

**Services TypeScript :** Linting et formatage Biome, mode strict TypeScript.

### Hooks de pré-commit
Le dépôt utilise des hooks `pre-commit` pour assurer la synchronisation des modèles Protobuf, le formatage, le linting et les conventions de messages de commit.

```bash
pre-commit install
```

### Tests

**Tests Unitaires :**

```bash
# Services Python
cd services/ingestion
uv run pytest tests/

# Services Java
cd services/state_manager
./gradlew test
```

**Tests d'Intégration :** Situés dans `tests/integration/` — testent les flux complets avec émulateurs.

**Tests End-to-End :** Situés dans `tests/e2e/` — testent le système complet avec flotte de drones simulée.

[↑ Retour en haut](#table-des-matières)

## Déploiement

### Pipeline CI/CD

Utilisation de GitHub Actions avec deux workflows :

**1. Intégration Continue (`.github/workflows/ci.yml`)**
Déclenché sur PR et push vers main. Vérifie le Protobuf, lance les tests unitaires et linting par service, build Docker dry-run et validation Terraform.

**2. Déploiement Continu (`.github/workflows/cd-dev.yml`)**
Déclenché sur push main ou manuel. Applique Terraform, build et push les images Docker sur Artifact Registry, déploie sur Cloud Run/Cloud Run Jobs et configure Cloud Scheduler.

### Déploiement manuel (GCP Dev)

```bash
# Authentification
gcloud auth login
gcloud config set project drone-fleet-optimizer-dev

# Infrastructure
cd infra/terraform/environments/dev
terraform init
terraform apply

# Image (exemple Ingestion)
docker build -t europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest \
  -f services/ingestion/Dockerfile .
docker push europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest

# Cloud Run
gcloud run deploy ingestion \
  --image europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/ingestion:latest \
  --region europe-west1 \
  --platform managed
```

### Monitoring et Observabilité

- **Loggin** : Logs JSON structurés via Cloud Logging.
- **Metrics** : Métriques natives Cloud Run (latence, erreurs), Firestore et Pub/Sub.
- **Alertes** : Alertes budgétaires via Terraform, surveillance DLQ et taux d'erreurs élevé.

[↑ Retour en haut](#table-des-matières)

## Décisions de conception

### Pourquoi une architecture polyglotte ?
J'ai choisi **Python pour l'API d'ingestion et l'Optimiseur** car ces services ont des besoins très différents. FastAPI est idéal pour les charges asynchrones à haut débit sans blocage. Concernant l'optimiseur, Google OR-Tools est la référence absolue pour les problèmes de tournées avec un support Python de premier plan. Plutôt que de forcer un langage unique, j'utilise l'outil le plus adapté à chaque tâche.

**Java propulse le Gestionnaire d'état** car c'est là que réside la logique métier complexe et critique. Le typage fort de Java et ses garanties à la compilation évitent des catégories entières de bugs. La gestion des transactions Spring Boot et la maturité du SDK Firestore en font le choix naturel pour un service qui doit être ultra-robuste.

**TypeScript gère le Frontend** car la gestion d'état UI réactive en temps réel est une priorité. SolidJS offre une réactivité fine — seules les parties du DOM qui changent sont rerendus — ce qui est crucial pour afficher des milliers de mises à jour de position par seconde.

### Pourquoi Firestore plutôt que PostgreSQL ?
Firestore a été choisi pour ce problème spécifique :
- **Géographique** : Le type natif `GeoPoint` facilite les requêtes spatiales sans gérer de colonnes lat/lon manuellement.
- **Scalabilité** : Mise à l'échelle automatique sans administration de base de données.
- **Atomicité** : Les transactions Firestore sont puissantes et intégrées au SDK.
Le compromis est une capacité de requête moins riche que le SQL (pas de joins complexes), mais les patterns d'accès du Gestionnaire d'état (lecture/écriture individuelle ou requêtes simples indexées) correspondent exactement aux forces de Firestore. Le coût est optimisé via des écritures par lots.

### Modèle de concurrence : First-Write-Wins
J'ai fait un compromis délibéré entre simplicité et efficacité de calcul. J'ai choisi le **"Le premier qui écrit gagne" avec verrouillage optimiste**. Un verrouillage pessimiste (`RESERVED`/`SOLVING`) éliminerait le gaspillage de calcul mais introduirait une complexité énorme (gestion de verrous distribués, timeouts, risque de deadlock). Vu que les cycles d'optimisation sont courts, rejeter quelques décisions occasionnellement est un prix acceptable pour la robustesse et la simplicité du système.

### Piloté par les événements vs requête-réponse
Le système utilise une stratégie hybride.
**Télémétrie et commandes via Pub/Sub** : flux asynchrones à haute fréquence qui n'exigent pas de réponse immédiate. Cela découple producteurs et consommateurs et offre un tampon naturel.
**Snapshots d'optimisation via HTTP GET synchrone** : L'Optimiseur a besoin d'une vue cohérente à un instant T. Le HTTP GET est plus direct, plus simple à gérer avec des retries et permet de "fail fast".

[↑ Retour en haut](#table-des-matières)

## Travaux en cours

### 1. Visualisation Frontend (En développement)
**Techno** : SolidJS + Leaflet + WebSocket. Carte temps réel, indicateurs de batterie, trajet des missions et dashboard de métriques.

### 2. Pipeline Analytique BigQuery (Prévu)
**Objectif** : Entrepôt de données historiques.
**Architecture** : Abonnements Pub/Sub to BigQuery pour insertion en direct, puis transformations dbt pour générer des vues "gold" sur la performance des drones, le respect des SLAs et l'efficacité des entrepôts.

### 3. Simulation d'exécution de mission (En développement)
Le simulateur va s'abonner au topic `decisions`, parser les `MissionAssignment`, et simuler le mouvement réel le long du trajet en publiant la télémétrie correspondante.

[↑ Retour en haut](#table-des-matières)

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.

[↑ Retour en haut](#table-des-matières)

---

**Statut du projet** : Développement actif. Moteur d'optimisation et gestionnaire d'état terminés. Visualisation frontend et pipeline analytique en cours.

**Contact** : Pour toute question ou collaboration, merci d'ouvrir une issue sur GitHub.

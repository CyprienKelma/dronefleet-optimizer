# Documentation de l'Optimizer et Choix Architecturaux

> **Note :** Ce document résume les réflexions et décisions prises concernant le module Optimizer (OR-Tools) et son intégration dans l'architecture globale.

## 1. Analyse du Problème d'Optimisation

Le cas d'usage correspond à un **PDPTW (Pickup and Delivery Problem with Time Windows)**, une variante du VRP classique.

| Contrainte | Description | Impact sur l'algo |
|------------|-------------|-------------------|
| **Pickup & Delivery** | Chaque mission a un point de collecte (warehouse) puis un point de livraison (hôpital) | Nécessite des contraintes de précédence |
| **Flotte homogène** | Tous les drones ont les mêmes specs (capacité, vitesse) | Simplifie le modèle |
| **Capacité unitaire** | 1 drone = 1 colis à la fois | Contraint le séquencement |
| **Batterie limitée** | Autonomie ~15-30 min typiquement | Contrainte de distance/temps max par mission |
| **Rolling Horizon** | Résoudre toutes les 10s avec nouvelles données | Nécessite un algo rapide et incrémentel |

## 2. Options d'Algorithmes avec OR-Tools choisi

### OR-Tools Routing Library
**VRP**

*   **Avantages :**
    *   Spécifiquement conçu pour les VRP
    *   Supporte nativement le PDPTW
    *   Très rapide (solutions en < 1 seconde pour 100 drones)
    *   Local Search avec métaheuristiques intégrées
    *   Support des contraintes de capacité, temps, distance
*   **Inconvénients :**
    *   Solution heuristique (pas garantie optimale)
    *   Moins flexible que CP-SAT pour contraintes exotiques

**Modèle type :**
```python
from ortools.constraint_solver import routing_enums_pb2
from ortools.constraint_solver import pywrapcp

def create_data_model(drones, orders, warehouses):
    """
    Nodes: [depot_0, ..., depot_n, pickup_1, delivery_1, ..., pickup_m, delivery_m]
    """
    data = {}
    data['num_vehicles'] = len(drones)
    data['vehicle_capacities'] = [1] * len(drones)  # 1 colis max par drone
    data['pickups_deliveries'] = [
        (pickup_idx, delivery_idx) for order in orders
    ]
    data['time_matrix'] = compute_time_matrix(...)
    data['time_windows'] = compute_time_windows(...)
    return data
```

## 3. Questions de Design à Trancher

### Q1 : Retour à la base ou Redéploiement ?

| Option | Description | Complexité |
|--------|-------------|------------|
| **Return-to-Depot** | Drone retourne toujours à sa base après livraison | Simple, VRP classique |
| **Free-Floating** | Drone peut rester sur place et accepter une nouvelle mission | Plus réaliste, nécessité ré-optimisation continue |
| **Multi-Depot** | Plusieurs bases/hubs de recharge | MDVRP (plus complexe) |

**Recommandation : Multi-Depot simplifié.**
*   Plus intéressant pour le portfolio et plus réaliste.
*   Chaque drone a un "home_depot_id".
*   OR-Tools le supporte nativement (`starts` et `ends` différents).
*   *Alternative MVP :* Depot = Warehouse (fusionnés).

````
Modele simplifie Multi-Depot :
- Chaque drone a un "home_depot_id" (sa base de rattachement)
- Plusieurs warehouses peuvent exister dans une region
- Chaque warehouse stocke certains types de produits
- Le drone PART de son depot, va au warehouse le plus proche qui a le produit,
  livre au client, puis RETOURNE a son depot

┌─────────────────────────────────────────────────────────┐
│ Depot A          Warehouse 1           Client/Hospital  │
│ (Home base)      (Pickup)              (Delivery)       │
│     H ─────────────> P ─────────────────> D             │
│      <─────────────────────────────────────             │
│                   (Return to depot)                     │
└─────────────────────────────────────────────────────────┘
```

### Q2 : Gestion de la Batterie

| Option | Description |
|--------|-------------|
| **Pré-filtrage** | Ne considérer que les drones avec batterie > seuil |
| **Contrainte modèle** | Intégrer conso batterie comme contrainte temps/distance |
| **Recharge dynamique** | Permettre arrêts recharge (très complexe) |

**Recommandation : Pré-filtrage avec Map de consommation.**
*   Utiliser une config statique (YAML/Env) pour les specs des modèles de drones.
*   Logique : `drone_eligible = (battery_current - conso_estimee) > MIN_BATTERY_RESERVE`.

**Exemple Config :**
```
DroneModelSpecs (configuration statique, pas en DB):
├── LIGHT_DELIVERY:
│   ├── consumption_percent_per_km: 2.5  (perd 2.5% par km)
│   ├── consumption_percent_per_minute_hover: 1.0
│   └── safety_margin: 1.3  (x1.3 sur la conso estimee)
│
├── HEAVY_LIFT:
│   ├── consumption_percent_per_km: 4.0
│   ├── consumption_percent_per_minute_hover: 1.5
│   └── safety_margin: 1.4
│
└── LONG_RANGE:
    ├── consumption_percent_per_km: 1.5
    ├── consumption_percent_per_minute_hover: 0.8
    └── safety_margin: 1.2
````
Logique de pre-filtrage dans l'optimizer :
(La config des specs par modele devrait etre en configuration (fichier YAML ou env vars), pas en base de donnees).
````
Pour une mission (pickup -> delivery -> return):
  distance_totale = dist(drone, pickup) + dist(pickup, delivery) + dist(delivery, depot)
  conso_estimee = distance_totale * consumption_per_km * safety_margin

  drone_eligible = (battery_current - conso_estimee) > MIN_BATTERY_RESERVE (ex: 15%)
````

### Q3 : Priorités des Commandes

**Recommandation : Approche Hybride.**

1.  **Priorité CRITICAL :**
    *   **Contrainte DURE :** Doit être assigné dans les 30 premières secondes.
    *   Si aucun drone dispo -> Alerte.
    *   Réserver N drones (ex: 10%) exclusivement pour CRITICAL.
2.  **Priorités HIGH / STANDARD / LOW :**
    *   **Pénalités différenciées** dans la fonction objectif.
    *   Pénalité retard = `temps_attente * coefficient_priorite` (ex: HIGH=100, STANDARD=10, LOW=1).

## 4. Architecture de l'Optimizer (Enterprise-Grade)

```
┌──────────────────────────────────────────────────────────────┐
│                    OPTIMIZER SERVICE                          │
│                    (Python + OR-Tools)                        │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 1. SNAPSHOT ACQUISITION                                  │ │
│  │    - Fetch drones (IDLE ou RESERVED_FOR_SOLVING)        │ │
│  │    - Fetch orders (PENDING ou SOLVING)                  │ │
│  │    - Fetch warehouses (positions des depots)            │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          │                                    │
│                          ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 2. PROBLEM BUILDER                                       │ │
│  │    - Construire la matrice de distances/temps           │ │
│  │    - Définir les contraintes (capacité, batterie, TW)   │ │
│  │    - Configurer la fonction objectif                    │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          │                                    │
│                          ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 3. SOLVER                                                │ │
│  │    - OR-Tools Routing avec limite de temps (8 sec)      │ │
│  │    - Stratégies: PARALLEL_CHEAPEST_INSERTION            │ │
│  │    - Local Search: GUIDED_LOCAL_SEARCH                  │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          │                                    │
│                          ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 4. SOLUTION EXTRACTOR                                    │ │
│  │    - Extraire les assignations drone -> order           │ │
│  │    - Calculer les routes (waypoints)                    │ │
│  │    - Générer les MissionAssignment DTOs                 │ │
│  └─────────────────────────────────────────────────────────┘ │
│                          │                                    │
│                          ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 5. DECISION PUBLISHER                                    │ │
│  │    - Publier sur topic "decisions" (Pub/Sub)            │ │
│  │    - 1 message par assignation drone/order              │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## 5. Mécanisme de Locking/Reservation (Crucial)

Pour éviter les race conditions, utilisation de statuts transactionnels dans le State Manager (Java).

**Order Status Flow:**
`PENDING` -> (Optimizer fetch) -> `SOLVING` -> (Decision) -> `ASSIGNED` -> `DELIVERED`
*(Rollback à PENDING si timeout/échec)*

**Drone Status Flow:**
`IDLE` -> (Optimizer fetch) -> `RESERVED` -> (Decision) -> `MOVING` -> `IDLE`
*(Rollback à IDLE si timeout/échec)*

**Schémas**
````
Order Status Flow:
  PENDING ─────────────────────────────────────────────────────────┐
     │                                                              │
     │ [Optimizer fetche le snapshot]                               │
     ▼                                                              │
  SOLVING ─────────────────────────────────────────────────────────┤
     │            │                                                 │
     │ [Decision] │ [Timeout 30s ou echec]                          │
     ▼            ▼                                                 │
  ASSIGNED    PENDING (rollback)                                    │
     │                                                              │
     │ [Delivery confirmed]                                         │
     ▼                                                              │
  DELIVERED ────────────────────────────────────────────────────────┘


Drone Status Flow:
  IDLE ────────────────────────────────────────────────────────────┐
     │                                                              │
     │ [Optimizer fetche le snapshot]                               │
     ▼                                                              │
  RESERVED ────────────────────────────────────────────────────────┤
     │            │                                                 │
     │ [Decision] │ [Timeout 30s ou echec]                          │
     ▼            ▼                                                 │
  MOVING       IDLE (rollback)                                      │
     │                                                              │
     │ [Mission complete]                                           │
     ▼                                                              │
  IDLE ─────────────────────────────────────────────────────────────┘
  ````


**Transaction Firestore (State Manager) :**
L'acquisition du snapshot (`acquireSnapshot`) doit être atomique :
1.  Lire drones IDLE et orders PENDING.
2.  Marquer comme RESERVED/SOLVING avec un `sessionId`.
3.  Retourner le snapshot.

````
// Port IN pour l'optimizer
public interface GetOptimizationSnapshotUseCase {
    /**
     * Atomically fetch available drones and pending orders,
     * marking them as SOLVING/RESERVED.
     * Returns a consistent snapshot for optimization.
     */
    OptimizationSnapshot acquireSnapshot(String solvingSessionId);
}

public interface ReleaseUnassignedResourcesUseCase {
    /**
     * Release drones/orders that were reserved but not assigned
     * after optimization completes.
     */
    void releaseUnassigned(String solvingSessionId, List<String> assignedOrderIds);
}

...

// Dans StateTransactionPort
OptimizationSnapshot runSnapshotAcquisitionTransaction(String sessionId) {
    // 1. Lire tous les drones IDLE avec batterie > 20%
    // 2. Lire toutes les orders PENDING
    // 3. Marquer les drones comme RESERVED + sessionId
    // 4. Marquer les orders comme SOLVING + sessionId
    // 5. Retourner le snapshot
    // (Tout en une seule transaction Firestore)
}
````


## 6. Concepts de Région, Entrepôt et Dépôt

Architecture proposée pour structurer géographiquement le système.

```
┌───────────────────────────────────────────────────────────────────┐
│                        REGION "Paris"                              │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ Metadata:                                                    │  │
│  │ - id: "region-paris"                                        │  │
│  │ - name: "Paris et Petite Couronne"                          │  │
│  │ - bounding_box: {ne: {lat, lon}, sw: {lat, lon}}           │  │
│  │ - timezone: "Europe/Paris"                                  │  │
│  │ - operational_hours: {start: "06:00", end: "22:00"}        │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌──────────────────┐  ┌──────────────────┐                       │
│  │ Depot "Nord"     │  │ Depot "Sud"      │                       │
│  │ - id: "depot-n"  │  │ - id: "depot-s"  │                       │
│  │ - region_id      │  │ - region_id      │                       │
│  │ - position       │  │ - position       │                       │
│  │ - capacity: 20   │  │ - capacity: 15   │ (nb drones max)       │
│  │ - charging_slots │  │ - charging_slots │                       │
│  └──────────────────┘  └──────────────────┘                       │
│                                                                    │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐ │
│  │ Warehouse "W1"   │  │ Warehouse "W2"   │  │ Warehouse "W3"   │ │
│  │ - id             │  │ - id             │  │ - id             │ │
│  │ - region_id      │  │ - region_id      │  │ - region_id      │ │
│  │ - position       │  │ - position       │  │ - position       │ │
│  │ - products: [A,B]│  │ - products: [C]  │  │ - products: [A,C]│ │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘ │
│                                                                    │
│  Drones:                                                           │
│  [D001: depot-n] [D002: depot-n] [D003: depot-s] ...              │
│                                                                    │
└───────────────────────────────────────────────────────────────────┘
```

**Séparation Dépôt vs Warehouse :**
*   **Dépôt** : Base de rattachement du drone (recharge, maintenance).
*   **Warehouse** : Lieu de stockage des produits (pickup).
*   *Recommandation :* Les séparer pour le réalisme, même si relation 1:1 au début.

**REST vs Pub/Sub pour Reference Data :**
*   **Données chaudes (Telemetry, Orders, Missions)** -> **Pub/Sub**
*   **Données froides/Réf (Regions, Depots, Warehouses, Drones)** -> **REST API**

## 7. Structure à adopter pour l'Optimizers

```
src/optimizer/
├── main.py              # Point d'entrée (Cloud Run Job)
├── config.py            # Configuration et variables d'environnement
│
├── models/              # Modèles de données et DTOs
│   ├── drone.py
│   ├── order.py
│   ├── snapshot.py      # Modèle pour le snapshot d'entrée
│   └── decision.py      # Modèle pour les décisions de sortie
│
├── services/            # Logique métier et Solver
│   ├── builder.py       # Construction du problème (OR-Tools)
│   ├── solver.py        # Coeur du solveur (OR-Tools Routing)
│   └── extractor.py     # Extraction et formatage de la solution
│
└── clients/             # Communications externes
    ├── state_manager.py # Client HTTP pour le State Manager
    └── publisher.py     # Client Pub/Sub pour les décisions
```

Architecture REST pour les Reference Data :
````
src/ingestion/api/v1/
├── endpoints/
│   ├── orders.py         # POST order -> Pub/Sub
│   │
│   └── admin/            # REST CRUD pour reference data
│       ├── regions.py    # GET/POST/PUT/DELETE /admin/regions
│       ├── depots.py     # GET/POST/PUT/DELETE /admin/depots
│       ├── warehouses.py # GET/POST/PUT/DELETE /admin/warehouses
│       └── drones.py     # GET/POST/PUT /admin/drones (register drone)

````


## 8. Considérations Enterprise-Grade

*   **Observabilité (OpenTelemetry) :**
    *   Métriques système (latence, erreurs) vs Métriques métier (positions).
    *   Métriques Optimizer : `solve_duration_ms`, `orders_assigned`, `drones_utilized`.
*   **Résilience :** Timeout solver, Retries Pub/Sub, DLQ.
*   **Testabilité :** Tests unitaires Problem Builder, tests intégration OR-Tools.


## Schéma logique :
````mermaid
sequenceDiagram
    participant Scheduler as Cloud Scheduler (10s)
    participant Optimizer as Optimizer (Python)
    participant SM as State Manager (Java)
    participant Firestore as Firestore DB
    participant PubSub as Pub/Sub

    Scheduler->>Optimizer: Trigger
    Optimizer->>SM: GET /api/v1/optimizer/snapshot
    SM->>Firestore: Transaction: Read IDLE drones + PENDING orders
    SM->>Firestore: Mark as RESERVED/SOLVING
    SM-->>Optimizer: OptimizationSnapshot (drones, orders, warehouses)
    Optimizer->>Optimizer: Build VRP problem + Solve (OR-Tools)
    Optimizer->>PubSub: Publish decisions to "decisions" topic
    PubSub->>SM: DecisionListener receives MissionAssignment
    SM->>Firestore: Transaction: Create mission, update drone/order status
    SM->>Firestore: Rollback RESERVED->IDLE, SOLVING->PENDING
````

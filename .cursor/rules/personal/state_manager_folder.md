```
src/state-manager/src/main/java/com/dronefleet/statemanager/
├── StateManagerApplication.java         # Point d'entrée Spring Boot
│
├── domain/                               # DOMAIN LAYER (Core Business Logic)
│   ├── model/                            # Les Entités et Règles Métier
│   │   ├── Drone.java
│   │   ├── Order.java
│   │   ├── Mission.java
│   │   ├── Position.java
│   │   └── DroneStatus.java (enum)
│   │
│   ├── port/                             # Interfaces (Ports Hexagonaux)
│   │   ├── in/                           # Ports entrants (Use Cases)
│   │   │   ├── UpdateDroneStateUseCase.java
│   │   │   ├── ProcessOrderUseCase.java
│   │   │   ├── GetFleetSnapshotUseCase.java
│   │   │   └── AssignMissionUseCase.java
│   │   │
│   │   └── out/                          # Ports sortants (Infrastructure)
│   │       ├── DroneRepository.java
│   │       ├── OrderRepository.java
│   │       ├── MissionRepository.java
│   │       └── StatePublisher.java
│   │
│   └── service/                          # Business Logic (implémente Use Cases)
│       ├── DroneStateService.java
│       ├── OrderProcessingService.java
│       └── MissionAssignmentService.java
│
├── application/                          # APPLICATION LAYER
│   ├── config/                           # Configuration Spring
│   │   ├── AppConfig.java
│   │   ├── PubSubConfig.java
│   │   └── FirestoreConfig.java
│   │
│   └── dto/                              # DTOs (Data Transfer Objects)
│       ├── TelemetryEventDto.java
│       ├── OrderEventDto.java
│       ├── CommandEventDto.java
│       └── FleetSnapshotDto.java
│
└── infrastructure/                       # INFRASTRUCTURE LAYER
    ├── adapter/
    │   ├── in/                           # Adaptateurs entrants
    │   │   ├── rest/                     # REST API
    │   │   │   ├── DroneController.java
    │   │   │   ├── OrderController.java
    │   │   │   └── HealthController.java
    │   │   │
    │   │   └── messaging/                # Pub/Sub Listeners
    │   │       ├── TelemetryListener.java
    │   │       ├── OrderListener.java
    │   │       └── CommandListener.java
    │   │
    │   └── out/                          # Adaptateurs sortants, ce qui sort
    │       ├── persistence/              # Repositories Firestore
    │       │   ├── FirestoreDroneRepository.java
    │       │   ├── FirestoreOrderRepository.java
    │       │   └── FirestoreMissionRepository.java
    │       │
    │       └── messaging/                # Différents Publishers, ici un seul pour l'instant avec Pub/Sub
    │           └── PubSubStatePublisher.java
    │
    └── config/                           # Configurations Infrastructure
        ├── GcpConfig.java
        └── ConcurrencyConfig.java        # Virtual Threads, Executors

````

```
┌────────────────────────────────────────────────────┐
│              ARCHITECTURE EN 3 COUCHES             │
├────────────────────────────────────────────────────┤
│                                                    │
│  COUCHE 1 : DOMAIN (Le Cœur)                       │
│  ┌──────────────────────────────────────┐          │
│  │ Logique métier PURE                  │          │
│  │ - Entités (Drone, Order, Mission)    │          │
│  │ - Règles métier (isAvailable(), etc.)│          │
│  │ - Interfaces (Ports IN et OUT)       │          │
│  │                                      │          │
│  │ Dépendances : ZÉRO                   │          │
│  │ - Pas de Spring                      │          │
│  │ - Pas de Firestore                   │          │
│  │ - Pas de Pub/Sub                     │          │
│  │ - Seulement Java pur                 │          │
│  └──────────────────────────────────────┘          │
│              ▲              ▲                      │
│              │              │                      │
│    Utilise   │              │  Implémente          │
│              │              │                      │
│  COUCHE 2 : APPLICATION                            │
│  ┌───────────┴──────────────┴───────────┐          │
│  │ Coordination & Configuration         │          │
│  │ - DTOs (objets de transport)         │          │
│  │ - Config Spring (@Configuration)     │          │
│  │ - Mapping DTO ↔ Domain               │          │
│  │                                      │          │
│  │ Dépendances : Domain + Spring        │          │
│  └──────────────────────────────────────┘          │
│              ▲              ▲                      │
│              │              │                      │
│  COUCHE 3 : INFRASTRUCTURE                         │
│  ┌───────────┴──────────────┴───────────┐          │
│  │ Adaptateurs (le "sale" code)         │          │
│  │ - REST Controllers                   │          │
│  │ - Pub/Sub Listeners                  │          │
│  │ - Firestore Repositories             │          │
│  │                                      │          │
│  │ Dépendances : TOUT                   │          │
│  │ - Domain + Application + GCP SDK     │          │
│  └──────────────────────────────────────┘          │
│                                                    │
└────────────────────────────────────────────────────┘
```

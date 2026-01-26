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

## Uses cases
````

┌────────────────────────────────────────────────────┐
│        EVENT DRIVEN ARCHI : DRONEFLEET             │
├────────────────────────────────────────────────────┤
│                                                    │
│ ACTEURS (Qui interagit avec le système ?)          │
│ ├─ Dispatcher (humain qui coordonne)               │
│ ├─ Drones (agents autonomes)                       │
│ ├─ Hôpitaux (clients)                              │
│ └─ Optimizer (algorithme)                          │
│                                                    │
│ ÉVÉNEMENTS MÉTIER (Que se passe-t-il ?)            │
│ ├─ [1] Un hôpital demande une livraison urgente    │
│ ├─ [2] Un drone envoie sa position toutes les 1s   │
│ ├─ [3] L'optimizer assigne un drone à une order    │
│ ├─ [4] Le drone accepte ou refuse la mission       │
│ ├─ [5] Le drone arrive à l'entrepôt (pickup)       │
│ ├─ [6] Le drone prend le colis                     │
│ ├─ [7] Le drone arrive à destination (delivery)    │
│ ├─ [8] Le drone livre le colis                     │
│ ├─ [9] Le drone retourne à la base                 │
│ └─ [10] Le drone a une batterie faible             │
│                                                    │
│ COMMANDES (Que peut-on FAIRE ?)                    │
│ ├─ CreateOrder (hôpital crée une demande)          │
│ ├─ UpdateDronePosition (drone envoie telemetry)    │
│ ├─ AssignDroneToOrder (optimizer décide)           │
│ ├─ ConfirmPickup (drone confirme prise de colis)  │
│ ├─ ConfirmDelivery (drone confirme livraison)     │
│ └─ RequestCharging (drone demande recharge)       │
│                                                    │
│ RÈGLES MÉTIER (Contraintes)                        │
│ ├─ Un drone ne peut avoir qu'UNE mission active   │
│ ├─ Un drone avec batterie < 20% ne peut pas       │
│ │   accepter de nouvelle mission                  │
│ ├─ Une order en attente > 5 min trigger une alerte│
│ ├─ Un drone en panne doit être marqué MAINTENANCE │
│ └─ Les orders HIGH priority sont traitées en 1er  │
│                                                    │
└────────────────────────────────────────────────────┘
````
## Choix de structure de code :

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

## Scénario : Un Drone Envoie Sa Télémétrie

```
┌────────────────────────────────────────────────────┐
│        FLUX COMPLET : TELEMETRY → FIRESTORE        │
├────────────────────────────────────────────────────┤
│                                                    │
│ 1. Drone envoie JSON via Pub/Sub                  │
│    └─> Topic "telemetry"                          │
│                                                    │
│ 2. INFRASTRUCTURE/ADAPTER/IN/MESSAGING             │
│    TelemetryListener.java                          │
│    ├─ @PubSubMessageHandler("telemetry-sub")      │
│    ├─ Reçoit TelemetryEventDto (auto-désérialisé) │
│    ├─ Convertit DTO → Position (domain type)      │
│    └─ Appelle updateDroneStateUseCase.update()    │
│                                                    │
│ 3. DOMAIN/PORT/IN                                  │
│    UpdateDroneStateUseCase.java (interface)        │
│    └─> Contrat : updateDroneState(id, pos, bat)   │
│                                                    │
│ 4. DOMAIN/SERVICE                                  │
│    DroneStateService.java (implémentation)         │
│    ├─ Charge : droneRepository.findById()         │
│    ├─ Applique : drone.updateTelemetry()          │
│    └─ Sauvegarde : droneRepository.save()         │
│                                                    │
│ 5. DOMAIN/PORT/OUT                                 │
│    DroneRepository.java (interface)                │
│    └─> Contrat : save(drone), findById(id)        │
│                                                    │
│ 6. INFRASTRUCTURE/ADAPTER/OUT/PERSISTENCE          │
│    FirestoreDroneRepository.java (implémentation)  │
│    ├─ Convertit Drone → Map                       │
│    └─ firestore.collection("drones").set(data)    │
│                                                    │
│ 7. Firestore                                       │
│    └─> Document mis à jour                        │
│                                                    │
│ 8. TelemetryListener                               │
│    └─> ack() (confirme à Pub/Sub)                 │
│                                                    │
└────────────────────────────────────────────────────┘
```

# Détail de la structure :

## A. DOMAIN LAYER (Cœur logique isolé des outils)
```
domain/
├── model/              # Entités métier
├── port/
│   ├── in/            # Use Cases (ce que le domain OFFRE)
│   └── out/           # Dependencies (ce que le domain DEMANDE)
└── service/           # Logique métier (implémente les Use Cases)
```

#### domain/model/ : Les Entités Métier

**Responsabilité :** Représenter les concepts métier avec leur logique intrinsèque.

**Ce qu'on y trouve :**
```
Drone.java
├─ Attributs : id, position, batteryLevel, status
├─ Logique métier : isAvailable(), updateTelemetry()
├─ Règles métier : batterie < 20% → status = LOW_BATTERY
└─ ZÉRO dépendance externe (Java pur)

Order.java
├─ Attributs : id, pickupLocation, deliveryLocation, priority, status
├─ Logique métier : canBeAssigned(), markAsDelivered()
└─ Règles métier : priority HIGH traité en premier

Mission.java
├─ Attributs : id, droneId, orderId, route, status
├─ Logique métier : isCompleted(), calculateDuration()
└─ Règles métier : une mission = un drone + une order

Position.java
├─ Value Object : latitude, longitude
├─ Logique métier : distanceTo(Position other)
└─ Immutable (pas de setters)

DroneStatus.java (enum)
├─ Valeurs : AVAILABLE, IN_MISSION, CHARGING, LOW_BATTERY, MAINTENANCE
└─ Pas de logique (juste des constantes)
````

#### domain/service/ : La Logique Métier

**Responsabilité :** **Orchestrer** la logique métier en utilisant les modèles et les ports.

**Ce qu'on y trouve :**
```
DroneStateService.java
├─ Implémente : UpdateDroneStateUseCase
├─ Utilise : DroneRepository (Port OUT)
├─ Logique : Charger drone → Appliquer règles → Sauvegarder
└─ Pas de dépendance infrastructure (seulement interfaces)

OrderProcessingService.java
├─ Implémente : ProcessOrderUseCase
├─ Utilise : OrderRepository, DroneRepository
├─ Logique : Valider order → Vérifier drones disponibles → Créer order
└─ Pas de dépendance infrastructure

MissionAssignmentService.java
├─ Implémente : AssignMissionUseCase
├─ Utilise : MissionRepository, DroneRepository, OrderRepository
├─ Logique : Valider assignation → Créer mission → Mettre à jour statuts
└─ Pas de dépendance infrastructure
```


### domain/port/in/ : Les Use Cases

**Responsabilité :** Définir les **opérations métier** que le système expose.

**Ce qu'on y trouve :**
```
UpdateDroneStateUseCase.java
├─ Méthode : updateDroneState(droneId, position, battery)
├─ Cas d'usage : Mettre à jour l'état d'un drone depuis la télémétrie
└─ Appelé par : TelemetryListener (infrastructure)

ProcessOrderUseCase.java
├─ Méthode : processOrder(orderId, pickupLocation, deliveryLocation)
├─ Cas d'usage : Créer une nouvelle commande
└─ Appelé par : OrderListener (infrastructure)

GetFleetSnapshotUseCase.java
├─ Méthode : getFleetSnapshot() → List<Drone>
├─ Cas d'usage : Obtenir l'état complet de la flotte
└─ Appelé par : DroneController (REST) ou Optimizer

AssignMissionUseCase.java
├─ Méthode : assignMission(droneId, orderId, route)
├─ Cas d'usage : Assigner un drone à une commande
└─ Appelé par : CommandListener (infrastructure)
```
**Différence Use Case vs Service :**


Use Case (interface) = "QUOI faire"
Service (implémentation) = "COMMENT le faire"

- UpdateDroneStateUseCase : "Je dois mettre à jour un drone"

- DroneStateService :
"Voilà comment je mets à jour un drone :
1. Je charge le drone
2. J'applique la logique métier
3. Je sauvegarde"

### domain/port/out/ : Les Dépendances

**Responsabilité :** Définir les **dépendances** dont le domaine a besoin.

**Ce qu'on y trouve :**
```
DroneRepository.java
├─ Méthodes : save(), findById(), findAvailable()
├─ Responsabilité : Persister et récupérer les drones
└─ Implémenté par : FirestoreDroneRepository (infrastructure)

OrderRepository.java
├─ Méthodes : save(), findById(), findPending()
├─ Responsabilité : Persister et récupérer les commandes
└─ Implémenté par : FirestoreOrderRepository (infrastructure)

MissionRepository.java
├─ Méthodes : save(), findById(), findByDroneId()
├─ Responsabilité : Persister et récupérer les missions
└─ Implémenté par : FirestoreMissionRepository (infrastructure)

StatePublisher.java
├─ Méthodes : publishStateUpdate(drone), publishCommand(command)
├─ Responsabilité : Publier des événements vers l'extérieur
└─ Implémenté par : PubSubStatePublisher (infrastructure)
```

### Différence Service vs Model :
```
Model (Drone) :
├─ Logique sur UN drone
├─ isAvailable(), updateTelemetry()
└─ Pas de dépendances

Service (DroneStateService) :
├─ Logique sur PLUSIEURS entités
├─ Charger drone, appliquer règles, sauvegarder
└─ Utilise des Repositories (Ports OUT)
```


## B. APPLICATION LAYER (Coordination)
```
application/
├── config/            # Configuration Spring
└── dto/               # Data Transfer Objects
```

### application/config/ : Configuration Spring

**Responsabilité :** Configurer les **beans Spring** et les dépendances GCP.

**Ce qu'on y trouve :**
```
AppConfig.java
├─ @ConfigurationProperties pour lire application.yml
├─ Valeurs : batchWriteInterval, maxBatchSize
└─ Utilisé par : Services pour paramétrer leur comportement

PubSubConfig.java
├─ Configure le PubSubTemplate
├─ Configure les subscriptions (parallelPullCount, executorThreads)
└─ Utilisé par : Listeners et Publishers

FirestoreConfig.java
├─ Configure le bean Firestore
├─ Détecte si émulateur (FIRESTORE_EMULATOR_HOST)
└─ Utilisé par : Repositories
````

Exemple de configuration :
````
// application/config/AppConfig.java

@Configuration
@ConfigurationProperties(prefix = "app.state-manager")
public class AppConfig {

    /**
     * Intervalle entre les batch writes vers Firestore.
     * Défini dans application.yml : app.state-manager.batch-write-interval
     */
    private long batchWriteInterval = 5000;  // 5 secondes par défaut

    /**
     * Nombre max de drones à écrire par batch.
     * Défini dans application.yml : app.state-manager.max-batch-size
     */
    private int maxBatchSize = 100;

    // Getters/Setters
}
````
Puis dans les configs:

```
# application.yml
app:
  state-manager:
    batch-write-interval: 5000  # 5 secondes
    max-batch-size: 100         # 100 drones max par batch
```
### application/dto/ : Data Transfer Objects

**Responsabilité :** Objets qui **transitent** entre les couches (HTTP, Pub/Sub, etc.).

**Ce qu'on y trouve :**
```
TelemetryEventDto.java
├─ Champs : droneId, latitude, longitude, batteryLevel, timestamp
├─ Annotations : @Valid, @NotNull, @Min, @Max (validation)
├─ Utilisé par : TelemetryListener (reçoit depuis Pub/Sub)
└─ Converti vers : Position (domain model)

OrderEventDto.java
├─ Champs : orderId, pickupLocation, deliveryLocation, priority
├─ Utilisé par : OrderListener (reçoit depuis Pub/Sub)
└─ Converti vers : Order (domain model)

CommandEventDto.java
├─ Champs : missionId, droneId, orderId, route
├─ Utilisé par : CommandListener (reçoit depuis Pub/Sub)
└─ Converti vers : Mission (domain model)

FleetSnapshotDto.java
├─ Champs : drones (List<DroneDto>), timestamp
├─ Utilisé par : DroneController (retourne en JSON)
└─ Converti depuis : List<Drone> (domain models)
```

**Pourquoi séparer DTO et Domain Model ?**
```
┌────────────────────────────────────────────────────┐
│         DTO vs DOMAIN MODEL                        │
├────────────────────────────────────────────────────┤
│                                                    │
│ DTO (TelemetryEventDto) :                         │
│ ├─ Format externe (JSON, snake_case)              │
│ ├─ Peut avoir des champs inutiles                 │
│ ├─ Dépend du format Pub/Sub                       │
│ └─ Exemple : { "drone_id": "D001", "lat": 50.6 }  │
│                                                    │
│ Domain Model (Drone) :                             │
│ ├─ Format interne (Java, camelCase)               │
│ ├─ Seulement les données métier                   │
│ ├─ Indépendant du format externe                  │
│ └─ Exemple : Drone(id="D001", position=...)       │
│                                                    │
│ Conversion :                                       │
│ DTO → Domain : Dans l'adaptateur IN (Listener)    │
│ Domain → DTO : Dans l'adaptateur IN (Controller)  │
│                                                    │
└────────────────────────────────────────────────────┘
```

### C. INFRASTRUCTURE LAYER (Adaptateurs)
```
infrastructure/
├── adapter/
│   ├── in/                # Adaptateurs entrants (qui APPELLENT le domain)
│   │   ├── rest/          # REST API
│   │   └── messaging/     # Pub/Sub Listeners
│   └── out/               # Adaptateurs sortants (APPELÉS par le domain)
│       ├── persistence/   # Repositories Firestore
│       └── messaging/     # Pub/Sub Publishers
└── config/                # Config infrastructure
```

### infrastructure/adapter/in/rest/ : Controllers REST

**Responsabilité :** Exposer des **endpoints HTTP** qui appellent les Use Cases.

**Ce qu'on y trouve :**
```
DroneController.java
├─ GET /api/v1/drones → getFleetSnapshot()
├─ GET /api/v1/drones/{id} → getDroneById()
├─ Appelle : GetFleetSnapshotUseCase (Port IN)
└─ Retourne : List<DroneDto> (DTO)

OrderController.java
├─ GET /api/v1/orders → getAllOrders()
├─ GET /api/v1/orders/pending → getPendingOrders()
├─ Appelle : GetOrdersUseCase (Port IN)
└─ Retourne : List<OrderDto> (DTO)

HealthController.java
├─ GET /actuator/health → healthCheck()
├─ Vérifie : Firestore, Pub/Sub
└─ Retourne : { "status": "UP" }
```

**Flux d'un appel REST :**
```
1. HTTP GET /api/v1/drones
2. DroneController (infrastructure/adapter/in/rest)
   ├─ @GetMapping
   └─ Appelle getFleetSnapshotUseCase.getFleetSnapshot() (Port IN)
3. FleetSnapshotService (domain/service)
   ├─ Appelle droneRepository.findAll() (Port OUT)
   └─ Retourne List<Drone>
4. FirestoreDroneRepository (infrastructure/adapter/out/persistence)
   └─ Lit depuis Firestore
5. DroneController
   ├─ Convertit List<Drone> → List<DroneDto>
   └─ Retourne JSON
```

### infrastructure/adapter/in/messaging/ : Pub/Sub Listeners

**Responsabilité :** **Écouter** les messages Pub/Sub et appeler les Use Cases.

**Ce qu'on y trouve :**
```
TelemetryListener.java
├─ Écoute : Topic "telemetry" (100-200 msg/sec)
├─ Reçoit : TelemetryEventDto (JSON désérialisé)
├─ Appelle : UpdateDroneStateUseCase (Port IN)
└─ Gère : ack/nack (Pub/Sub acknowledgment)

OrderListener.java
├─ Écoute : Topic "orders" (10-20 msg/sec)
├─ Reçoit : OrderEventDto
├─ Appelle : ProcessOrderUseCase (Port IN)
└─ Gère : ack/nack

CommandListener.java
├─ Écoute : Topic "commands" (10 msg/sec)
├─ Reçoit : CommandEventDto
├─ Appelle : AssignMissionUseCase (Port IN)
└─ Gère : ack/nack
```

**Flux d'un message Pub/Sub :**
```
1. Pub/Sub Topic "telemetry" (message JSON)
2. TelemetryListener (infrastructure/adapter/in/messaging)
   ├─ @PubSubMessageHandler
   ├─ Désérialise JSON → TelemetryEventDto
   ├─ Convertit DTO → Position (domain type)
   └─ Appelle updateDroneStateUseCase.updateDroneState() (Port IN)
3. DroneStateService (domain/service)
   ├─ Charge drone via droneRepository.findById() (Port OUT)
   ├─ Applique drone.updateTelemetry() (logique métier)
   └─ Sauvegarde via droneRepository.save() (Port OUT)
4. FirestoreDroneRepository (infrastructure/adapter/out/persistence)
   └─ Écrit dans Firestore
5. TelemetryListener
   └─ ack() (confirme le traitement à Pub/Sub)
```

### infrastructure/adapter/out/persistence/ : Repositories Firestore

**Responsabilité :** **Implémenter** les Ports OUT pour la persistance.

**Ce qu'on y trouve :**
```
FirestoreDroneRepository.java
├─ Implémente : DroneRepository (Port OUT)
├─ Méthodes : save(), findById(), findAvailable()
├─ Code : Appels Firestore SDK (collection, document, get, set)
└─ Conversion : Drone (domain) ↔ Map (Firestore)

FirestoreOrderRepository.java
├─ Implémente : OrderRepository (Port OUT)
├─ Méthodes : save(), findById(), findPending()
└─ Code : Appels Firestore SDK

FirestoreMissionRepository.java
├─ Implémente : MissionRepository (Port OUT)
├─ Méthodes : save(), findById(), findByDroneId()
└─ Code : Appels Firestore SDK
```

**Flux de persistance :**
```
1. DroneStateService (domain)
   └─ Appelle droneRepository.save(drone) (Port OUT, interface)
2. Spring (DI)
   └─ Injecte FirestoreDroneRepository (implémentation)
3. FirestoreDroneRepository (infrastructure/adapter/out/persistence)
   ├─ Convertit Drone → Map<String, Object>
   └─ firestore.collection("drones").document(id).set(data)
```

**Pourquoi cette indirection ?**
```
SANS Port OUT :
DroneStateService → firestore.collection("drones").set()
=> Dépendance directe à Firestore
=> Impossible de tester sans Firestore

AVEC Port OUT :
DroneStateService → droneRepository.save() (interface)
                    ↓
    Spring injecte FirestoreDroneRepository (implémentation)
=> Dépendance sur l'interface seulement
=> Testable avec InMemoryDroneRepository
`````

### infrastructure/adapter/out/messaging/ : Pub/Sub Publishers

**Responsabilité :** **Publier** des messages vers Pub/Sub (implémente StatePublisher).

**Ce qu'on y trouve :**
```
PubSubStatePublisher.java
├─ Implémente : StatePublisher (Port OUT)
├─ Méthodes : publishStateUpdate(), publishCommand()
├─ Code : PubSubTemplate.publish(topic, message)
└─ Utilisé par : Services qui doivent notifier l'extérieur
```

**Flux de publication :**
```
1. MissionAssignmentService (domain)
   └─ Appelle statePublisher.publishCommand(command) (Port OUT)
2. Spring (DI)
   └─ Injecte PubSubStatePublisher (implémentation)
3. PubSubStatePublisher (infrastructure/adapter/out/messaging)
   ├─ Convertit Command → CommandEventDto
   └─ pubSubTemplate.publish("commands", dto)
````


### infrastructure/config/ : Configuration Infrastructure

**Responsabilité :** Configurer les **outils GCP** (Firestore, Pub/Sub).

**Ce qu'on y trouve :**
```
GcpConfig.java
├─ Configure le bean Firestore
├─ Détecte émulateur (FIRESTORE_EMULATOR_HOST)
└─ Crée les connexions GCP

ConcurrencyConfig.java
├─ Configure les Virtual Threads (Java 21)
├─ Configure les Executors pour Pub/Sub
└─ Optimise la concurrence
```

## Conclusion : Pourquoi cette complexité ?

**Question : Pourquoi ne pas tout mettre dans un seul fichier Controller ?**

**Réponse : Parce que dans 6 mois, tu voudras :**

1. **Changer Firestore pour PostgreSQL**
   - Avec cette archi : Tu crées `PostgresDroneRepository` (1 fichier)
   - Sans cette archi : Tu réécris TOUT (10+ fichiers)

2. **Ajouter un endpoint GraphQL**
   - Avec cette archi : Tu crées `GraphQLDroneResolver` qui appelle les MÊMES Use Cases
   - Sans cette archi : Tu dupliques la logique métier

3. **Tester sans GCP**
   - Avec cette archi : Tu crées `InMemoryDroneRepository` et tu testes
   - Sans cette archi : Tu DOIS avoir Firestore qui tourne

4. **Montrer ton code en entretien**
   - Avec cette archi : Le recruteur voit une architecture professionnelle
   - Sans cette archi : Le recruteur voit du code spaghetti

**En résumé :**
```
Architecture Hexagonale = Investissement initial
                         + Maintenabilité long terme
                         + Impression en entretien

Architecture Lasagne = Rapidité initiale
                      + Dette technique
                      + Difficulté à évoluer
```

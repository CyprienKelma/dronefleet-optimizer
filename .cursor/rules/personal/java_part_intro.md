# Java State Manager: Architecture & Fundamentals

This documentation covers the core concepts, architecture patterns, and Spring Boot fundamentals for the DroneFleet State Manager.

## 1. Conclusion : Mes Réponses Directes

1.  **Était-ce une mauvaise idée de demander la structure ?**
    → **NON.** L'architecture hexagonale est un pattern établi, pas une invention. Mais tu **DOIS** comprendre le pourquoi de chaque choix.

2.  **Devrait-on clarifier le besoin métier d'abord ?**
    → **OUI**, absolument. Fais un Event Storming / User Story Mapping **AVANT** de coder. Je t'ai donné un exemple ci-dessus.

3.  **La lecture a-t-elle un impact sur les capacités cognitives ?**
    → **OUI**, impact **MASSIF**. La lecture profonde développe la pensée abstraite, la concentration, et les modèles mentaux. C'est scientifiquement prouvé.

4.  **Vaut-il le coup de lire de la doc longue ?**
    → **OUI**, mais de manière **CIBLÉE**. Lis seulement ce dont tu as besoin, au moment où tu en as besoin (Just-In-Time Learning).

5.  **DDIA est-il pertinent pour toi ?**
    → **OUI**, à 100%. Ce livre te donnera les fondamentaux des systèmes distribués. Lis 10-15 pages/jour, applique les concepts à DroneFleet.

### Prochaine étape concrète (cette semaine) :

- [x] Crée `docs/business-requirements.md` (2h)
- [x] Fais un Event Storming sur papier (1h)
- [x] Définis 5 Use Cases prioritaires (1h)
- [x] Lis Chapitre 1 de DDIA (2h)
- [x] Code `Drone.java` (modèle de domaine) TOI-MÊME (2h)

---

## 2. Les 20% de Java/Spring Boot qui Servent 80% du Temps

### Introduction : La Règle de Pareto Appliquée à Spring Boot

```text
┌────────────────────────────────────────────────────┐
│      CONCEPTS SPRING BOOT PAR FRÉQUENCE D'USAGE    │
├────────────────────────────────────────────────────┤
│                                                    │
│ 20% DES CONCEPTS (Ce que je vais t'expliquer) :   │
│ ├─ Inversion of Control (IoC) & Dependency         │
│ │  Injection (DI)                                  │
│ ├─ Annotations Spring (@Component, @Service, etc.) │
│ ├─ Configuration (application.yml, @Bean)          │
│ ├─ REST Controllers (@RestController, @GetMapping) │
│ ├─ Spring Cloud GCP (Pub/Sub, Firestore)          │
│ └─ Testing (@SpringBootTest, @MockBean)            │
│                                                    │
│ → Utilisés dans 80% de ton code DroneFleet        │
│                                                    │
│ 80% DES CONCEPTS (Avancés, pas urgents) :         │
│ ├─ Spring Security (OAuth2, JWT)                  │
│ ├─ Spring Data JPA (ORM complexe)                 │
│ ├─ Spring Batch (batch processing)                │
│ ├─ Spring WebFlux (reactive programming)          │
│ ├─ Spring AOP (Aspect-Oriented Programming)       │
│                                                    │
│ → Tu n'en auras PAS besoin pour DroneFleet MVP    │
│                                                    │
└────────────────────────────────────────────────────┘
```

### 1. Inversion of Control (IoC) & Dependency Injection (DI)

#### A. Le Problème Sans Spring (Code Traditionnel)

```java
// ❌ SANS Spring : Tu gères les dépendances manuellement
public class TelemetryListener {
    private DroneStateService droneStateService;

    public TelemetryListener() {
        // Tu dois instancier TOUTES les dépendances toi-même
        DroneRepository droneRepository = new FirestoreDroneRepository();
        this.droneStateService = new DroneStateService(droneRepository);
    }
}
```

#### B. La Solution Spring : Inversion of Control

```text
┌────────────────────────────────────────────────────┐
│          INVERSION OF CONTROL (IoC)                │
├────────────────────────────────────────────────────┤
│                                                    │
│ SANS Spring :                                      │
│ Toi → "Je veux un DroneStateService"              │
│     → new DroneStateService(new FirestoreRepo())  │
│                                                    │
│ AVEC Spring :                                      │
│ Toi → "J'ai besoin d'un DroneStateService"        │
│ Spring → "OK, je vais le créer pour toi"          │
│       → Spring crée FirestoreRepo                 │
│       → Spring crée DroneStateService             │
│       → Spring te l'injecte                       │
└────────────────────────────────────────────────────┘
```

```java
// ✅ AVEC Spring : Constructor Injection (BEST PRACTICE)
@Component
public class TelemetryListener {
    private final DroneStateService droneStateService;

    public TelemetryListener(DroneStateService droneStateService) {
        this.droneStateService = droneStateService;
    }
}
```

---

## 3. Spring Cloud GCP : Intégration Google Cloud

### Pub/Sub avec Spring Cloud GCP

Spring Cloud GCP agit comme un wrapper de haut niveau autour du SDK Google Cloud.

```text
┌────────────────────────────────────────────────────┐
│           SPRING CLOUD GCP : ABSTRACTION           │
├────────────────────────────────────────────────────┤
│                                                    │
│ Google Cloud SDK (Bas niveau) :                   │
│ ├─ Gestion manuelle des credentials                │
│ ├─ Gestion manuelle des threads                    │
│                                                    │
│ Spring Cloud GCP (Haut niveau) :                  │
│ ├─ Credentials automatiques                        │
│ ├─ Annotations simples (@PubSubMessageHandler)     │
└────────────────────────────────────────────────────┘
```

---

## 4. Architecture Hexagonale (Senior Perspective)

### Pourquoi Cette Structure ?

L'objectif est d'isoler le **Domain** (logique métier pure) des détails techniques (Infrastructure).

```text
┌────────────────────────────────────────────────────┐
│              ARCHITECTURE EN 3 COUCHES             │
├────────────────────────────────────────────────────┤
│                                                    │
│  COUCHE 1 : DOMAIN (Le Cœur)                      │
│  ┌──────────────────────────────────────┐         │
│  │ Logique métier PURE                  │         │
│  │ - Entités (Drone, Order)             │         │
│  │ - Règles métier (isAvailable())      │         │
│  │ - Interfaces (Ports IN/OUT)          │         │
│  │                                       │         │
│  │ Dépendances : ZÉRO                   │         │
│  └──────────────────────────────────────┘         │
│              ▲              ▲                      │
│              │              │                      │
│    Utilise   │              │  Implémente          │
└──────────────┴──────────────┴──────────────────────┘
```

### Opt-in vs Opt-out
- **Opt-in** : Tu choisis explicitement d'inclure une dépendance ou une fonctionnalité.
- **Opt-out** : Tu retires une fonctionnalité par défaut.

En architecture hexagonale, le domaine est "Opt-in" pour tout ce qui est externe : il ne connaît rien par défaut et définit ce dont il a besoin via des **Ports**.

---

## 5. Build Configuration (build.gradle)

Voici ton `build.gradle` actuel expliqué :

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.9'
    id 'io.spring.dependency-management' version '1.1.7'
}

dependencies {
    // Monitoring & Observabilité (Cloud Run health checks)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Validation des DTOs
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Serveur Web (API REST)
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Intégration GCP
    implementation 'com.google.cloud:spring-cloud-gcp-starter-pubsub'
    implementation 'com.google.cloud:spring-cloud-gcp-starter-storage'

    // Productivité
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

│    Utilise   │              │  Implémente          │
│              │              │                      │
│  COUCHE 2 : APPLICATION                            │
│  ┌───────────┴──────────────┴──────────┐          │
│  │ Coordination & Configuration         │          │
│  │ - DTOs (objets de transport)         │          │
│  │ - Config Spring (@Configuration)     │          │
│  │ - Mapping DTO ↔ Domain              │          │
│  │                                      │          │
│  │ Dépendances : Domain + Spring        │          │
│  └──────────────────────────────────────┘          │
│              ▲              ▲                      │
│              │              │                      │
│  COUCHE 3 : INFRASTRUCTURE                         │
│  ┌───────────┴──────────────┴──────────┐          │
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
Règle d'or : Les dépendances vont TOUJOURS vers le centre (DOMAIN).

2. Ports IN vs Ports OUT : La Clé de la Compréhension
A. Analogie : Le Restaurant
Imagine le State Manager comme un restaurant :
┌────────────────────────────────────────────────────┐
│            ANALOGIE DU RESTAURANT                  │
├────────────────────────────────────────────────────┤
│                                                    │
│ LA CUISINE (DOMAIN) :                              │
│ - Le chef (logique métier)                         │
│ - Les recettes (règles métier)                     │
│ - Cuisine selon des règles strictes                │
│                                                    │
│ PORTS IN (Ce que la cuisine OFFRE) :               │
│ - "Je peux préparer un plat"                       │
│ - "Je peux vérifier l'état des commandes"          │
│ - "Je peux annuler une commande"                   │
│ → Interface : CommandeUseCase                      │
│                                                    │
│ PORTS OUT (Ce que la cuisine DEMANDE) :            │
│ - "J'ai besoin d'ingrédients"                      │
│ - "J'ai besoin d'un frigo pour stocker"            │
│ - "J'ai besoin d'un carnet de commandes"           │
│ → Interface : FrigoRepository, CarnetRepository    │
│                                                    │
│ ADAPTATEURS IN (Qui utilise la cuisine) :          │
│ - Serveur (REST API)                               │
│ - Téléphone (Pub/Sub Listener)                     │
│ - Application mobile (GraphQL)                     │
│ → Implémentations : RestController, PubSubListener│
│                                                    │
│ ADAPTATEURS OUT (Où la cuisine se fournit) :       │
│ - Frigo électrique (Firestore)                    │
│ - Frigo à gaz (PostgreSQL)                        │
│ - Carnet papier (In-Memory)                       │
│ → Implémentations : FirestoreRepo, PostgresRepo   │
│                                                    │
└────────────────────────────────────────────────────┘
Point clé : La cuisine (DOMAIN) ne sait PAS :

Qui l'appelle (serveur, téléphone, app mobile)
Où sont stockés les ingrédients (frigo électrique, frigo à gaz)

Elle expose juste des interfaces (contrats) pour dire ce qu'elle offre et ce dont elle a besoin.
B. Ports IN : Les Use Cases (Ce que le Domain OFFRE)
Définition : Un Port IN est une promesse que le domaine fait au monde extérieur.
Port IN = "Je promets de faire ça si tu m'appelles"
Exemple concret pour DroneFleet :
java// domain/port/in/UpdateDroneStateUseCase.java

/**
 * Port IN : Use Case pour mettre à jour l'état d'un drone.
 *
 * Ce que le domaine PROMET de faire :
 * - Recevoir une telemetry
 * - Valider les données
 * - Mettre à jour l'état du drone
 * - Appliquer les règles métier (batterie < 20% → LOW_BATTERY)
 *
 * Ce que le domaine NE PROMET PAS :
 * - Comment la telemetry arrive (Pub/Sub ? REST ? Kafka ?)
 * - Où l'état est stocké (Firestore ? PostgreSQL ?)
 * → Ces détails sont dans les adaptateurs
 */
public interface UpdateDroneStateUseCase {

    /**
     * Update drone state from telemetry data.
     *
     * @param droneId Identifiant du drone
     * @param position Nouvelle position GPS
     * @param batteryLevel Niveau de batterie actuel (0-100)
     * @throws DroneNotFoundException si le drone n'existe pas
     * @throws InvalidTelemetryException si les données sont invalides
     */
    void updateDroneState(String droneId, Position position, double batteryLevel);
}
Qui implémente ce Port IN ?
java// domain/service/DroneStateService.java

/**
 * Service qui IMPLÉMENTE le Use Case.
 * C'est ici que la VRAIE logique métier vit.
 */
@Service
public class DroneStateService implements UpdateDroneStateUseCase {

    private final DroneRepository droneRepository;  // Port OUT (on en parle après)

    @Override
    public void updateDroneState(String droneId, Position position, double batteryLevel) {
        // 1. Charger le drone (via Port OUT)
        Drone drone = droneRepository.findById(droneId)
            .orElseThrow(() -> new DroneNotFoundException(droneId));

        // 2. Appliquer la logique métier (DOMAIN MODEL)
        drone.updateTelemetry(position, batteryLevel);

        // 3. Persister le drone (via Port OUT)
        droneRepository.save(drone);
    }
}
Qui APPELLE ce Use Case ?
java// infrastructure/adapter/in/messaging/TelemetryListener.java

/**
 * Adaptateur IN : Écoute Pub/Sub et appelle le Use Case.
 * Cet adaptateur est JETABLE. Si demain on passe à Kafka,
 * on crée un KafkaListener qui appelle le MÊME Use Case.
 */
@Component
public class TelemetryListener {

    private final UpdateDroneStateUseCase updateDroneStateUseCase;  // Port IN

    @PubSubMessageHandler(subscriptionName = "telemetry-sub")
    public void handleTelemetry(TelemetryEventDto event) {
        // 1. Convertir DTO → Domain types
        Position position = new Position(event.getLatitude(), event.getLongitude());

        // 2. Appeler le Use Case (Domain)
        updateDroneStateUseCase.updateDroneState(
            event.getDroneId(),
            position,
            event.getBatteryLevel()
        );
    }
}
```

**Le flux complet :**

```
Pub/Sub (message JSON)
    ↓
TelemetryListener (INFRASTRUCTURE - Adaptateur IN)
    ├─ Désérialise JSON → TelemetryEventDto
    ├─ Convertit DTO → Position (domain type)
    └─ Appelle updateDroneState() (Port IN)
        ↓
DroneStateService (DOMAIN - Service)
    ├─ Charge le drone via droneRepository.findById() (Port OUT)
    ├─ Applique drone.updateTelemetry() (logique métier)
    └─ Sauvegarde via droneRepository.save() (Port OUT)
        ↓
FirestoreDroneRepository (INFRASTRUCTURE - Adaptateur OUT)
    └─ Écrit dans Firestore
```

**Pourquoi séparer Port IN (interface) et Service (implémentation) ?**

1. **Testabilité :** Tu peux mocker l'interface dans les tests
2. **Documentation :** L'interface documente le contrat
3. **Multiples implémentations :** Tu pourrais avoir `DroneStateServiceV1` et `DroneStateServiceV2`

### C. Ports OUT : Les Dépendances (Ce que le Domain DEMANDE)

**Définition :** Un Port OUT est une **demande** que le domaine fait au monde extérieur.
```
Port OUT = "J'ai besoin de ça, quelqu'un doit me le fournir"
Exemple concret :
java// domain/port/out/DroneRepository.java

/**
 * Port OUT : Interface que le domaine DEMANDE.
 *
 * Le domaine dit : "J'ai besoin d'un moyen de persister les drones,
 * mais je m'en fous de COMMENT c'est fait."
 *
 * C'est l'infrastructure qui décide comment implémenter ça.
 */
public interface DroneRepository {

    /**
     * Sauvegarder un drone.
     * Le domaine ne sait PAS où il est sauvegardé.
     */
    void save(Drone drone);

    /**
     * Trouver un drone par ID.
     * Le domaine ne sait PAS d'où il vient.
     */
    Optional<Drone> findById(String id);

    /**
     * Trouver tous les drones disponibles.
     * Le domaine ne sait PAS comment c'est requêté.
     */
    List<Drone> findAvailable();
}
Qui implémente ce Port OUT ?
java// infrastructure/adapter/out/persistence/FirestoreDroneRepository.java

/**
 * Adaptateur OUT : Implémente le Port OUT avec Firestore.
 * Cet adaptateur est JETABLE. Si demain on passe à PostgreSQL,
 * on crée un PostgresDroneRepository qui implémente la MÊME interface.
 */
@Repository
public class FirestoreDroneRepository implements DroneRepository {

    private final Firestore firestore;

    @Override
    public void save(Drone drone) {
        // Code spécifique à Firestore
        firestore.collection("drones")
            .document(drone.getId())
            .set(convertToMap(drone));
    }

    @Override
    public Optional<Drone> findById(String id) {
        // Code spécifique à Firestore
        DocumentSnapshot doc = firestore.collection("drones")
            .document(id)
            .get()
            .get();

        return doc.exists()
            ? Optional.of(convertToDrone(doc))
            : Optional.empty();
    }

    @Override
    public List<Drone> findAvailable() {
        // Query spécifique à Firestore
        return firestore.collection("drones")
            .whereEqualTo("status", "AVAILABLE")
            .whereGreaterThan("batteryLevel", 20.0)
            .get()
            .get()
            .toObjects(Drone.class);
    }
}
```

**Le flux complet (sens inverse) :**
```
DroneStateService (DOMAIN)
    └─ Appelle droneRepository.findById() (Port OUT)
        ↓
FirestoreDroneRepository (INFRASTRUCTURE - Adaptateur OUT)
    └─ Lit depuis Firestore
```

**Pourquoi cette inversion ?**
```
┌────────────────────────────────────────────────────┐
│     SANS Port OUT (Dépendance directe)             │
├────────────────────────────────────────────────────┤
│                                                    │
│ DroneStateService                                  │
│    ├─ import com.google.cloud.firestore.Firestore │
│    └─ firestore.collection("drones").get()        │
│                                                    │
│ Problèmes :                                        │
│ ❌ Le service DÉPEND de Firestore                  │
│ ❌ Impossible de tester sans Firestore             │
│ ❌ Changer de DB = modifier le service             │
│                                                    │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│     AVEC Port OUT (Inversion de dépendance)        │
├────────────────────────────────────────────────────┤
│                                                    │
│ DroneStateService                                  │
│    ├─ import com.dronefleet.domain.port.out.      │
│    │   DroneRepository (INTERFACE)                │
│    └─ droneRepository.findById() (abstraction)    │
│                                                    │
│ FirestoreDroneRepository (séparé)                 │
│    ├─ import com.google.cloud.firestore.Firestore │
│    └─ IMPLÉMENTE DroneRepository                  │
│                                                    │
│ Avantages :                                        │
│ ✅ Le service ne dépend QUE de l'interface         │
│ ✅ Testable avec un Mock                           │
│ ✅ Changer de DB = créer un nouvel adaptateur      │
│                                                    │
└────────────────────────────────────────────────────┘
```

---

## 3. Décomposition de la Structure : Chaque Dossier Expliqué

### A. DOMAIN LAYER (Le Cœur Sacré)
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
Pourquoi séparer model/ ?

Single Responsibility : Chaque entité a UNE responsabilité
Testabilité : Tu peux tester Drone.isAvailable() sans base de données
Documentation : Le modèle documente le domaine métier

Exemple de règle métier dans le modèle :
java// domain/model/Drone.java

public class Drone {
    private String id;
    private Position position;
    private double batteryLevel;
    private DroneStatus status;

    /**
     * Règle métier : Un drone est disponible si :
     * - Son statut est AVAILABLE
     * - Sa batterie est > 20%
     *
     * Cette règle est dans le MODÈLE, pas dans un service.
     * Pourquoi ? Parce que c'est une propriété INTRINSÈQUE du drone.
     */
    public boolean isAvailable() {
        return status == DroneStatus.AVAILABLE && batteryLevel > 20.0;
    }

    /**
     * Règle métier : Mettre à jour la télémétrie et ajuster le statut.
     *
     * Cette règle est dans le MODÈLE car elle concerne l'état interne du drone.
     */
    public void updateTelemetry(Position newPosition, double batteryLevel) {
        this.position = newPosition;
        this.batteryLevel = batteryLevel;
        this.lastUpdate = Instant.now();

        // Règle métier : Batterie faible → changer le statut
        if (batteryLevel < 20.0 && this.status == DroneStatus.AVAILABLE) {
            this.status = DroneStatus.LOW_BATTERY;
        }
    }
}
```

**Ce qui NE doit PAS être dans model/ :**
- ❌ Appels à Firestore
- ❌ Appels à Pub/Sub
- ❌ Dépendances Spring
- ❌ DTOs (objets de transport)

#### domain/port/in/ : Les Use Cases

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

**Pourquoi des interfaces pour les Use Cases ?**

1. **Contrat clair :** L'interface documente ce que le système peut faire
2. **Testabilité :** Tu peux mocker l'interface dans les tests
3. **Découplage :** L'infrastructure ne dépend que de l'interface, pas de l'implémentation

**Différence Use Case vs Service :**
```
Use Case (interface) = "QUOI faire"
Service (implémentation) = "COMMENT le faire"

UpdateDroneStateUseCase : "Je dois mettre à jour un drone"
DroneStateService : "Voilà comment je mets à jour un drone :
                     1. Je charge le drone
                     2. J'applique la logique métier
                     3. Je sauvegarde"
```

#### domain/port/out/ : Les Dépendances

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

**Pourquoi des interfaces pour les dépendances ?**

1. **Inversion de dépendance :** Le domaine ne dépend PAS de l'infrastructure
2. **Testabilité :** Tu peux créer un `InMemoryDroneRepository` pour les tests
3. **Flexibilité :** Tu peux changer Firestore pour PostgreSQL sans toucher au domaine

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

**Différence Service vs Model :**
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

**Exemple de flux dans un Service :**
```
1. TelemetryListener (infrastructure) appelle updateDroneState() (Port IN)
2. DroneStateService (domain/service) :
   a. Charge le drone via droneRepository.findById() (Port OUT)
   b. Applique drone.updateTelemetry() (logique modèle)
   c. Sauvegarde via droneRepository.save() (Port OUT)
3. FirestoreDroneRepository (infrastructure) écrit dans Firestore
```

---

### B. APPLICATION LAYER (Coordination)
```
application/
├── config/            # Configuration Spring
└── dto/               # Data Transfer Objects
```

#### application/config/ : Configuration Spring

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
Pourquoi séparer config/ ?

Single Responsibility : La config est séparée du code métier
Environnements : Facile de changer la config selon l'environnement (local/dev/prod)
Testabilité : Tu peux override la config dans les tests

Exemple de configuration :
java// application/config/AppConfig.java

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
yaml# application.yml
app:
  state-manager:
    batch-write-interval: 5000  # 5 secondes
    max-batch-size: 100         # 100 drones max par batch
```

#### application/dto/ : Data Transfer Objects

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
Exemple de conversion :
java// infrastructure/adapter/in/messaging/TelemetryListener.java

@PubSubMessageHandler(subscriptionName = "telemetry-sub")
public void handleTelemetry(TelemetryEventDto dto) {  // DTO reçu de Pub/Sub

    // Conversion DTO → Domain types
    Position position = new Position(dto.getLatitude(), dto.getLongitude());

    // Appel du Use Case avec les types domain
    updateDroneStateUseCase.updateDroneState(
        dto.getDroneId(),
        position,
        dto.getBatteryLevel()
    );
}
```

**Avantage :** Si le format JSON change (lat/lon → location), tu changes SEULEMENT le DTO et la conversion, pas le domaine.

---

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

#### infrastructure/adapter/in/rest/ : Controllers REST

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

#### infrastructure/adapter/in/messaging/ : Pub/Sub Listeners

**Responsabilité :** **Écouter** les messages Pub/Sub et appeler les Use Cases.

**Ce qu'on y trouve :**

- **TelemetryListener.java**
    - ├─ Écoute : Topic "telemetry" (100-200 msg/sec)
    - ├─ Reçoit : `TelemetryEventDto` (JSON désérialisé)
    - ├─ Appelle : `UpdateDroneStateUseCase` (Port IN)
    - └─ Gère : ack/nack (Pub/Sub acknowledgment)
- **OrderListener.java**
    - ├─ Écoute : Topic "orders" (10-20 msg/sec)
    - ├─ Reçoit : `OrderEventDto`
    - ├─ Appelle : `ProcessOrderUseCase` (Port IN)
    - └─ Gère : ack/nack
- **CommandListener.java**
    - ├─ Écoute : Topic "commands" (10 msg/sec)
    - ├─ Reçoit : `CommandEventDto`
    - ├─ Appelle : `AssignMissionUseCase` (Port IN)
    - └─ Gère : ack/nack

**Flux d'un message Pub/Sub :**

1.  **Pub/Sub Topic "telemetry"** (message JSON)
2.  **TelemetryListener** (infrastructure/adapter/in/messaging)
    - ├─ `@PubSubMessageHandler`
    - ├─ Désérialise JSON → `TelemetryEventDto`
    - ├─ Convertit DTO → `Position` (domain type)
    - └─ Appelle `updateDroneStateUseCase.updateDroneState()` (Port IN)
3.  **DroneStateService** (domain/service)
    - ├─ Charge drone via `droneRepository.findById()` (Port OUT)
    - ├─ Applique `drone.updateTelemetry()` (logique métier)
    - └─ Sauvegarde via `droneRepository.save()` (Port OUT)
4.  **FirestoreDroneRepository** (infrastructure/adapter/out/persistence)
    - └─ Écrit dans Firestore
5.  **TelemetryListener**
    - └─ `ack()` (confirme le traitement à Pub/Sub)

#### infrastructure/adapter/out/persistence/ : Repositories Firestore

**Responsabilité :** **Implémenter** les Ports OUT pour la persistance.

**Ce qu'on y trouve :**

- **FirestoreDroneRepository.java**
    - ├─ Implémente : `DroneRepository` (Port OUT)
    - ├─ Méthodes : `save()`, `findById()`, `findAvailable()`
    - ├─ Code : Appels Firestore SDK (collection, document, get, set)
    - └─ Conversion : `Drone` (domain) ↔ `Map` (Firestore)

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
❌ Dépendance directe à Firestore
❌ Impossible de tester sans Firestore

AVEC Port OUT :
DroneStateService → droneRepository.save() (interface)
                    ↓
    Spring injecte FirestoreDroneRepository (implémentation)
✅ Dépendance sur l'interface seulement
✅ Testable avec InMemoryDroneRepository
```

#### infrastructure/adapter/out/messaging/ : Pub/Sub Publishers

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
```

#### infrastructure/config/ : Configuration Infrastructure

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

---

## 4. Récapitulatif : Le Flux Complet

### Scénario : Un Drone Envoie Sa Télémétrie
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

---

## 5. Conclusion : Pourquoi Cette Complexité ?

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

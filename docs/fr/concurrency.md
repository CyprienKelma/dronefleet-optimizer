# Concurrence

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

# Composants du système

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

# Pile technologique

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

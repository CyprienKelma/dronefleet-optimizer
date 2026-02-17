# Structure du dépôt

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

 # PROJECT_CONTEXT.md - DroneFleet Optimizer

> **INSTRUCTION POUR L'IA :** Ce document est la source de vérité absolue du projet. Agis en tant que Lead Developer / Architecte Cloud. Réfère-toi toujours à ces contraintes, cette stack et cette architecture avant de proposer du code.

## 1. Vision & Business Case
**Nom du Produit :** DroneFleet Optimizer (LifeLine Logistics)
**Le Pitch :** Une plateforme de logistique autonome capable de livrer du matériel médical d'urgence (sang, vaccins, défibrillateurs) en moins de 15 minutes en zone urbaine, en coordonnant une flotte de drones via un algorithme central (de recherche opérationelle ou plus tard de metaheuristique).

**Niveau d'Exigence :** "Enterprise Grade".
Ce n'est pas un POC jetable. Le code doit être :
* **Typé strictement** (Pydantic pour Python, Typage fort pour Java).
* **Résilient** (Gestion des erreurs, Retry policies, Dead Letter Queues).
* **Agnostique** (Architecture Hexagonale pour découpler la logique métier de l'infra).
* **Bonne pratique only** (Respecte toujours les règles du 12 Factor App de 12factor.net, utilise des Design Pattern, pense en séparant les environnement, accorde beacuoup d'importance à la sécurité)
* **Commentaires et explications** (Expliquer au maximum les choix et le code avec des commentaires. Ne JAMAIS mettre d'Emoji nulle part et écrire des commentaires professionels, propres, concis, impersonels et en Anglais)

L'objectif final sur le long terme est d'obtenir un projet digne d'une entreprise Tech qui impressionne en entretien et qui témoigne d'une grande maitrise et experience.

## 2. Métriques & Contraintes (SLAs)
* **Latence Visuelle :** < 500ms (Temps réel mou).
* **Cycle d'Optimisation :** Batch toutes les 10 secondes (Rolling Horizon Planning).
* **Échelle (Scale) :** 50 à 100 drones actifs simultanés pour le MVP.
* **Fiabilité :** Aucune perte de commande ("At-Least-Once" delivery).
* **FinOps :** Optimisation des coûts Firestore (Batch writes) pour rester compatible Free Tier/Low Cost en dev.

## 3. Architecture Technique (Hybride & Hexagonale)

### A. La Stack "Polyglotte"

![Architecture Schema](../../../docs/images/global_architecture_png.png)

| Composant | Langage / Framework | Infra Local (Dev) | Infra Prod (GCP) | Rôle |
| :--- | :--- | :--- | :--- | :--- |
| **Ingestion API** | Python 3.11 / **FastAPI** | Docker Compose | Cloud Run (Service) | Gateway d'entrée, Validation JSON, Push to PubSub. |
| **Message Bus** | - | **Pub/Sub Emulator** | Google Pub/Sub | Bus d'événements asynchrone. |
| **State Manager** | **Java 21 / Spring Boot 4** | Docker Compose | Cloud Run (Native) | Consomme les events, gère la cohérence, écrit en DB. |
| **Optimizer** | Python 3.11 / **OR-Tools** | Docker Compose | Cloud Run (Job) | Résout le VRP (Vehicle Routing Problem). |
| **Database** | - | **Firestore Emulator** | Firestore (NoSQL) | Stockage de l'état "Chaud" (Hot Storage). |
| **Frontend** | Python / **Streamlit** | Docker Compose | Cloud Run | Visualisation cartographique. |

### B. Pattern d'Adaptateur (Adapter Pattern)
Le code doit être agnostique de l'infrastructure via des interfaces.
* **Stratégie actuelle :** `ON_CLOUD` (Simulé localement via Emulateurs).
* **Stratégie future (Optionnelle) :** `ON_PREMISE` (Kafka + Postgres sur K8s).
* **Règle :** L'API d'ingestion ne doit pas importer `google.cloud.pubsub` directement dans le service, mais passer par une interface `EventDispatcher`.

## 4. Flux de Données (Data Flow)

1.  **Ingestion :**
    * `Simulator` -> HTTP POST -> `Ingestion API`
    * Validation Pydantic -> Push dans Topic `telemetry` ou `orders`.
2.  **State Management (Hot Path) :**
    * `State Manager (Java)` écoute `telemetry`.
    * Met à jour l'état en mémoire + Persistance optimisée (Firestore).
3.  **Optimisation (Batch - 10s) :**
    * `Optimizer (Python)` se réveille.
    * Récupère le snapshot global (Drones + Commandes en attente).
    * Calcule les trajets (VRP with Pickup & Delivery).
    * Publie les `MissionOrders` dans Topic `commands`.
4.  **Distribution :**
    * `State Manager` reçoit `commands`, met à jour le statut des drones et notifie le Frontend.

## 5. Structure du Projet (Monorepo)

```text
drone-fleet-optimizer/
.
├── config
│   ├── dev.env
│   ├── local.env
│   └── prod.env
├── docs
│   ├── images
│   │   ├── global_architecture_png.png
│   │   └── global_architecture.svg
│   └── roadmap_technique.md
├── hello.py
├── infrastructure
│   ├── on_cloud
│   │   └── terraform
│   │       ├── environments
│   │       │   ├── dev
│   │       │   │   ├── backend.tf
│   │       │   │   ├── main.tf
│   │       │   │   ├── terraform.tfvars
│   │       │   │   └── variables.tf
│   │       │   └── prod
│   │       │       ├── backend.tf
│   │       │       ├── main.tf
│   │       │       ├── terraform.tfvars
│   │       │       └── variables.tf
│   │       └── modules
│   │           ├── cloud-run
│   │           │   └── main.tf
│   │           ├── firestore
│   │           ├── iam
│   │           │   └── main.tf
│   │           └── pubsub
│   │               ├── main.tf
│   │               ├── outputs.tf
│   │               └── variables.tf
│   └── on_premise
│       ├── create_topics.py
│       ├── debug_pubsub.py
│       └── docker-compose.yml
├── LICENSE
├── mise.toml
├── pubsub_tool.py
├── pyproject.toml
├── README.md
├── src
│   ├── ingestion-api
│   │   ├── __init__.py
│   │   ├── __pycache__
│   │   │   ├── __init__.cpython-311.pyc
│   │   │   └── main.cpython-311.pyc
│   │   ├── api
│   │   │   ├── __init__.py
│   │   │   ├── __pycache__
│   │   │   │   └── __init__.cpython-311.pyc
│   │   │   ├── tests
│   │   │   └── v1
│   │   │       ├── __init__.py
│   │   │       ├── __pycache__
│   │   │       │   └── __init__.cpython-311.pyc
│   │   │       └── endpoints
│   │   │           ├── __init__.py
│   │   │           ├── __pycache__
│   │   │           │   ├── __init__.cpython-311.pyc
│   │   │           │   ├── orders.cpython-311.pyc
│   │   │           │   ├── position.cpython-311.pyc
│   │   │           │   ├── states.cpython-311.pyc
│   │   │           │   └── telemetry.cpython-311.pyc
│   │   │           ├── orders.py
│   │   │           ├── states.py
│   │   │           └── telemetry.py
│   │   ├── Dockerfile
│   │   ├── main.py
│   │   ├── messaging
│   │   │   ├── __init__.py
│   │   │   ├── __pycache__
│   │   │   │   ├── __init__.cpython-311.pyc
│   │   │   │   ├── base_publisher.cpython-311.pyc
│   │   │   │   └── factory.cpython-311.pyc
│   │   │   ├── base_publisher.py
│   │   │   ├── factory.py
│   │   │   └── publisher
│   │   │       ├── __pycache__
│   │   │       │   ├── kafka_publisher.cpython-311.pyc
│   │   │       │   └── pubsub_publisher.cpython-311.pyc
│   │   │       ├── kafka_publisher.py
│   │   │       └── pubsub_publisher.py
│   │   ├── README.md
│   │   └── services
│   │       ├── __init__.py
│   │       ├── __pycache__
│   │       │   ├── __init__.cpython-311.pyc
│   │       │   ├── request.cpython-311.pyc
│   │       │   └── telemetry.cpython-311.pyc
│   │       ├── archives.py
│   │       ├── request.py
│   │       ├── states.py
│   │       └── telemetry.py
│   ├── shared
│   │   ├── configs
│   │   │   └── global_config.py
│   │   └── schemas
│   │       ├── __init__.py
│   │       ├── __pycache__
│   │       │   ├── __init__.cpython-311.pyc
│   │       │   ├── product.cpython-311.pyc
│   │       │   ├── protocol.cpython-311.pyc
│   │       │   ├── request.cpython-311.pyc
│   │       │   └── telemetry.cpython-311.pyc
│   │       ├── drones.py
│   │       ├── mission.py
│   │       ├── product.py
│   │       ├── protocol.py
│   │       ├── request.py
│   │       ├── telemetry.py
│   │       └── warehouses.py
│   └── simulator
│       └── main.py
└── uv.lock
```

## 6. Environnements isolés

```
┌──────────────────────────────────────────────────────────┐
│           STRATÉGIE MULTI-ENVIRONNEMENTS                 │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  Chaque environnement = Projet GCP isolé                 │
│                                                          │
│  ┌─────────────────────────────────────────────────┐     │
│  │ LOCAL (Dev Machine)                             │     │
│  │ - Émulateurs (Pub/Sub, Firestore)               │     │
│  │ - Docker Compose                                │     │
│  │ - Pas de coût GCP                               │     │
│  └─────────────────────────────────────────────────┘     │
│                         │                                │
│                         ▼                                │
│  ┌─────────────────────────────────────────────────┐     │
│  │ DEV (GCP Project: drone-fleet-optimizer-dev)    │     │
│  │ - Services GCP réels (Pub/Sub, Firestore)       │     │
│  │ - Deploy automatique sur push 'develop'         │     │
│  │ - Données de test                               │     │
│  └─────────────────────────────────────────────────┘     │
│                         │                                │
│                         ▼                                │
│  ┌─────────────────────────────────────────────────┐     │
│  │ PROD (GCP Project: drone-fleet-optimizer-prod)  │     │
│  │ - Environnement de production                   │     │
│  │ - Deploy seulement via release tags             │     │
│  │ - Monitoring strict                             │     │
│  └─────────────────────────────────────────────────┘     │
│                                                          │
└──────────────────────────────────────────────────────────┘

main (prod, deploy manuel)
  │
  ├── develop (dev, deploy auto lors d'un merge)
  │     │
  │     ├── feature/add-battery-optimization (local)
  │     ├── feature/new-optimizer-algorithm
  │     └── bugfix/fix-firestore-batch
  │
  └── hotfix/critical-pubsub-fix (merge direct dans main)
```

# Architecture

Le système implémente une **architecture de microservices polyglotte** suivant le modèle hexagonal pour l'indépendance de l'infrastructure :

![Diagramme d'architecture](images/global_architecture_png.png)

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

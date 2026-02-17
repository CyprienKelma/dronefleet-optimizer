# Architecture

The system implements a **polyglot microservices architecture** with hexagonal pattern for infrastructure independence:

![Architecture Diagram](images/global_architecture_png.png)

## Architectural Principles

- **Event-Driven**: Pub/Sub message bus decouples components
- **Polyglot**: Python (FastAPI), Java (Spring Boot), TypeScript (SolidJS) for optimal tool-task matching
- **Hexagonal Architecture**: Domain logic isolated from infrastructure via ports and adapters
- **Cloud-Native**: Designed for Google Cloud Platform with local emulation support
- **Infrastructure as Code**: Complete Terraform definitions for reproducible deployments

## Environments

```
LOCAL → DEV → PROD
```

- **LOCAL**: Docker Compose with Pub/Sub and Firestore emulators (zero GCP cost)
- **DEV**: Full GCP services with automatic deployment on push to main branch
- **PROD**: Production environment with manual deployment via release tags

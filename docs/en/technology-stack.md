# Technology Stack

| Component | Technology | Infrastructure | Purpose |
|-----------|-----------|----------------|---------|
| **Ingestion API** | Python 3.11 / FastAPI | Cloud Run (Service) | Gateway for telemetry and orders, JSON validation, Pub/Sub publishing |
| **Message Bus** | Google Pub/Sub | Pub/Sub / Emulator | Asynchronous event distribution with dead letter queue |
| **State Manager** | Java 21 / Spring Boot 4 | Cloud Run (Service) | State consistency, Firestore transactions, snapshot generation |
| **Optimizer** | Python 3.11 / OR-Tools | Cloud Run (Job) | Vehicle Routing Problem solver with pickup-delivery constraints |
| **Database** | Firestore Native | Firestore / Emulator | Hot storage for real-time state (drones, orders, missions) |
| **Frontend** | TypeScript / SolidJS | Cloud Run (Service) | Real-time map visualization (WebSocket) |
| **Analytics** | BigQuery | BigQuery | Historical data warehouse (work in progress) |

## Shared Model Layer

All components share a single source of truth for data models via **Protocol Buffers**:

![Proto Diagram](images/buf_logic.png)

- Definitions in `shared/proto/dronefleet/v1/*.proto`
- Generated code for Java, Python, TypeScript
- Validation via Buf (linting, breaking change detection)
- Automated synchronization enforced by pre-commit hooks and CI/CD

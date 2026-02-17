# Getting Started

## Prerequisites

- **Docker** and Docker Compose
- **Mise** (polyglot tool version manager) - [Installation](https://mise.jdx.dev/)
- **uv** (Python package manager) - Installed via mise
- **Buf** (Protobuf tooling) - Installed via mise
- **Java 21** (Temurin distribution)
- **Bun** (TypeScript runtime)

## Local Setup

1. **Clone the repository**

```bash
git clone https://github.com/CyprienKelma/dronefleet-optimizer.git
cd dronefleet-optimizer
```

2. **Install tool versions via mise**

```bash
mise install
```

3. **Generate shared models from protobuf definitions**

```bash
mise run //shared/proto:generate
```

4. **Start infrastructure with Docker Compose**

```bash
cd infra/local
docker-compose up -d --build
```

This starts:
- Pub/Sub emulator (port 8085)
- Firestore emulator (port 8080)

5. **Create Pub/Sub topics**

```bash
mise run //infra/local:create-topics
```

6. **Start services (in separate terminals)**

```bash
# Ingestion API
cd services/ingestion
mise run dev

# State Manager
cd services/state_manager
./gradlew bootRun --args='--spring.profiles.active=local'

# Path Optimizer (manual trigger for testing)
cd services/path_optimizer
mise run start

# Simulator
cd services/simulators
mise run dev
```

7. **Verify system is running**

Check Firestore emulator UI: http://localhost:4000
Check Ingestion API docs: http://localhost:8000/docs

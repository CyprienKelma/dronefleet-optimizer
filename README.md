# DroneFleet Optimizer

[![English](https://img.shields.io/badge/Language-English-blue?style=for-the-badge)](README.md)
[![Français](https://img.shields.io/badge/Langue-Français-gray?style=for-the-badge)](README.fr.md)

## What's this repo ?

This project is a complete real-time cloud management system for emergency medical delivery drone fleets.

It's based on an event-driven architecture deployed on GCP. With a complete CI/CD deployment, as well as a data Simulator and an ELT pipeline to process and analyse data using BigQuery.

This is a personal project I completed during my final year of computer engineering studies to put into practice all the concepts I learned that I enjoyed the most.

My ultimate goal was to design and implement an end-to-end data infrastructure: from data generation (simulating a live source system) through the ingestion, operational research solving, and real-time flow management, to a medallion architecture for data cleaning, transformation, and analytics.

It also allowed me to deepen my understanding of concepts such as concurrency management, containers, event-driven architecture, monorepo project organization, continuous integration/deployment, and cloud deployment.

### **View the Full Documentation:** [DroneFleet Optimizer Documentation](https://CyprienKelma.github.io/dronefleet-optimizer/)

<img src="docs/images/drone_map_gif_demo.gif" alt="Description" width="900" height="600" />

## Getting Started

### Prerequisites

- **Docker** and Docker Compose
- **Mise** (polyglot tool version manager) - [Installation](https://mise.jdx.dev/)
- **Java 21**
- **uv**, **Buf**, **Bun** (managed via mise)

### Quick Start

1. **Clone and Setup**
   ```bash
   git clone https://github.com/CyprienKelma/dronefleet-optimizer.git
   cd dronefleet-optimizer
   mise install
   mise run //shared/proto:generate
   ```

2. **Start Infrastructure**
   ```bash
   cd infra/local
   docker-compose up -d
   mise run //infra/local:create-topics
   ```

3. **Run Services** (in separate terminals)
   ```bash
   mise //services/ingestion:run
   mise //services/state_manager:run
   mise //services/simulators:run
   ```

For detailed instructions, dpeloyment en and architecture deep dives, please visit the [Full Documentation Site](https://CyprienKelma.github.io/dronefleet-optimizer/).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

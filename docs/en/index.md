<img src="images/logo_header.png" alt="Header Logo" width="1200" height="480" />

# What's this repo ?
This project is a complete real-time cloud management system for emergency medical delivery drone fleets.

It's based on an event-driven architecture deployed on GCP. With a complete CI/CD deployment, as well as a data Simulator and an ELT pipeline to process and analyse data using BigQuery.

<img src="images/drone_map_gif_demo.gif" alt="Description" width="900" height="600" />

This is a personal project I completed during my final year of computer engineering studies, with the aim of putting into practice the concepts that interest me most.

My goal was to design and implement an end-to-end data infrastructure: from data generation (simulation of a source system) to ingestion, operational research problem solving, real-time flow management with messaging queue, and a “medallion” architecture for data cleaning, transformation, and analysis.

This project allowed me to deepen my mastery of concepts such as concurrency management, containerization, event-driven architectures, monorepo organization, CI/CD, isolation of environments and cloud deployment. As well as using Python (Pydantic, FastAPI), Java (with Spring Boot), protobuf and buf library to manage single source of truth, and also designing complete CI/CD deployment with GitHub Action

![colored Architecture Diagram](images/colored_diagram.png)

You can download it for free by following the [Getting Started](getting-started.md) steps, or read on [Overview](overview.md) to learn more about how it works and the technical choices made, as well as broader considerations and reflections on the creation and management of such a system.

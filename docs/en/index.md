# DroneFleet Optimizer

## What's this repo ?

This project is a complete real-time cloud management system for emergency medical delivery drone fleets.

It's based on an event-driven architecture deployed on GCP. With a complete CI/CD deployment, as well as a data Simulator and an ELT pipeline to process and analyse data using BigQuery.

<img src="images/drone_map_gif_demo.gif" alt="Description" width="900" height="600" />

This is a personal project I completed during my final year of computer engineering studies to put into practice all the concepts I learned that I enjoyed the most.

My ultimate goal was to design and implement an end-to-end data infrastructure: from data generation (simulating a live source system) through the ingestion, operational research solving, and real-time flow management, to a medallion architecture for data cleaning, transformation, and analytics.

It also allowed me to deepen my understanding of concepts such as concurrency management, containers, event-driven architecture, monorepo project organization, continuous integration/deployment, and cloud deployment.

You can download it for free by following the [Getting Started](getting-started.md) steps, or read on [Overview](overview.md) to learn more about how it works and the technical choices made, as well as broader considerations and reflections on the creation and management of such a system.

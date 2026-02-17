# DroneFleet Optimizer

[![English](https://img.shields.io/badge/Language-English-gray?style=for-the-badge)](README.md)
[![Français](https://img.shields.io/badge/Langue-Français-blue?style=for-the-badge)](README.fr.md)

## Qu'est-ce que ce repo ?

Ce projet est un système complet de gestion cloud en temps réel pour des flottes de drones de livraison médicale d'urgence.

Il est basé sur une architecture pilotée par les événements (event-driven) déployée sur GCP. Il inclut un pipeline CI/CD complet, un simulateur de données et un pipeline ELT pour traiter et analyser les données via BigQuery.

Il s'agit d'un projet personnel réalisé lors de ma dernière année d'études d'ingénieur en informatique, visant à mettre en pratique les concepts technologiques qui me passionnent le plus.

> **🚀 Voir la documentation complète :** [https://CyprienKelma.github.io/dronefleet-optimizer/fr/](https://CyprienKelma.github.io/dronefleet-optimizer/fr/)

<img src="docs/images/drone_map_gif_demo.gif" alt="Description" width="900" height="600" />

## Mise en place

### Prérequis

- **Docker** et Docker Compose
- **Mise** (gestionnaire de versions d'outils polyglotte) - [Installation](https://mise.jdx.dev/)
- **Java 21**
- **uv**, **Buf**, **Bun** (gérés via mise)

### Démarrage rapide

1. **Clonage et configuration**
   ```bash
   git clone https://github.com/CyprienKelma/dronefleet-optimizer.git
   cd dronefleet-optimizer
   mise install
   mise run //shared/proto:generate
   ```

2. **Lancer l'infrastructure**
   ```bash
   cd infra/local
   docker-compose up -d
   mise run //infra/local:create-topics
   ```

3. **Lancer les services** (dans des terminaux séparés)
   ```bash
   mise //services/ingestion:run
   mise //services/state_manager:run
   mise //services/simulators:run
   ```

Pour des instructions détaillées et une analyse approfondie de l'architecture, veuillez consulter le [Site de documentation complète](https://CyprienKelma.github.io/dronefleet-optimizer/fr/).

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.

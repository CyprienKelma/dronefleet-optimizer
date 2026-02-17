# DroneFleet Optimizer

## Qu'est-ce que ce repo ?

Ce projet est un système complet de gestion cloud en temps réel pour des flottes de drones de livraison médicale d'urgence.

Il est basé sur une architecture pilotée par les événements (event-driven) déployée sur GCP. Il inclut un pipeline CI/CD complet, un simulateur de données et un pipeline ELT pour traiter et analyser les données via BigQuery.

<img src="images/drone_map_gif_demo.gif" alt="Description" width="900" height="600" />

Il s'agit d'un projet personnel réalisé lors de ma dernière année d'études d'ingénieur informatique, visant à mettre en pratique les concepts technologiques qui me passionnent le plus.

Mon objectif ultime était de concevoir et d'implémenter une infrastructure de données de bout en bout : de la génération de données (simulation d'un système source) à l'ingestion, en passant par la résolution de problèmes de recherche opérationnelle, la gestion des flux en temps réel, et jusqu'à une architecture "médaillon" pour le nettoyage, la transformation et l'analyse des données.

Ce projet m'a permis d'approfondir ma maîtrise de concepts tels que la gestion de la concurrence, la conteneurisation, les architectures événementielles, l'organisation en monorepo, le CI/CD et le déploiement cloud.

![Zoomed Architecture Diagram](images/zoomed_archi.png)s

Vous pouvez tester le projet gratuitement en suivant les étapes de [Mise en Place](getting-started.md), ou poursuivre votre lecture vers [Overview](overview.md) pour découvrir son fonctionnement, les choix techniques effectués et mes réflexions sur la conception d'un tel système.

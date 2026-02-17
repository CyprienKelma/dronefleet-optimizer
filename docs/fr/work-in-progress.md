# Travaux en cours

### 1. Visualisation Frontend (En développement)
**Techno** : SolidJS + Leaflet + WebSocket. Carte temps réel, indicateurs de batterie, trajet des missions et dashboard de métriques.

### 2. Pipeline Analytique BigQuery (Prévu)
**Objectif** : Entrepôt de données historiques.
**Architecture** : Abonnements Pub/Sub to BigQuery pour insertion en direct, puis transformations dbt pour générer des vues "gold" sur la performance des drones, le respect des SLAs et l'efficacité des entrepôts.

### 3. Simulation d'exécution de mission (En développement)
Le simulateur va s'abonner au topic `decisions`, parser les `MissionAssignment`, et simuler le mouvement réel le long du trajet en publiant la télémétrie correspondante.

---

**Statut du projet** : Développement actif. Moteur d'optimisation et gestionnaire d'état terminés. Visualisation frontend et pipeline analytique en cours.

**Contact** : Pour toute question ou collaboration, merci d'ouvrir une issue sur GitHub.

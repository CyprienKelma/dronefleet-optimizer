# Roadmap Technique & Améliorations "Enterprise Grade"

Ce document recense les fonctionnalités et pratiques techniques avancées à implémenter pour élever le niveau du projet **DroneFleet Optimizer** vers un standard "Senior / Architecte".

Ces points sont issus de la discussion sur la crédibilité technique et l'aspect "Impressionnant" du projet.

## 1. Priorité 1 : Observabilité & Qualité Industrielle (The "Must-Have")
*Socle indispensable pour une architecture distribuée crédible.*

*   [ ] **Observabilité Complète (OpenTelemetry)**
    *   **Objectif :** Traces distribuées traversant la stack polyglotte (FastAPI → Pub/Sub → Java Spring Boot).
    *   **Implémentation :** Instrumentation automatique des services.
    *   **Visualisation :** Dashboard Grafana exposant les Golden Signals (Latence P95, Error Rate, Saturation).
    *   **Logs :** Logs structurés JSON avec `correlation_id` pour lier les logs aux traces.

*   [ ] **Tests d'Intégration Réalistes (TestContainers)**
    *   **Objectif :** Ne pas mocker l'infrastructure critique dans les tests d'intégration.
    *   **Outil :** Utilisation de `TestContainers` (Java & Python) pour lancer de vrais conteneurs Pub/Sub et Firestore éphémères pendant les tests.
    *   **CI/CD :** Pipeline GitHub Actions (Lint → Test → Build).

*   [ ] **Documentation d'Architecte**
    *   **ADR (Architecture Decision Records) :** Documenter les choix structurants (ex: Pourquoi Java pour le State Manager ? Pourquoi Pub/Sub ?).
    *   **Runbook :** Procédures de debugging, rollback et gestion d'incidents.
    *   **API Specs :** OpenAPI (Swagger) généré automatiquement avec exemples.

## 2. Priorité 2 : Résilience & Performance (The "High-Value")
*Démontrer la maîtrise des systèmes distribués faillibles.*

*   [ ] **Démonstration de Résilience (Chaos Engineering)**
    *   **Scénario :** Simuler un crash du `State Manager` ou une indisponibilité temporaire de la base de données.
    *   **Mécanismes à prouver :**
        *   Retry Policy avec Exponential Backoff (visible dans les logs/traces).
        *   Dead Letter Queue (DLQ) pour les messages "empoisonnés" ou non traitables.
        *   Recovery automatique sans perte de données ("At-Least-Once").

*   [ ] **Tests de Charge (Load Testing)**
    *   **Outil :** Locust ou K6.
    *   **Objectif :** Prouver la tenue de charge (ex: 500 events/s) et valider le SLA de latence (< 500ms).
    *   **Livrable :** Rapport de performance (graphes latence vs throughput).

## 3. Priorité 3 : Intelligence & Produit (The "Nice-to-Have")
*Fonctionnalités avancées à ajouter une fois le socle technique stabilisé.*

*   [ ] **Optimisation Prédictive (Machine Learning)**
    *   Ajout d'un modèle simple pour prédire les zones de demande et pré-positionner les drones (vs réactif pur).
    *   *Note : À faire seulement si l'algo OR-Tools de base est parfaitement stable.*

*   [ ] **Feature Flipping & A/B Testing**
    *   Implémenter deux stratégies d'optimisation (ex: "Rapide" vs "Économe").
    *   Comparer les métriques business en temps réel.

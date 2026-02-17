# Décisions de conception

### Pourquoi une architecture polyglotte ?
J'ai choisi **Python pour l'API d'ingestion et l'Optimiseur** car ces services ont des besoins très différents. FastAPI est idéal pour les charges asynchrones à haut débit sans blocage. Concernant l'optimiseur, Google OR-Tools est la référence absolue pour les problèmes de tournées avec un support Python de premier plan. Plutôt que de forcer un langage unique, j'utilise l'outil le plus adapté à chaque tâche.

**Java propulse le Gestionnaire d'état** car c'est là que réside la logique métier complexe et critique. Le typage fort de Java et ses garanties à la compilation évitent des catégories entières de bugs. La gestion des transactions Spring Boot et la maturité du SDK Firestore en font le choix naturel pour un service qui doit être ultra-robuste.

**TypeScript gère le Frontend** car la gestion d'état UI réactive en temps réel est une priorité. SolidJS offre une réactivité fine — seules les parties du DOM que change sont rerendus — ce qui est crucial pour afficher des milliers de mises à jour de position par seconde.

### Pourquoi Firestore plutôt que PostgreSQL ?
Firestore a été choisi pour ce problème spécifique :
- **Géographique** : Le type natif `GeoPoint` facilite les requêtes spatiales sans gérer de colonnes lat/lon manuellement.
- **Scalabilité** : Mise à l'échelle automatique sans administration de base de données.
- **Atomicité** : Les transactions Firestore sont puissantes et intégrées au SDK.
Le compromis est une capacité de requête moins riche que le SQL (pas de joins complexes), mais les patterns d'accès du Gestionnaire d'état (lecture/écriture individuelle ou requêtes simples indexées) correspondent exactement aux forces de Firestore. Le coût est optimisé via des écritures par lots.

### Modèle de concurrence : First-Write-Wins
J'ai fait un compromis délibéré entre simplicité et efficacité de calcul. J'ai choisi le **"Le premier qui écrit gagne" avec verrouillage optimiste**. Un verrouillage pessimiste (`RESERVED`/`SOLVING`) éliminerait le gaspillage de calcul mais introduirait une complexité énorme (gestion de verrous distribués, timeouts, risque de deadlock). Vu que les cycles d'optimisation sont courts, rejeter quelques décisions occasionnellement est un prix acceptable pour la robustesse et la simplicité du système.

### Piloté par les événements vs requête-réponse
Le système utilise une stratégie hybride.
**Télémétrie et commandes via Pub/Sub** : flux asynchrones à haute fréquence qui n'exigent pas de réponse immédiate. Cela découple producteurs et consommateurs et offre un tampon naturel.
**Snapshots d'optimisation via HTTP GET synchrone** : L'Optimiseur a besoin d'une vue cohérente à un instant T. Le HTTP GET est plus direct, plus simple à gérer avec des retries et permet de "fail fast".

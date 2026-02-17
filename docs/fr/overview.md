# Vue d'ensemble

DroneFleet Optimizer est une plateforme logistique autonome capable de livrer des fournitures médicales d'urgence (sang, vaccins, défibrillateurs) en moins de 15 minutes en zone urbaine, en coordonnant une flotte de drones via un algorithme d'optimisation centralisé.

### Contexte métier

Le système répond à des défis critiques de logistique médicale :

- Optimiser les itinéraires de livraison pour 50 à 100 drones simultanés.
- Respecter des SLAs stricts : commandes critiques livrées en moins de 15 minutes, haute priorité en moins de 30 minutes.
- Gérer les contraintes en temps réel : niveaux de batterie, fenêtres de temps, compatibilité entre entrepôts et produits.
- Garantir la cohérence des données et la tolérance aux pannes entre les composants distribués.

### Métriques clés

- **Latence** : < 500ms pour les mises à jour en temps réel.
- **Cycle d'optimisation** : Planification sur un horizon roulant de 10 secondes.
- **Échelle** : 50 à 100 drones actifs en phase MVP.
- **Fiabilité** : Garantie de livraison "au moins une fois" (at-least-once) sans perte de commande.
- **Optimisation des coûts** : Écritures par lots (batch) dans Firestore pour rester dans le niveau gratuit pendant le développement.

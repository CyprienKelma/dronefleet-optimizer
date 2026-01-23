## Acteurs
- **Hôpitaux** : Clients qui demandent des livraisons urgentes
- **Drones** : Véhicules autonomes qui effectuent les livraisons
- **Dispatcher** : Humain qui supervise les opérations
- **Optimizer** : Algorithme qui assigne les drones

## Cas d'Usage Prioritaires (MVP)

### UC1 : Créer une Demande de Livraison
**Acteur** : Hôpital
**Préconditions** : L'hôpital est enregistré dans le système
**Scénario** :
1. L'hôpital envoie une demande (pickup, delivery, priority, cargo)
2. Le système valide la demande
3. Le système crée une Order avec statut PENDING
4. Le système notifie le dispatcher

**Postconditions** : Une Order existe dans le système avec statut PENDING

### UC2 : Mettre à Jour la Position d'un Drone
**Acteur** : Drone
**Préconditions** : Le drone est enregistré
**Scénario** :
1. Le drone envoie sa position toutes les 1 seconde
2. Le système met à jour l'état en mémoire
3. Le système vérifie la batterie
4. Si batterie < 20%, le système change le statut à LOW_BATTERY

**Postconditions** : La position du drone est à jour

### UC3 : Assigner un Drone à une Order
**Acteur** : Optimizer
**Préconditions** : Il existe au moins une Order PENDING et un Drone AVAILABLE
**Scénario** :
1. L'optimizer calcule la meilleure assignation
2. L'optimizer envoie une commande d'assignation
3. Le système crée une Mission (drone + order + route)
4. Le système change le statut du drone à IN_MISSION
5. Le système change le statut de l'order à ASSIGNED

**Postconditions** : Une Mission existe, le drone et l'order ont les bons statuts

## Règles Métier

### R1 : Disponibilité des Drones
Un drone est disponible si :
- Status = AVAILABLE
- Battery > 20%
- Pas de mission active

### R2 : Priorité des Orders
Les orders sont traitées dans cet ordre :
1. HIGH priority
2. MEDIUM priority
3. LOW priority

En cas d'égalité, FIFO (First In First Out).

### R3 : Contrainte de Capacité
Un drone ne peut transporter qu'un seul colis à la fois.

## Événements Métier

### OrderCreated
```json
{
  "orderId": "ORD001",
  "pickupLocation": {"lat": 50.6, "lon": 3.1},
  "deliveryLocation": {"lat": 50.7, "lon": 3.2},
  "priority": "HIGH",
  "cargo": "Blood pack - O+",
  "timestamp": "2026-01-19T10:30:00Z"
}
```

### DroneTelemetryReceived
```json
{
  "droneId": "D001",
  "position": {"lat": 50.6, "lon": 3.1},
  "batteryLevel": 85.5,
  "status": "AVAILABLE",
  "timestamp": "2026-01-19T10:30:01Z"
}
```

### MissionAssigned
```json
{
  "missionId": "M001",
  "droneId": "D001",
  "orderId": "ORD001",
  "route": [...],
  "timestamp": "2026-01-19T10:30:05Z"
}
```

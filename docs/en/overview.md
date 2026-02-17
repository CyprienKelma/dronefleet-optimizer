# Overview

DroneFleet Optimizer is an autonomous logistics platform capable of delivering emergency medical supplies (blood, vaccines, defibrillators) within 15 minutes in urban areas by coordinating a fleet of drones through a centralized optimization algorithm.

## Business Context

The system addresses critical medical logistics challenges by:

- Optimizing delivery routes for 50-100 simultaneous drones
- Meeting strict SLAs: critical orders delivered within 15 minutes, high priority within 30 minutes
- Handling real-time constraints: battery levels, time windows, warehouse-product compatibility
- Ensuring data consistency and fault tolerance across distributed components

## Key Metrics

- **Latency**: < 500ms for real-time updates
- **Optimization Cycle**: 10-second rolling horizon planning
- **Scale**: 50-100 active drones in MVP phase
- **Reliability**: At-least-once delivery guarantee with no lost orders
- **Cost Optimization**: Firestore batch writes to stay within free tier during development

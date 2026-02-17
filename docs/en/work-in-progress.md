# Work in Progress

## 1. Frontend Visualization (In Development)

**Technology**: SolidJS + Leaflet + WebSocket

**Features**:
- Real-time map with drone positions
- Active mission routes displayed
- Battery levels and drone status indicators
- Order queue visualization
- Metrics dashboard (orders per minute, average delivery time, fleet utilization)

**Architecture**:
- WebSocket server (TypeScript/Bun) subscribes to `telemetry` Pub/Sub topic
- Frontend connects via WebSocket for live updates
- Fallback to HTTP polling for resilience

## 2. BigQuery Analytics Pipeline (Planned)

**Objective**: Historical data warehouse for post-hoc analysis and reporting

**Architecture**:

```
Pub/Sub Topics → BigQuery Subscriptions → BigQuery Tables
  (telemetry)        (streaming insert)       (raw layer)
  (orders)                                        ↓
  (decisions)                                dbt transformations
                                                     ↓
                                              BigQuery Views
                                                 (gold layer)
```

**Planned Tables**:

**Raw Layer**:
- `telemetry_raw` - Timestamped drone positions and battery levels
- `orders_raw` - Order creation and status updates
- `decisions_raw` - Mission assignments and rejections

**Gold Layer (dbt models)**:
- `drone_performance` - Metrics per drone (total distance, missions completed, average battery consumption)
- `order_sla_compliance` - Delivery times vs deadlines, priority analysis
- `fleet_utilization` - Idle time, active missions, capacity utilization
- `warehouse_efficiency` - Pickup frequency, average wait time

**Visualization**: Looker Studio dashboards for operational insights

## 3. Simulator Mission Execution (In Development)

**Current State**: Simulator generates telemetry and orders but does not consume missions

**Planned Enhancement**:
- Subscribe to `decisions` Pub/Sub topic
- Parse `MissionAssignment` messages
- Simulate drone movement along assigned route
- Publish telemetry at each waypoint
- Update order status upon delivery completion

**Implementation**: Asyncio-based event loop with concurrent drone agents

---

**Project Status**: Active development. Core optimization engine and state management complete. Frontend visualization and analytics pipeline in progress.

**Contact**: For questions or collaboration opportunities, please open an issue on GitHub.

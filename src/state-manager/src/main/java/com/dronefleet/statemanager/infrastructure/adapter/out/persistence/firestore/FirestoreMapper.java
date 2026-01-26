package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneStatus;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.model.Position;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared mapper for converting between Firestore documents and domain objects.
 */
@Component
public class FirestoreMapper {

    // --- Drone Mapping ---

    public Drone mapToDrone(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        Map<String, Object> posMap = (Map<String, Object>) doc.get("position");
        Position position = mapToPosition(posMap);

        Timestamp timestamp = doc.getTimestamp("lastUpdate");
        Instant lastUpdate = (timestamp != null) ? timestamp.toDate().toInstant() : null;

        return Drone.builder()
                .id(doc.getId())
                .position(position)
                .batteryPercentage(doc.getDouble("batteryPercentage") != null ? doc.getDouble("batteryPercentage") : 0.0)
                .speedKmh(doc.getDouble("speedKmh") != null ? doc.getDouble("speedKmh") : 0.0)
                .status(DroneStatus.parseStatus(doc.getString("status")))
                .currentMissionId(doc.getString("currentMissionId"))
                .lastUpdate(lastUpdate)
                .build();
    }

    public Map<String, Object> mapFromDrone(Drone drone) {
        Map<String, Object> map = new HashMap<>();
        map.put("batteryPercentage", drone.getBatteryPercentage());
        map.put("speedKmh", drone.getSpeedKmh());
        map.put("status", drone.getStatus().name());
        map.put("currentMissionId", drone.getCurrentMissionId());

        if (drone.getPosition() != null) {
            map.put("position", mapFromPosition(drone.getPosition()));
        }

        if (drone.getLastUpdate() != null) {
            map.put("lastUpdate", Timestamp.of(java.util.Date.from(drone.getLastUpdate())));
        }

        return map;
    }

    // --- Order Mapping ---

    public Order mapToOrder(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        return Order.builder()
                .id(doc.getId())
                .pickupLocation(mapToPosition((Map<String, Object>) doc.get("pickupLocation")))
                .deliveryLocation(mapToPosition((Map<String, Object>) doc.get("deliveryLocation")))
                .status(doc.getString("status"))
                .priority(doc.getString("priority"))
                .createdAt(doc.getTimestamp("createdAt") != null ? doc.getTimestamp("createdAt").toDate().toInstant() : null)
                .assignedDroneId(doc.getString("assignedDroneId"))
                .assignedMissionId(doc.getString("assignedMissionId"))
                .build();
    }

    public Map<String, Object> mapFromOrder(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", order.getStatus());
        map.put("priority", order.getPriority());
        map.put("assignedDroneId", order.getAssignedDroneId());
        map.put("assignedMissionId", order.getAssignedMissionId());

        if (order.getPickupLocation() != null) {
            map.put("pickupLocation", mapFromPosition(order.getPickupLocation()));
        }
        if (order.getDeliveryLocation() != null) {
            map.put("deliveryLocation", mapFromPosition(order.getDeliveryLocation()));
        }
        if (order.getCreatedAt() != null) {
            map.put("createdAt", Timestamp.of(java.util.Date.from(order.getCreatedAt())));
        }
        return map;
    }

    // --- Mission Mapping ---

    public Mission mapToMission(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        List<Map<String, Object>> routeMaps = (List<Map<String, Object>>) doc.get("route");
        List<Position> route = null;
        if (routeMaps != null) {
            route = routeMaps.stream()
                    .map(this::mapToPosition)
                    .collect(Collectors.toList());
        }

        return Mission.builder()
                .id(doc.getId())
                .droneId(doc.getString("droneId"))
                .orderId(doc.getString("orderId"))
                .route(route)
                .status(doc.getString("status"))
                .startTime(doc.getTimestamp("startTime") != null ? doc.getTimestamp("startTime").toDate().toInstant() : null)
                .endTime(doc.getTimestamp("endTime") != null ? doc.getTimestamp("endTime").toDate().toInstant() : null)
                .build();
    }

    public Map<String, Object> mapFromMission(Mission mission) {
        Map<String, Object> map = new HashMap<>();
        map.put("droneId", mission.getDroneId());
        map.put("orderId", mission.getOrderId());
        map.put("status", mission.getStatus());

        if (mission.getRoute() != null) {
            map.put("route", mission.getRoute().stream()
                    .map(this::mapFromPosition)
                    .collect(Collectors.toList()));
        }

        if (mission.getStartTime() != null) {
            map.put("startTime", Timestamp.of(java.util.Date.from(mission.getStartTime())));
        }
        if (mission.getEndTime() != null) {
            map.put("endTime", Timestamp.of(java.util.Date.from(mission.getEndTime())));
        }
        return map;
    }

    // --- Position Mapping ---

    public Position mapToPosition(Map<String, Object> map) {
        if (map == null) return null;
        return new Position(
                ((Number) map.get("lat")).doubleValue(),
                ((Number) map.get("lon")).doubleValue()
        );
    }

    public Map<String, Object> mapFromPosition(Position pos) {
        if (pos == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("lat", pos.lat());
        map.put("lon", pos.lon());
        return map;
    }
}

package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Depot;
import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.OrderPriority;
import com.dronefleet.shared.models.Position;
import com.dronefleet.shared.models.Warehouse;
import com.dronefleet.shared.models.Waypoint;
import com.dronefleet.shared.models.WaypointType;
import com.dronefleet.statemanager.domain.service.DronePolicy;
import com.dronefleet.statemanager.domain.service.OrderPolicy;

/** Shared mapper for converting between Firestore documents and domain objects. */
@Component
@RequiredArgsConstructor
public class FirestoreMapper {

    private final DronePolicy dronePolicy;
    private final OrderPolicy orderPolicy;

    // --- Drone Mapping ---

    public Drone mapToDrone(DocumentSnapshot doc) {
        if (!doc.exists()) {
            return null;
        }

        Map<String, Object> posMap = (Map<String, Object>) doc.get("position");
        Position position = mapToPosition(posMap);

        Timestamp timestamp = doc.getTimestamp("lastUpdate");
        com.google.protobuf.Timestamp protoTimestamp = null;
        if (timestamp != null) {
            protoTimestamp =
                    com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(timestamp.getSeconds())
                            .setNanos(timestamp.toDate().toInstant().getNano())
                            .build();
        }

        Drone.Builder builder =
                Drone.newBuilder()
                        .setId(doc.getId())
                        .setBatteryPercentage(
                                doc.getDouble("batteryPercentage") != null
                                        ? doc.getDouble("batteryPercentage")
                                        : 0.0)
                        .setSpeedKmh(
                                doc.getDouble("speedKmh") != null ? doc.getDouble("speedKmh") : 0.0)
                        .setStatus(dronePolicy.parseStatus(doc.getString("status")))
                        .setCurrentMissionId(
                                doc.getString("currentMissionId") != null
                                        ? doc.getString("currentMissionId")
                                        : "")
                        .setSolvingSessionId(
                                doc.getString("solvingSessionId") != null
                                        ? doc.getString("solvingSessionId")
                                        : "")
                        .setHomeDepotId(
                                doc.getString("homeDepotId") != null
                                        ? doc.getString("homeDepotId")
                                        : "")
                        .setBatteryCapacityMah(
                                doc.getLong("batteryCapacityMah") != null
                                        ? doc.getLong("batteryCapacityMah").intValue()
                                        : 0)
                        .setConsumptionPerKm(
                                doc.getDouble("consumptionPerKm") != null
                                        ? doc.getDouble("consumptionPerKm")
                                        : 0.0)
                        .setMaxFlightTimeMinutes(
                                doc.getLong("maxFlightTimeMinutes") != null
                                        ? doc.getLong("maxFlightTimeMinutes").intValue()
                                        : 0);

        if (position != null) {
            builder.setPosition(position);
        }
        if (protoTimestamp != null) {
            builder.setLastUpdate(protoTimestamp);
        }

        return builder.build();
    }

    public Map<String, Object> mapFromDrone(Drone drone) {
        Map<String, Object> map = new HashMap<>();
        map.put("batteryPercentage", drone.getBatteryPercentage());
        map.put("speedKmh", drone.getSpeedKmh());
        map.put("status", drone.getStatus().name());
        map.put("currentMissionId", drone.getCurrentMissionId());
        map.put("solvingSessionId", drone.getSolvingSessionId());
        map.put("homeDepotId", drone.getHomeDepotId());
        map.put("batteryCapacityMah", drone.getBatteryCapacityMah());
        map.put("consumptionPerKm", drone.getConsumptionPerKm());
        map.put("maxFlightTimeMinutes", drone.getMaxFlightTimeMinutes());

        if (drone.hasPosition()) {
            map.put("position", mapFromPosition(drone.getPosition()));
        }

        if (drone.hasLastUpdate()) {
            Instant lastUpdate =
                    Instant.ofEpochSecond(
                            drone.getLastUpdate().getSeconds(), drone.getLastUpdate().getNanos());
            map.put("lastUpdate", Timestamp.of(java.util.Date.from(lastUpdate)));
        }

        return map;
    }

    // --- Order Mapping ---

    public Order mapToOrder(DocumentSnapshot doc) {
        if (!doc.exists()) {
            return null;
        }

        Order.Builder builder =
                Order.newBuilder()
                        .setId(doc.getId())
                        .setStatus(orderPolicy.parseStatus(doc.getString("status")))
                        .setPriority(
                                doc.getString("priority") != null
                                        ? OrderPriority.valueOf(
                                                "ORDER_PRIORITY_"
                                                        + doc.getString("priority").toUpperCase())
                                        : OrderPriority.ORDER_PRIORITY_STANDARD)
                        .setProductType(
                                doc.getString("productType") != null
                                        ? doc.getString("productType")
                                        : "")
                        .setAssignedDroneId(
                                doc.getString("assignedDroneId") != null
                                        ? doc.getString("assignedDroneId")
                                        : "")
                        .setAssignedMissionId(
                                doc.getString("assignedMissionId") != null
                                        ? doc.getString("assignedMissionId")
                                        : "")
                        .setSolvingSessionId(
                                doc.getString("solvingSessionId") != null
                                        ? doc.getString("solvingSessionId")
                                        : "");

        Position pickup = mapToPosition((Map<String, Object>) doc.get("pickupLocation"));
        if (pickup != null) {
            builder.setPickupLocation(pickup);
        }

        Position delivery = mapToPosition((Map<String, Object>) doc.get("deliveryLocation"));
        if (delivery != null) {
            builder.setDeliveryLocation(delivery);
        }

        Timestamp ts = doc.getTimestamp("createdAt");
        if (ts != null) {
            builder.setCreatedAt(
                    com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(ts.getSeconds())
                            .setNanos(ts.toDate().toInstant().getNano())
                            .build());
        }

        return builder.build();
    }

    public Map<String, Object> mapFromOrder(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", order.getStatus().name());
        map.put("priority", order.getPriority().name().replace("ORDER_PRIORITY_", ""));
        map.put("productType", order.getProductType());
        map.put("assignedDroneId", order.getAssignedDroneId());
        map.put("assignedMissionId", order.getAssignedMissionId());
        map.put("solvingSessionId", order.getSolvingSessionId());

        if (order.hasPickupLocation()) {
            map.put("pickupLocation", mapFromPosition(order.getPickupLocation()));
        }
        if (order.hasDeliveryLocation()) {
            map.put("deliveryLocation", mapFromPosition(order.getDeliveryLocation()));
        }
        if (order.hasCreatedAt()) {
            Instant createdAt =
                    Instant.ofEpochSecond(
                            order.getCreatedAt().getSeconds(), order.getCreatedAt().getNanos());
            map.put("createdAt", Timestamp.of(java.util.Date.from(createdAt)));
        }
        return map;
    }

    // --- Mission Mapping ---

    public Mission mapToMission(DocumentSnapshot doc) {
        if (!doc.exists()) {
            return null;
        }

        List<Map<String, Object>> routeMaps = (List<Map<String, Object>>) doc.get("route");

        Mission.Builder builder =
                Mission.newBuilder()
                        .setId(doc.getId())
                        .setDroneId(
                                doc.getString("droneId") != null ? doc.getString("droneId") : "")
                        .addAllOrderIds((List<String>) doc.get("orderIds"))
                        .setStatus(doc.getString("status") != null ? doc.getString("status") : "")
                        .setEstimatedBatteryConsumption(
                                doc.getDouble("estimatedBatteryConsumption") != null
                                        ? doc.getDouble("estimatedBatteryConsumption")
                                        : 0.0)
                        .setEstimatedDurationMinutes(
                                doc.getDouble("estimatedDurationMinutes") != null
                                        ? doc.getDouble("estimatedDurationMinutes")
                                        : 0.0);

        if (routeMaps != null) {
            builder.addAllRoute(
                    routeMaps.stream().map(this::mapToWaypoint).collect(Collectors.toList()));
        }

        Timestamp start = doc.getTimestamp("startTime");
        if (start != null) {
            builder.setStartTime(
                    com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(start.getSeconds())
                            .setNanos(start.toDate().toInstant().getNano())
                            .build());
        }

        Timestamp end = doc.getTimestamp("endTime");
        if (end != null) {
            builder.setEndTime(
                    com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(end.getSeconds())
                            .setNanos(end.toDate().toInstant().getNano())
                            .build());
        }

        return builder.build();
    }

    public Map<String, Object> mapFromMission(Mission mission) {
        Map<String, Object> map = new HashMap<>();
        map.put("droneId", mission.getDroneId());
        map.put("orderIds", mission.getOrderIdsList());
        map.put("status", mission.getStatus());
        map.put("estimatedBatteryConsumption", mission.getEstimatedBatteryConsumption());
        map.put("estimatedDurationMinutes", mission.getEstimatedDurationMinutes());

        if (mission.getRouteCount() > 0) {
            map.put(
                    "route",
                    mission.getRouteList().stream()
                            .map(this::mapFromWaypoint)
                            .collect(Collectors.toList()));
        }

        if (mission.hasStartTime()) {
            Instant start =
                    Instant.ofEpochSecond(
                            mission.getStartTime().getSeconds(), mission.getStartTime().getNanos());
            map.put("startTime", Timestamp.of(java.util.Date.from(start)));
        }
        if (mission.hasEndTime()) {
            Instant end =
                    Instant.ofEpochSecond(
                            mission.getEndTime().getSeconds(), mission.getEndTime().getNanos());
            map.put("endTime", Timestamp.of(java.util.Date.from(end)));
        }
        return map;
    }

    // --- Waypoint Mapping ---

    private Waypoint mapToWaypoint(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Waypoint.Builder builder =
                Waypoint.newBuilder()
                        .setType(WaypointType.valueOf((String) map.get("type")))
                        .setRelatedOrderId(
                                (String) map.get("relatedOrderId") != null
                                        ? (String) map.get("relatedOrderId")
                                        : "")
                        .setRelatedWarehouseId(
                                (String) map.get("relatedWarehouseId") != null
                                        ? (String) map.get("relatedWarehouseId")
                                        : "");

        Position pos = mapToPosition((Map<String, Object>) map.get("position"));
        if (pos != null) {
            builder.setPosition(pos);
        }

        return builder.build();
    }

    private Map<String, Object> mapFromWaypoint(Waypoint waypoint) {
        if (waypoint == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("type", waypoint.getType().name());
        if (waypoint.hasPosition()) {
            map.put("position", mapFromPosition(waypoint.getPosition()));
        }
        map.put("relatedOrderId", waypoint.getRelatedOrderId());
        map.put("relatedWarehouseId", waypoint.getRelatedWarehouseId());
        return map;
    }

    // --- Position Mapping ---

    public Position mapToPosition(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return Position.newBuilder()
                .setLat(((Number) map.get("lat")).doubleValue())
                .setLon(((Number) map.get("lon")).doubleValue())
                .build();
    }

    public Map<String, Object> mapFromPosition(Position pos) {
        if (pos == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("lat", pos.getLat());
        map.put("lon", pos.getLon());
        return map;
    }

    // --- Warehouse Mapping ---

    public Warehouse mapToWarehouse(DocumentSnapshot doc) {
        if (!doc.exists()) {
            return null;
        }

        Warehouse.Builder builder =
                Warehouse.newBuilder()
                        .setId(doc.getId())
                        .setName(doc.getString("name") != null ? doc.getString("name") : "")
                        .setIsColdStorageCapable(
                                doc.getBoolean("isColdStorageCapable") != null
                                        && doc.getBoolean("isColdStorageCapable"));

        Position pos = mapToPosition((Map<String, Object>) doc.get("position"));
        if (pos != null) {
            builder.setPosition(pos);
        }

        List<String> types = (List<String>) doc.get("authorizedProductTypes");
        if (types != null) {
            builder.addAllAuthorizedProductTypes(types);
        }

        return builder.build();
    }

    public Map<String, Object> mapFromWarehouse(Warehouse warehouse) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", warehouse.getName());
        map.put("authorizedProductTypes", warehouse.getAuthorizedProductTypesList());
        map.put("isColdStorageCapable", warehouse.getIsColdStorageCapable());

        if (warehouse.hasPosition()) {
            map.put("position", mapFromPosition(warehouse.getPosition()));
        }

        return map;
    }

    // --- Depot Mapping ---

    public Depot mapToDepot(DocumentSnapshot doc) {
        if (!doc.exists()) {
            return null;
        }

        Depot.Builder builder =
                Depot.newBuilder()
                        .setId(doc.getId())
                        .setName(doc.getString("name") != null ? doc.getString("name") : "")
                        .setCapacity(
                                doc.getLong("capacity") != null
                                        ? doc.getLong("capacity").intValue()
                                        : 0)
                        .setChargingSlots(
                                doc.getLong("chargingSlots") != null
                                        ? doc.getLong("chargingSlots").intValue()
                                        : 0);

        Position pos = mapToPosition((Map<String, Object>) doc.get("position"));
        if (pos != null) {
            builder.setPosition(pos);
        }

        return builder.build();
    }

    public Map<String, Object> mapFromDepot(Depot depot) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", depot.getName());
        map.put("capacity", depot.getCapacity());
        map.put("chargingSlots", depot.getChargingSlots());

        if (depot.hasPosition()) {
            map.put("position", mapFromPosition(depot.getPosition()));
        }

        return map;
    }
}

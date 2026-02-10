package com.dronefleet.statemanager.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.state-manager")
public class AppProperties {

    private int batchWriteInterval;

    private int maxBatchSize;

    private String dronesCollection = "drones";

    private String ordersCollection = "orders";

    private String missionsCollection = "missions";

    private String warehousesCollection = "warehouses";

    private String depotsCollection = "depots";

    private int minBatteryForOptimization = 20;
}

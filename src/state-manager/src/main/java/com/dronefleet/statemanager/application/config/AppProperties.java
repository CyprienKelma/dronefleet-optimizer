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

    private String dronesCollection;

    private String ordersCollection;

    private String missionsCollection;

    private String warehousesCollection;

    private int minBatteryForOptimization = 20;
}

package com.dronefleet.statemanager;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requires GCP emulators (Pub/Sub + Firestore)")
@SpringBootTest(
        classes = StateManagerApplication.class,
        properties = {
            "spring.cloud.gcp.project-id=test-project",
            "pubsub.subscriptions.telemetry=telemetry-sub",
            "pubsub.subscriptions.orders=orders-sub",
            "pubsub.subscriptions.decisions=decisions-sub"
        })
class StateManagerApplicationTests {

    @Test
    void contextLoads() {}
}

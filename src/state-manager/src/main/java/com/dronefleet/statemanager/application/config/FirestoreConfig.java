package com.dronefleet.statemanager.application.config;

import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    @Profile("local")
    public Firestore firestoreLocal(@Value("${spring.cloud.gcp.firestore.host-port:localhost:8080}") String hostPort) {
        return FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setHost(hostPort)
                .setCredentials(NoCredentials.getInstance())
                .build()
                .getService();
    }

    @Bean
    @Profile("!local")
    public Firestore firestoreCloud() {
        return FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}

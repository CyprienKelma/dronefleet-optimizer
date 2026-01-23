package com.dronefleet.statemanager.application.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.NoCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration specific to the 'local' environment.
 */
@Configuration
@Profile("local")
public class LocalGcpConfig {

    @Bean
    public CredentialsProvider googleCredentials() {
        return FixedCredentialsProvider.create(NoCredentials.getInstance());
    }
}

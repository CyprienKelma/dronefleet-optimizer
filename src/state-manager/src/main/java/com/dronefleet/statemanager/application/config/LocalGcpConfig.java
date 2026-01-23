package com.dronefleet.statemanager.application.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.NoCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration specific to the 'local' environment using emulators.
 * <p>
 * This class overrides the default Google Cloud credentials provider.
 * When running locally with emulators, we do not need (and do not want)
 * valid Service Account credentials. Attempting to provide fake JSON
 * often results in parsing errors.
 * <p>
 * But using NoCredentials.getInstance() bypasses the auth checks entirely,
 * allowing the client libraries to connect purely via the configured
 * emulator host/port.
 */
@Configuration
@Profile("local")
public class LocalGcpConfig {

    /**
     * Provides a "No Credentials" instance for local development.
     * This bean overrides the default GcpPubSubAutoConfiguration credentials.
     *
     * @return A CredentialsProvider that returns no credentials.
     */
    @Bean
    public CredentialsProvider googleCredentials() {
        return FixedCredentialsProvider.create(NoCredentials.getInstance());
    }
}

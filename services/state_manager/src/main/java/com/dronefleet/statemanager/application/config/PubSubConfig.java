package com.dronefleet.statemanager.application.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/** Spring configuration for Pub/Sub integration. */
@Configuration
public class PubSubConfig {

    @Value("${pubsub.subscriptions.telemetry}")
    private String telemetrySubscription;

    @Value("${pubsub.subscriptions.orders}")
    private String ordersSubscription;

    @Value("${pubsub.subscriptions.decisions}")
    private String decisionsSubscription;

    @Bean
    public MessageChannel telemetryInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel ordersInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel decisionsInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter telemetryAdapter(
            @Qualifier("telemetryInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {

        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, telemetrySubscription);
        adapter.setOutputChannel(inputChannel);
        adapter.setAutoStartup(false);

        return adapter;
    }

    @Bean
    public PubSubInboundChannelAdapter ordersAdapter(
            @Qualifier("ordersInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {

        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, ordersSubscription);
        adapter.setOutputChannel(inputChannel);
        adapter.setAutoStartup(false);

        return adapter;
    }

    @Bean
    public PubSubInboundChannelAdapter decisionsAdapter(
            @Qualifier("decisionsInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {

        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, decisionsSubscription);
        adapter.setOutputChannel(inputChannel);
        adapter.setAutoStartup(false);

        return adapter;
    }

    // Start the adapters when the application is ready to avoid MessageDispatchingException
    // error caused by pub/sub listeners not being ready yet
    @EventListener(ApplicationReadyEvent.class)
    public void startAdapters(ApplicationReadyEvent event) {
        event.getApplicationContext()
                .getBeansOfType(PubSubInboundChannelAdapter.class)
                .values()
                .forEach(PubSubInboundChannelAdapter::start);
    }
}

package org.springframework.cloud.bus.emqx;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;

public class EmqxInboundBridge implements MqttCallback {

    private final MqttClient client;
    private final BusEmqxProperties properties;
    private final ObjectMapper mapper;
    private final Environment environment;
    private final ApplicationEventPublisher publisher;
    private String busTopic;

    public EmqxInboundBridge(MqttClient client, BusEmqxProperties properties, ObjectMapper mapper, Environment environment, ApplicationEventPublisher publisher) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
        this.environment = environment;
        this.publisher = publisher;
    }

    @PostConstruct
    public void init() {
        String destination = environment.getProperty("spring.cloud.bus.destination", "springCloudBus");
        this.busTopic = properties.getTopicPrefix() + "/" + destination;
        try {
            client.setCallback(this);
            client.subscribe(busTopic + "/#", properties.getQos());
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to subscribe to EMQX bus topic: " + busTopic, e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        // Let client auto-reconnect; optionally log
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        try {
            BusEnvelope envelope = mapper.readValue(payload, BusEnvelope.class);
            if (properties.getClientId().equals(envelope.getSenderId())) {
                // Ignore messages we published ourselves
                return;
            }
            Class<?> clazz = Class.forName(envelope.getType());
            Object event = mapper.readValue(envelope.getPayload(), mapper.constructType(clazz));
            // mark inbound to avoid re-publish by outbound bridge
            EmqxInboundContext.markInbound();
            try {
                publisher.publishEvent(event);
            } finally {
                EmqxInboundContext.clearInbound();
            }
        } catch (Exception ex) {
            // Fallback: try to deserialize directly as RemoteApplicationEvent
            try {
                Class<?> base = Class.forName("org.springframework.cloud.bus.event.RemoteApplicationEvent");
                Object event = mapper.readValue(payload, mapper.constructType(base));
                EmqxInboundContext.markInbound();
                try {
                    publisher.publishEvent(event);
                } finally {
                    EmqxInboundContext.clearInbound();
                }
            } catch (Exception ignored) {
                // Could not parse, ignore
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No-op for inbound
    }
}

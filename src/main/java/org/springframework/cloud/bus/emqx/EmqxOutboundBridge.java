package org.springframework.cloud.bus.emqx;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class EmqxOutboundBridge  implements ApplicationListener<ApplicationEvent> {

    private final MqttClient client;
    private final BusEmqxProperties properties;
    private final ObjectMapper mapper;
    private final Environment environment;
    private final String busTopic;
    private final String busId;

    public EmqxOutboundBridge(MqttClient client, BusEmqxProperties properties, ObjectMapper mapper, Environment environment) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
        this.environment = environment;
        String destination = environment.getProperty("spring.cloud.bus.destination", "springCloudBus");
        this.busTopic = properties.getTopicPrefix() + "/" + destination;
        this.busId = environment.getProperty("spring.application.name", "application");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // Only handle RemoteApplicationEvent instances
        if (!isRemoteApplicationEvent(event)) {
            return;
        }
        // Skip events generated from inbound
        if (EmqxInboundContext.isInbound()) {
            return;
        }
        String originService = getProperty(event, "originService");
        if (originService == null || !originService.startsWith(busId)) {
            // We only publish events originating locally
            return;
        }
        try {
            String type = event.getClass().getName();
            String payload = mapper.writeValueAsString(event);
            BusEnvelope envelope = new BusEnvelope();
            envelope.setType(type);
            envelope.setPayload(payload);
            envelope.setOriginService(originService);
            envelope.setDestinationService(getProperty(event, "destinationService"));
            envelope.setSenderId(properties.getClientId());
            envelope.setTimestamp(System.currentTimeMillis());
            String msg = mapper.writeValueAsString(envelope);
            MqttMessage mqttMessage = new MqttMessage(msg.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(properties.getQos());
            log.info("Sending message to topic: {}Message: {}", busTopic, msg);
            client.publish(busTopic, mqttMessage);
        } catch (Exception e) {
            // ignore publish errors for robustness
        }
    }

    private boolean isRemoteApplicationEvent(Object event) {
        try {
            Class<?> base = Class.forName("org.springframework.cloud.bus.event.RemoteApplicationEvent");
            return base.isInstance(event);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private String getProperty(Object event, String name) {
        try {
            return (String) event.getClass().getMethod("get" + StringUtils.capitalize(name)).invoke(event);
        } catch (Exception e) {
            return null;
        }
    }
}

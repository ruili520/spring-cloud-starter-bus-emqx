package org.springframework.cloud.bus.emqx;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;

/**
 * EMQX-based BusBridge implementation that publishes RemoteApplicationEvent
 * to MQTT topics understood by the EMQX inbound bridge.
 */
@Slf4j
public class EmqxBusBridge implements BusBridge {

    private final MqttClient client;
    private final BusEmqxProperties properties;
    private final ObjectMapper mapper;
    private final Environment environment;
    private final String busTopic;
    private final String busId;

    public EmqxBusBridge(MqttClient client,
                         BusEmqxProperties properties,
                         ObjectMapper mapper,
                         Environment environment) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
        this.environment = environment;
        String destination = environment.getProperty("spring.cloud.bus.destination", "springCloudBus");
        this.busTopic = properties.getTopicPrefix() + "/" + destination;
        log.info("Bus busTopic : {}", this.busTopic);
        this.busId = environment.getProperty("spring.application.name", "application");
    }

    @Override
    public void send(RemoteApplicationEvent event) {
        if (event == null) {
            return;
        }
        try {
            BusEnvelope envelope = new BusEnvelope();
            envelope.setType(event.getClass().getName());
            envelope.setPayload(mapper.writeValueAsString(event));
            envelope.setOriginService(event.getOriginService());
            envelope.setDestinationService(event.getDestinationService());
            envelope.setSenderId(properties.getClientId());
            envelope.setTimestamp(System.currentTimeMillis());
            String msg = mapper.writeValueAsString(envelope);
            log.info("Sending message to topic: {}Message: {}", busTopic, msg);
            MqttMessage mqttMessage = new MqttMessage(msg.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(properties.getQos());
            client.publish(busTopic, mqttMessage);
        } catch (Exception e) {
            // ignore publish errors to avoid disrupting application flow
        }
    }
}
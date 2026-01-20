package org.springframework.cloud.bus.emqx;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(BusEmqxProperties.class)
@ConditionalOnProperty(prefix = "spring.cloud.bus.emqx", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmqxBusAutoConfiguration {

    @Bean
    public MqttClient emqxMqttClient(BusEmqxProperties properties, Environment environment) throws MqttException {
        String clientId = properties.getClientId();
        if (!StringUtils.hasText(clientId)) {
            String appName = environment.getProperty("spring.application.name", "application");
            clientId = appName + "-bus-" + UUID.randomUUID();
        }
        properties.setClientId(clientId);
        MqttClient client = new MqttClient(properties.getServer(), clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(properties.isCleanSession());
        options.setConnectionTimeout(properties.getConnectionTimeout());
        options.setKeepAliveInterval(properties.getKeepAliveInterval());
        if (StringUtils.hasText(properties.getUsername())) {
            options.setUserName(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            options.setPassword(properties.getPassword().toCharArray());
        }
        client.connect(options);
        log.info("Connected to EMQX MQTT broker at {} as {}", properties.getServer(), clientId);
        return client;
    }

    @Bean
    @DependsOn("emqxMqttClient")
    public EmqxInboundBridge emqxInboundBridge(MqttClient client, BusEmqxProperties properties, ObjectProvider<ObjectMapper> objectMapperProvider, Environment environment, ApplicationEventPublisher publisher) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new EmqxInboundBridge(client, properties, mapper, environment, publisher);
    }

    @Bean
    @DependsOn("emqxMqttClient")
    @ConditionalOnMissingBean(BusBridge.class)
    public EmqxOutboundBridge emqxOutboundBridge(MqttClient client, BusEmqxProperties properties, ObjectProvider<ObjectMapper> objectMapperProvider, Environment environment) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new EmqxOutboundBridge(client, properties, mapper, environment);
    }

    @Bean
    @DependsOn("emqxMqttClient")
    @ConditionalOnMissingBean(BusBridge.class)
    public BusBridge emqxBusBridge(MqttClient client, BusEmqxProperties properties, ObjectProvider<ObjectMapper> objectMapperProvider, Environment environment) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new EmqxBusBridge(client, properties, mapper, environment);
    }

}
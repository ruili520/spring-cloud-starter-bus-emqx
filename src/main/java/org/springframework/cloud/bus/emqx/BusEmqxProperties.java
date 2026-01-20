package org.springframework.cloud.bus.emqx;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "spring.cloud.bus.emqx")
public class BusEmqxProperties {

    /**
     * EMQX MQTT broker URL, e.g. tcp://localhost:1883 or ssl://host:8883
     */
    private String server = "tcp://localhost:1883";

    /**
     * MQTT clientId. Defaults to `${spring.application.name}-${random}` when unspecified.
     */
    private String clientId;

    /**
     * Username for EMQX authentication.
     */
    private String username;

    /**
     * Password for EMQX authentication.
     */
    private String password;

    /**
     * Topic prefix to use for Spring Cloud Bus messages.
     * Example: myapp/bus (final topic will be `${topicPrefix}/springCloudBus`).
     */
    private String topicPrefix = "spring/bus";

    /**
     * MQTT QoS level (0,1,2). Default 1.
     */
    private int qos = 1;

    /**
     * Clean session flag. Default true.
     */
    private boolean cleanSession = true;

    /**
     * Keep alive interval in seconds. Default 30.
     */
    private int keepAliveInterval = 30;

    /**
     * Connection timeout in seconds. Default 10.
     */
    private int connectionTimeout = 10;

    /**
     * Enable EMQX Bus bridge. Default true.
     */
    private boolean enabled = true;

}
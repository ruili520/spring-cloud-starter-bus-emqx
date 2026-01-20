package org.springframework.cloud.bus.emqx;

import lombok.Data;

@Data
public class BusEnvelope {

    private String type;

    private String payload;

    private String originService;

    private String destinationService;

    private String senderId;

    private long timestamp;

}

package org.springframework.cloud.bus.emqx;

public class EmqxInboundContext {

    private static final ThreadLocal<Boolean> INBOUND = new ThreadLocal<>();

    static void markInbound() {
        INBOUND.set(Boolean.TRUE);
    }

    static boolean isInbound() {
        return Boolean.TRUE.equals(INBOUND.get());
    }

    static void clearInbound() {
        INBOUND.remove();
    }

}

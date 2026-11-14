package com.smartcity.iot;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Spring Boot Actuator HealthIndicator for the Eclipse Mosquitto MQTT broker connection.
 * Exposes MQTT connectivity status on /actuator/health alongside PostgreSQL, MongoDB, and Redis.
 */
@Component
public class MqttHealthIndicator implements HealthIndicator {

    private final MqttClientManager mqttClientManager;
    private final MqttProperties mqttProperties;

    public MqttHealthIndicator(MqttClientManager mqttClientManager, MqttProperties mqttProperties) {
        this.mqttClientManager = mqttClientManager;
        this.mqttProperties = mqttProperties;
    }

    @Override
    public Health health() {
        if (!mqttProperties.isEnabled()) {
            return Health.up()
                    .withDetail("status", "DISABLED")
                    .withDetail("message", "MQTT disabled via configuration")
                    .build();
        }

        if (mqttClientManager != null && mqttClientManager.isConnected()) {
            return Health.up()
                    .withDetail("brokerUrl", mqttProperties.getBrokerUrl())
                    .withDetail("clientId", mqttProperties.getClientId())
                    .withDetail("status", "CONNECTED")
                    .build();
        }

        return Health.down()
                .withDetail("brokerUrl", mqttProperties.getBrokerUrl())
                .withDetail("status", "DISCONNECTED")
                .build();
    }
}

package com.smartcity.iot;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttClientManager implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttClientManager.class);

    private final MqttProperties properties;
    private final IoTProcessingService ioTProcessingService;
    private MqttClient mqttClient;
    private volatile boolean connected = false;

    public MqttClientManager(MqttProperties properties, IoTProcessingService ioTProcessingService) {
        this.properties = properties;
        this.ioTProcessingService = ioTProcessingService;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("MQTT integration is disabled via configuration");
            return;
        }

        // Connect asynchronously to avoid blocking Spring context startup
        Thread connectorThread = new Thread(this::connect, "mqtt-connector-thread");
        connectorThread.setDaemon(true);
        connectorThread.start();
    }

    public synchronized void connect() {
        if (connected) return;

        try {
            String clientId = properties.getClientId() + "-" + System.currentTimeMillis();
            mqttClient = new MqttClient(properties.getBrokerUrl(), clientId, new MemoryPersistence());
            mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(properties.isCleanSession());
            options.setAutomaticReconnect(properties.isAutomaticReconnect());
            options.setConnectionTimeout(properties.getConnectionTimeout());
            options.setKeepAliveInterval(properties.getKeepAliveInterval());

            if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
                options.setUserName(properties.getUsername());
            }
            if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
                options.setPassword(properties.getPassword().toCharArray());
            }

            log.info("Connecting to Mosquitto MQTT broker at '{}' with client ID '{}'", properties.getBrokerUrl(), clientId);
            mqttClient.connect(options);
            connected = true;
            log.info("Successfully connected to Mosquitto MQTT broker at '{}'", properties.getBrokerUrl());

        } catch (MqttException e) {
            log.warn("Failed to connect to Mosquitto MQTT broker at '{}': {}. Reconnect will be attempted.",
                    properties.getBrokerUrl(), e.getMessage());
            connected = false;
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        connected = true;
        log.info("MQTT connection established to {} (reconnect={})", serverURI, reconnect);

        try {
            // Subscribe to sensor data and status topics
            mqttClient.subscribe(properties.getDataTopic(), properties.getQos());
            log.info("Subscribed to MQTT data topic: {}", properties.getDataTopic());

            mqttClient.subscribe(properties.getStatusTopic(), properties.getQos());
            log.info("Subscribed to MQTT status topic: {}", properties.getStatusTopic());

        } catch (MqttException e) {
            log.error("Failed to subscribe to MQTT topics: {}", e.getMessage(), e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        connected = false;
        log.warn("MQTT connection lost: {}", cause != null ? cause.getMessage() : "Unknown reason");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("Received MQTT message on '{}': {}", topic, payload);

        try {
            if (topic.endsWith("/data")) {
                ioTProcessingService.processDataMessage(topic, payload);
            } else if (topic.endsWith("/status")) {
                ioTProcessingService.processStatusMessage(topic, payload);
            } else {
                log.info("Unhandled MQTT topic: '{}'", topic);
            }
        } catch (Exception e) {
            log.error("Error processing MQTT message on '{}': {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Delivery acknowledgment for published messages
    }

    public void publish(String topic, String payload) throws MqttException {
        publish(topic, payload, properties.getQos(), false);
    }

    public void publish(String topic, String payload, int qos, boolean retained) throws MqttException {
        if (mqttClient == null || !mqttClient.isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }

        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(qos);
        message.setRetained(retained);
        mqttClient.publish(topic, message);
        log.debug("Published MQTT message to '{}': {}", topic, payload);
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    @PreDestroy
    public void disconnect() {
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnectForcibly(1000);
                }
                mqttClient.close();
                log.info("MQTT client disconnected and closed successfully");
            } catch (Exception e) {
                log.warn("Error disconnecting MQTT client: {}", e.getMessage());
            }
        }
    }
}

package com.smartcity.iot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mqtt")
public class MqttProperties {

    private String brokerUrl = "tcp://localhost:1883";
    private String username;
    private String password;
    private String clientId = "civicpulse-backend";
    private String dataTopic = "smartcity/sensors/+/data";
    private String statusTopic = "smartcity/sensors/+/status";
    private int qos = 1;
    private int connectionTimeout = 10;
    private int keepAliveInterval = 30;
    private boolean automaticReconnect = true;
    private boolean cleanSession = true;
    private boolean enabled = true;

    public String getBrokerUrl() { return brokerUrl; }
    public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getDataTopic() { return dataTopic; }
    public void setDataTopic(String dataTopic) { this.dataTopic = dataTopic; }

    public String getStatusTopic() { return statusTopic; }
    public void setStatusTopic(String statusTopic) { this.statusTopic = statusTopic; }

    public int getQos() { return qos; }
    public void setQos(int qos) { this.qos = qos; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getKeepAliveInterval() { return keepAliveInterval; }
    public void setKeepAliveInterval(int keepAliveInterval) { this.keepAliveInterval = keepAliveInterval; }

    public boolean isAutomaticReconnect() { return automaticReconnect; }
    public void setAutomaticReconnect(boolean automaticReconnect) { this.automaticReconnect = automaticReconnect; }

    public boolean isCleanSession() { return cleanSession; }
    public void setCleanSession(boolean cleanSession) { this.cleanSession = cleanSession; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

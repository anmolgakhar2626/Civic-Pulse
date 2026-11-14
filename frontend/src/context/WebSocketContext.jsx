import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './AuthContext';

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
  const { token, user } = useAuth();
  const [connected, setConnected] = useState(false);
  const [latestIncidentEvent, setLatestIncidentEvent] = useState(null);
  const [latestSensorEvent, setLatestSensorEvent] = useState(null);
  const [latestSlaEvent, setLatestSlaEvent] = useState(null);
  const [latestAssignmentEvent, setLatestAssignmentEvent] = useState(null);
  const [latestAlertEvent, setLatestAlertEvent] = useState(null);
  const [alerts, setAlerts] = useState([]);

  const clientRef = useRef(null);
  const userSubRef = useRef(null);
  const prevTokenRef = useRef(token);
  const listenersRef = useRef({
    incidents: new Set(),
    sensors: new Set(),
    sla: new Set(),
    alerts: new Set(),
    assignments: new Set(),
    notifications: new Set(),
  });

  const dismissAlert = (id) => {
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  };

  const pushAlert = (alertData) => {
    const newAlert = {
      id: alertData.id || `${Date.now()}-${Math.random()}`,
      title: alertData.title || 'Municipal Alert',
      message: alertData.message || alertData.description || 'System notification received',
      severity: alertData.severity || 'MEDIUM',
      timestamp: alertData.timestamp || new Date().toISOString(),
    };

    setAlerts((prev) => [newAlert, ...prev.slice(0, 4)]);

    // Auto dismiss after 8 seconds
    setTimeout(() => {
      dismissAlert(newAlert.id);
    }, 8000);
  };

  const subscribeUserChannel = (client, userId) => {
    if (!client || !client.connected || !userId) return;

    if (userSubRef.current) {
      try {
        userSubRef.current.unsubscribe();
      } catch (e) {
        // ignore unsubscribe errors
      }
      userSubRef.current = null;
    }

    try {
      userSubRef.current = client.subscribe(`/topic/notifications/${userId}`, (message) => {
        try {
          const payload = JSON.parse(message.body);
          listenersRef.current.notifications.forEach((cb) => cb(payload));
          window.dispatchEvent(new CustomEvent('civicpulse_notification_received', { detail: payload }));

          pushAlert({
            id: payload.id,
            title: payload.title || 'Assigned Notification',
            message: payload.message || 'Direct notification received',
            severity: payload.severity || 'MEDIUM',
          });
        } catch (e) {
          console.error('Error parsing direct notification event', e);
        }
      });
    } catch (err) {
      console.warn('Could not subscribe to personal notification channel', err);
    }
  };

  useEffect(() => {
    const hasNativeWebSocket = typeof window !== 'undefined' && typeof window.WebSocket !== 'undefined';
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const brokerURL = `${protocol}//${window.location.host}/ws`;

    const client = new Client({
      // Prefer standard native WebSocket over /ws; fallback to SockJS only if native WebSocket is unavailable
      brokerURL: hasNativeWebSocket ? brokerURL : undefined,
      webSocketFactory: !hasNativeWebSocket
        ? () => {
            try {
              return new SockJS('/ws');
            } catch (err) {
              console.warn('SockJS fallback initialization error:', err);
              return null;
            }
          }
        : undefined,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      // Attach current JWT authentication token to the STOMP CONNECT frame
      beforeConnect: () => {
        const activeToken = localStorage.getItem('civicpulse_token') || token;
        if (activeToken) {
          client.connectHeaders = {
            Authorization: `Bearer ${activeToken}`,
          };
        } else {
          client.connectHeaders = {};
        }
      },

      debug: () => {},
    });

    client.onConnect = () => {
      setConnected(true);

      // 1. Incidents Topic
      client.subscribe('/topic/incidents', (message) => {
        try {
          const payload = JSON.parse(message.body);
          setLatestIncidentEvent(payload);
          listenersRef.current.incidents.forEach((cb) => cb(payload));

          if (payload.priorityTier === 'CRITICAL' || payload.eventType === 'CRITICAL_ALERT') {
            pushAlert({
              title: `Critical Incident: ${payload.incidentType || 'Urgent'}`,
              message: payload.title || payload.description || 'Emergency services dispatched',
              severity: 'CRITICAL',
            });
          }
        } catch (e) {
          console.error('Error parsing incident event', e);
        }
      });

      // 2. Sensors Topic
      client.subscribe('/topic/sensors', (message) => {
        try {
          const payload = JSON.parse(message.body);
          setLatestSensorEvent(payload);
          listenersRef.current.sensors.forEach((cb) => cb(payload));

          if (payload.thresholdCrossed) {
            pushAlert({
              title: `Sensor Alert: ${payload.sensorId}`,
              message: `${payload.sensorType}: ${payload.value} ${payload.unit || ''} exceeds safety threshold`,
              severity: payload.severity || 'HIGH',
            });
          }
        } catch (e) {
          console.error('Error parsing sensor event', e);
        }
      });

      // 3. SLA Topic
      client.subscribe('/topic/sla', (message) => {
        try {
          const payload = JSON.parse(message.body);
          setLatestSlaEvent(payload);
          listenersRef.current.sla.forEach((cb) => cb(payload));

          if (payload.slaStatus === 'BREACHED') {
            pushAlert({
              title: 'SLA Breached',
              message: `Incident ${payload.incidentNumber || payload.incidentId} has exceeded target resolution time`,
              severity: 'CRITICAL',
            });
          } else if (payload.slaStatus === 'AT_RISK') {
            pushAlert({
              title: 'SLA At Risk',
              message: `Incident ${payload.incidentNumber || payload.incidentId} target resolution deadline nearing`,
              severity: 'HIGH',
            });
          }
        } catch (e) {
          console.error('Error parsing SLA event', e);
        }
      });

      // 4. Alerts Topic
      client.subscribe('/topic/alerts', (message) => {
        try {
          const payload = JSON.parse(message.body);
          setLatestAlertEvent(payload);
          pushAlert(payload);
          listenersRef.current.alerts.forEach((cb) => cb(payload));
        } catch (e) {
          console.error('Error parsing alert event', e);
        }
      });

      // 5. Assignments Topic
      client.subscribe('/topic/assignments', (message) => {
        try {
          const payload = JSON.parse(message.body);
          setLatestAssignmentEvent(payload);
          listenersRef.current.assignments.forEach((cb) => cb(payload));
        } catch (e) {
          console.error('Error parsing assignment event', e);
        }
      });

      // 6. Notifications Topic (Broadcast)
      client.subscribe('/topic/notifications', (message) => {
        try {
          const payload = JSON.parse(message.body);
          listenersRef.current.notifications.forEach((cb) => cb(payload));
          window.dispatchEvent(new CustomEvent('civicpulse_notification_received', { detail: payload }));

          if (payload.severity === 'CRITICAL' || payload.severity === 'HIGH') {
            pushAlert({
              id: payload.id,
              title: payload.title || 'Municipal Notification',
              message: payload.message || 'New operational update received',
              severity: payload.severity,
            });
          }
        } catch (e) {
          console.error('Error parsing notification event', e);
        }
      });

      // 7. User-Specific Notification Topic
      const currentUserId = user?.id || (() => {
        try {
          const saved = localStorage.getItem('civicpulse_user');
          return saved ? JSON.parse(saved)?.id : null;
        } catch {
          return null;
        }
      })();

      if (currentUserId) {
        subscribeUserChannel(client, currentUserId);
      }
    };

    client.onDisconnect = () => {
      setConnected(false);
    };

    client.onStompError = (frame) => {
      console.warn('STOMP Broker error: ' + (frame.headers ? frame.headers['message'] : 'unknown'));
      setConnected(false);
    };

    client.onWebSocketError = (err) => {
      console.warn('WebSocket transport warning (non-fatal):', err);
      setConnected(false);
    };

    client.onWebSocketClose = () => {
      setConnected(false);
    };

    clientRef.current = client;
    client.activate();

    // Clean disconnect before browser window unload or page reload
    const handleBeforeUnload = () => {
      if (client.active) {
        client.deactivate();
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      if (userSubRef.current) {
        try {
          userSubRef.current.unsubscribe();
        } catch (e) {
          // ignore
        }
        userSubRef.current = null;
      }
      if (clientRef.current === client) {
        clientRef.current = null;
      }
      client.deactivate().catch(() => {});
    };
  }, []);

  // Update user-specific notification subscription whenever user identity changes
  useEffect(() => {
    if (clientRef.current && clientRef.current.connected) {
      if (user?.id) {
        subscribeUserChannel(clientRef.current, user.id);
      } else if (userSubRef.current) {
        try {
          userSubRef.current.unsubscribe();
        } catch (e) {
          // ignore
        }
        userSubRef.current = null;
      }
    }
  }, [user?.id, connected]);

  // Re-authenticate STOMP connection when authentication token changes (e.g., login or logout)
  useEffect(() => {
    if (prevTokenRef.current !== token) {
      prevTokenRef.current = token;
      if (clientRef.current && clientRef.current.active) {
        clientRef.current.deactivate().then(() => {
          if (clientRef.current) {
            clientRef.current.activate();
          }
        }).catch((err) => {
          console.warn('STOMP re-auth error:', err);
        });
      }
    }
  }, [token]);

  const subscribeTopic = (topic, callback) => {
    if (listenersRef.current[topic]) {
      listenersRef.current[topic].add(callback);
      return () => {
        listenersRef.current[topic].delete(callback);
      };
    }
    return () => {};
  };

  return (
    <WebSocketContext.Provider
      value={{
        connected,
        latestIncidentEvent,
        latestSensorEvent,
        latestSlaEvent,
        latestAssignmentEvent,
        latestAlertEvent,
        alerts,
        dismissAlert,
        subscribeTopic,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};

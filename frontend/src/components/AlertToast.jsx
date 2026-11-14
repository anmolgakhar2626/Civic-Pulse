import React from 'react';
import { FiAlertOctagon, FiAlertTriangle, FiInfo, FiX } from 'react-icons/fi';
import { useWebSocket } from '../context/WebSocketContext';

export const AlertToast = () => {
  const { alerts, dismissAlert } = useWebSocket();

  if (!alerts || alerts.length === 0) return null;

  return (
    <div className="cc-toast-container">
      {alerts.map((alert) => {
        let toastClass = 'cc-toast';
        let icon = <FiInfo size={16} color="var(--accent-primary)" />;

        if (alert.severity === 'CRITICAL') {
          toastClass += ' critical';
          icon = <FiAlertOctagon size={16} color="var(--color-critical)" />;
        } else if (alert.severity === 'HIGH') {
          toastClass += ' high';
          icon = <FiAlertTriangle size={16} color="var(--color-high)" />;
        } else if (alert.severity === 'MEDIUM') {
          toastClass += ' medium';
          icon = <FiAlertTriangle size={16} color="var(--color-medium)" />;
        }

        return (
          <div key={alert.id} className={toastClass}>
            <div style={{ flexShrink: 0, marginTop: '2px' }}>{icon}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 600, color: 'var(--text-main)', fontSize: '0.8rem' }}>{alert.title}</div>
              <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem', marginTop: '2px' }}>{alert.message}</div>
            </div>
            <button
              onClick={() => dismissAlert(alert.id)}
              style={{ background: 'none', border: 'none', color: 'var(--text-subtle)', cursor: 'pointer', padding: '2px' }}
              title="Dismiss"
            >
              <FiX size={14} />
            </button>
          </div>
        );
      })}
    </div>
  );
};

export default AlertToast;

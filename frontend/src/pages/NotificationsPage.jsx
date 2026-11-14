import React from 'react';
import NotificationCenter from '../components/NotificationCenter';
import { FiBell, FiShield, FiAlertTriangle, FiCheckCircle } from 'react-icons/fi';

export default function NotificationsPage() {
  return (
    <div className="notifications-page-container" style={{ padding: '1.5rem', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-main, #0f172a)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <FiBell style={{ color: 'var(--accent-primary, #2563eb)' }} />
          Municipal Notification Center
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted, #64748b)', marginTop: '0.25rem' }}>
          Real-time alerts, operational assignments, SLA warnings, and municipal dispatch updates.
        </p>
      </div>

      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid var(--border-light, #e2e8f0)', boxShadow: 'var(--shadow-sm)' }}>
        <NotificationCenter isStandalone={true} isOpen={true} />
      </div>
    </div>
  );
}

import React from 'react';
import { FiInbox } from 'react-icons/fi';

export const EmptyState = ({
  title = 'No records found',
  description = 'No operational data matches the current filters or criteria.',
  action,
}) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem 1.5rem', textAlign: 'center' }}>
      <div style={{ width: '44px', height: '44px', borderRadius: '50%', backgroundColor: 'var(--bg-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-subtle)', marginBottom: '0.75rem' }}>
        <FiInbox size={22} />
      </div>
      <h3 style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.25rem' }}>{title}</h3>
      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', maxWidth: '340px', marginBottom: action ? '1rem' : 0 }}>{description}</p>
      {action}
    </div>
  );
};

export default EmptyState;

import React from 'react';
import { FiClock, FiAlertCircle, FiCheckCircle } from 'react-icons/fi';

export const SlaTimer = ({ remainingMinutes, status, targetMinutes, deadlineAt }) => {
  const normStatus = (status || 'ON_TRACK').toUpperCase();

  let badgeClass = 'badge badge-status';
  let icon = <FiClock size={12} />;

  if (normStatus === 'BREACHED') {
    badgeClass = 'badge badge-critical';
    icon = <FiAlertCircle size={12} />;
  } else if (normStatus === 'WARNING' || normStatus === 'AT_RISK') {
    badgeClass = 'badge badge-high';
    icon = <FiAlertCircle size={12} />;
  } else if (normStatus === 'COMPLETED' || normStatus === 'RESOLVED') {
    badgeClass = 'badge badge-low';
    icon = <FiCheckCircle size={12} />;
  }

  const formatRemaining = (mins) => {
    if (mins == null) return '--';
    if (mins < 0) {
      const overdue = Math.abs(mins);
      const hrs = Math.floor(overdue / 60);
      const m = overdue % 60;
      return hrs > 0 ? `+${hrs}h ${m}m Overdue` : `+${m}m Overdue`;
    }
    const hrs = Math.floor(mins / 60);
    const m = mins % 60;
    return hrs > 0 ? `${hrs}h ${m}m left` : `${m}m left`;
  };

  return (
    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
      <span className={badgeClass}>
        {icon}
        {normStatus.replace('_', ' ')}
      </span>
      {remainingMinutes != null && normStatus !== 'RESOLVED' && normStatus !== 'COMPLETED' && (
        <span style={{ fontSize: '0.75rem', fontWeight: 600, color: normStatus === 'BREACHED' ? '#b91c1c' : '#475569' }}>
          {formatRemaining(remainingMinutes)}
        </span>
      )}
    </div>
  );
};

export default SlaTimer;

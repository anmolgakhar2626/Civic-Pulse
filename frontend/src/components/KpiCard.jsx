import React from 'react';

export const KpiCard = ({ label, value, subtext, icon, variant = 'default', onClick, style }) => {
  let iconBg = '#f1f5f9';
  let iconColor = '#475569';

  if (variant === 'critical') {
    iconBg = 'var(--color-critical-bg)';
    iconColor = 'var(--color-critical)';
  } else if (variant === 'high') {
    iconBg = 'var(--color-high-bg)';
    iconColor = 'var(--color-high)';
  } else if (variant === 'warning') {
    iconBg = 'var(--color-medium-bg)';
    iconColor = 'var(--color-medium)';
  } else if (variant === 'success') {
    iconBg = 'var(--color-low-bg)';
    iconColor = 'var(--color-low)';
  } else if (variant === 'accent') {
    iconBg = '#f0f9ff';
    iconColor = 'var(--accent-primary)';
  }

  return (
    <div
      className="cc-kpi-card"
      onClick={onClick}
      style={{
        cursor: onClick ? 'pointer' : 'default',
        transition: 'all 0.15s ease',
        ...style
      }}
    >
      <div className="cc-kpi-top">
        <span className="cc-kpi-label">{label}</span>
        {icon && (
          <div className="cc-kpi-icon" style={{ backgroundColor: iconBg, color: iconColor }}>
            {icon}
          </div>
        )}
      </div>
      <div className="cc-kpi-value">{value != null ? value : '--'}</div>
      {subtext && <div className="cc-kpi-subtext">{subtext}</div>}
    </div>
  );
};

export default KpiCard;

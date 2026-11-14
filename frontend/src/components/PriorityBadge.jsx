import React from 'react';
import { FiAlertOctagon, FiAlertTriangle, FiClock, FiCheck } from 'react-icons/fi';

export const PriorityBadge = ({ tier }) => {
  const normalized = (tier || 'MEDIUM').toUpperCase();

  switch (normalized) {
    case 'CRITICAL':
    case 'TIER 1':
      return (
        <span className="badge badge-critical" title="Target SLA: <= 30 mins">
          <FiAlertOctagon size={12} />
          CRITICAL
        </span>
      );
    case 'HIGH':
    case 'TIER 2':
      return (
        <span className="badge badge-high" title="Target SLA: 2-4 hours">
          <FiAlertTriangle size={12} />
          HIGH
        </span>
      );
    case 'MEDIUM':
    case 'TIER 3':
      return (
        <span className="badge badge-medium" title="Target SLA: 12-24 hours">
          <FiClock size={12} />
          MEDIUM
        </span>
      );
    case 'LOW':
    case 'TIER 4':
    default:
      return (
        <span className="badge badge-low" title="Target SLA: 24-72 hours">
          <FiCheck size={12} />
          LOW
        </span>
      );
  }
};

export default PriorityBadge;

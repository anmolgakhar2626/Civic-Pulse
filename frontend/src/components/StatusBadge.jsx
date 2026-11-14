import React from 'react';

export const StatusBadge = ({ status }) => {
  const norm = (status || 'OPEN').toUpperCase();

  let className = 'badge badge-status';
  if (['OPEN', 'DETECTED'].includes(norm)) {
    className += ' active';
  } else if (['ASSIGNED', 'ACKNOWLEDGED'].includes(norm)) {
    className += ' assigned';
  } else if (['IN_PROGRESS', 'CLASSIFIED', 'PRIORITIZED'].includes(norm)) {
    className += ' in_progress';
  } else if (['RESOLVED', 'VERIFIED'].includes(norm)) {
    className += ' resolved';
  } else if (['CLOSED'].includes(norm)) {
    className += ' closed';
  } else if (['REJECTED', 'CANCELLED', 'DUPLICATE'].includes(norm)) {
    className += ' rejected';
  } else if (['ESCALATED'].includes(norm)) {
    className = 'badge badge-critical';
  }

  const label = norm.replace(/_/g, ' ');

  return <span className={className}>{label}</span>;
};

export default StatusBadge;

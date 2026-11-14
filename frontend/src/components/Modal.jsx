import React, { useEffect } from 'react';
import { FiX } from 'react-icons/fi';

export const Modal = ({ title, isOpen, onClose, children, footer, maxWidth = '560px' }) => {
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="cc-modal-backdrop" onClick={onClose}>
      <div
        className="cc-modal"
        style={{ maxWidth }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="cc-modal-header">
          <h2 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-main)' }}>{title}</h2>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', color: 'var(--text-subtle)', cursor: 'pointer', padding: '4px' }}
            title="Close"
          >
            <FiX size={18} />
          </button>
        </div>
        <div className="cc-modal-body">{children}</div>
        {footer && <div className="cc-modal-footer">{footer}</div>}
      </div>
    </div>
  );
};

export default Modal;

import React from 'react';
import {
  FiX,
  FiDownload,
  FiPrinter,
  FiCheckCircle,
  FiShield,
  FiClock,
  FiExternalLink,
  FiMapPin,
  FiPhone,
  FiUser
} from 'react-icons/fi';
import { complaintsApi } from '../api/complaintsApi';

export default function ComplaintReceiptModal({ complaint, receipt, onClose, onTrack }) {
  if (!complaint && !receipt) return null;

  const data = receipt || complaint?.receipt || {};
  const recordNumber = data.complaintRecordNumber || complaint?.complaintNumber || 'CP-2026-000001';
  const receiptNumber = data.receiptNumber || recordNumber.replace('CP-', 'RCP-');
  const citizenName = data.citizenName || complaint?.citizenName || 'Citizen';
  const citizenPhone = data.citizenPhone || complaint?.citizenPhone || '+91 98765 43210';
  const title = data.title || complaint?.title || 'Civic Grievance';
  const category = data.category || complaint?.category || 'GENERAL';
  const departmentName = data.departmentName || complaint?.departmentName || 'Public Works Department';
  const departmentCode = complaint?.departmentCode || 'PWD';
  const priority = data.priority || complaint?.priority || 'MEDIUM';
  const status = data.status || complaint?.status || 'SUBMITTED';
  const formattedDate = data.generatedAt
    ? new Date(data.generatedAt).toLocaleString()
    : new Date().toLocaleString();
  const slaDeadlineFormatted = data.slaDeadline
    ? new Date(data.slaDeadline).toLocaleString()
    : 'Within Guaranteed Window';

  const handleDownload = async () => {
    try {
      const complaintId = complaint?.id || data.complaintId;
      if (!complaintId) return;

      const response = await complaintsApi.downloadReceiptPdf(complaintId);
      const blobPayload = response?.data || response;
      const blob = new Blob([blobPayload], { type: 'text/plain;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `CivicPulse-Receipt-${recordNumber}.txt`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download failed, using client fallback:', err);
      // Fallback text download
      const fallbackText = `CIVICPULSE SMART CITY COMMAND CENTRE
OFFICIAL CITIZEN GRIEVANCE ACKNOWLEDGEMENT RECEIPT
================================================================================
Receipt Number     : ${receiptNumber}
Complaint Number   : ${recordNumber}
Filing Date & Time : ${formattedDate}
SLA Resolution Due : ${slaDeadlineFormatted} (${priority} Priority)

--- CITIZEN PARTICULARS ---
Citizen Name       : ${citizenName}
Registered Mobile  : ${citizenPhone}
Filing Status      : OFFICIALLY REGISTERED

--- GRIEVANCE PARTICULARS ---
Title              : ${title}
Category           : ${category}
Assigned Department: ${departmentName} (${departmentCode})
Status             : ${status}

Notice:
This document serves as legal proof of submission into the CivicPulse Command Centre.
Track progress at: http://localhost:5173/dashboard/citizen
================================================================================`;
      const blob = new Blob([fallbackText], { type: 'text/plain;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `CivicPulse-Receipt-${recordNumber}.txt`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.7)',
        backdropFilter: 'blur(5px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1100,
        padding: '1rem',
      }}
      onClick={onClose}
    >
      <div
        className="cc-modal"
        style={{
          width: '100%',
          maxWidth: '650px',
          maxHeight: '92vh',
          backgroundColor: '#ffffff',
          borderRadius: '12px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Receipt Header */}
        <div
          style={{
            backgroundColor: '#0f172a',
            color: '#ffffff',
            padding: '1.25rem 1.5rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                backgroundColor: 'rgba(2, 132, 199, 0.2)',
                color: '#38bdf8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '1.25rem',
              }}
            >
              <FiShield />
            </div>
            <div>
              <h2 style={{ fontSize: '1rem', fontWeight: 700, margin: 0, color: '#f8fafc' }}>
                CivicPulse Smart City Command Centre
              </h2>
              <p style={{ margin: 0, fontSize: '0.725rem', color: '#94a3b8' }}>
                Official Electronic Grievance Intake Acknowledgement
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '4px',
            }}
          >
            <FiX size={20} />
          </button>
        </div>

        {/* Printable Receipt Paper Container */}
        <div
          id="civicpulse-printable-receipt"
          style={{
            padding: '1.5rem',
            overflowY: 'auto',
            flex: 1,
            backgroundColor: '#ffffff',
            display: 'flex',
            flexDirection: 'column',
            gap: '1.25rem',
          }}
        >
          {/* Success Banner */}
          <div
            style={{
              backgroundColor: '#f0fdf4',
              border: '1px solid #bbf7d0',
              borderRadius: '8px',
              padding: '0.85rem 1rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
            }}
          >
            <FiCheckCircle size={24} color="#16a34a" style={{ flexShrink: 0 }} />
            <div>
              <div style={{ fontWeight: 700, color: '#166534', fontSize: '0.9rem' }}>
                Grievance Formally Registered into Command Centre
              </div>
              <div style={{ fontSize: '0.75rem', color: '#15803d' }}>
                Automated assignment rules triggered. Field crews & nodal officers alerted.
              </div>
            </div>
          </div>

          {/* Numbers Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '1rem',
              backgroundColor: '#f8fafc',
              border: '1px solid var(--border-light)',
              borderRadius: '8px',
              padding: '1rem',
            }}
          >
            <div>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Complaint Record Sequence #
              </span>
              <div style={{ fontFamily: 'monospace', fontWeight: 800, fontSize: '1.15rem', color: 'var(--accent-primary)', marginTop: '2px' }}>
                {recordNumber}
              </div>
            </div>

            <div>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Digital Acknowledgement Receipt #
              </span>
              <div style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '1.05rem', color: '#0f172a', marginTop: '2px' }}>
                {receiptNumber}
              </div>
            </div>
          </div>

          {/* Grievance Details Matrix */}
          <div
            style={{
              border: '1px solid var(--border-light)',
              borderRadius: '8px',
              overflow: 'hidden',
              fontSize: '0.825rem',
            }}
          >
            <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', padding: '0.65rem 1rem', borderBottom: '1px solid var(--border-light)', backgroundColor: '#ffffff' }}>
              <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Grievance Title:</span>
              <span style={{ color: 'var(--text-main)', fontWeight: 600 }}>{title}</span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', padding: '0.65rem 1rem', borderBottom: '1px solid var(--border-light)', backgroundColor: '#f8fafc' }}>
              <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Assigned Dept:</span>
              <span style={{ color: 'var(--accent-teal)', fontWeight: 700 }}>{departmentName} ({departmentCode})</span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', padding: '0.65rem 1rem', borderBottom: '1px solid var(--border-light)', backgroundColor: '#ffffff' }}>
              <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Category:</span>
              <span style={{ color: 'var(--text-main)' }}>{category}</span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', padding: '0.65rem 1rem', borderBottom: '1px solid var(--border-light)', backgroundColor: '#f8fafc' }}>
              <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Citizen Details:</span>
              <span style={{ color: 'var(--text-main)' }}>{citizenName} • {citizenPhone}</span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', padding: '0.65rem 1rem', borderBottom: '1px solid var(--border-light)', backgroundColor: '#ffffff' }}>
              <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Filing Timestamp:</span>
              <span style={{ color: 'var(--text-main)' }}>{formattedDate}</span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', padding: '0.65rem 1rem', backgroundColor: '#f0fdf4' }}>
              <span style={{ color: '#166534', fontWeight: 700 }}>Guaranteed SLA:</span>
              <span style={{ color: '#15803d', fontWeight: 700 }}>Target Due: {slaDeadlineFormatted} ({priority} Priority)</span>
            </div>
          </div>

          {/* Legal / Verification Notice */}
          <div
            style={{
              padding: '0.75rem 1rem',
              backgroundColor: '#f8fafc',
              border: '1px dashed var(--border-medium)',
              borderRadius: '6px',
              fontSize: '0.725rem',
              color: 'var(--text-muted)',
              lineHeight: 1.4,
            }}
          >
            <strong>Official Municipal Record:</strong> This digital acknowledgement is issued under the Smart City Municipal Administration Act. Retain sequence number <strong>{recordNumber}</strong> for status inquiries, field verification, and grievance escalations.
          </div>
        </div>

        {/* Footer Actions */}
        <div
          style={{
            padding: '1rem 1.5rem',
            borderTop: '1px solid var(--border-light)',
            backgroundColor: '#f8fafc',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '0.75rem',
          }}
        >
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              onClick={handleDownload}
              className="btn btn-outline btn-sm"
              style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.8rem' }}
            >
              <FiDownload size={14} />
              Download Receipt
            </button>
            <button
              onClick={handlePrint}
              className="btn btn-outline btn-sm"
              style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.8rem' }}
            >
              <FiPrinter size={14} />
              Print
            </button>
          </div>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {onTrack && (
              <button
                onClick={() => {
                  onClose();
                  onTrack(complaint);
                }}
                className="btn btn-primary btn-sm"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '5px',
                  backgroundColor: 'var(--accent-primary)',
                  borderColor: 'var(--accent-primary)',
                  color: '#ffffff',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                }}
              >
                <FiExternalLink size={14} />
                Track Real-Time Status
              </button>
            )}
            <button
              onClick={onClose}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '0.8rem' }}
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}


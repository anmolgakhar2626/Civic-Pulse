import React, { useState, useEffect } from 'react';
import { complaintsApi } from '../api/complaintsApi';
import { useAuth } from '../context/AuthContext';
import {
  FiX,
  FiClock,
  FiShield,
  FiUser,
  FiMapPin,
  FiPhone,
  FiCheckCircle,
  FiAlertCircle,
  FiDownload,
  FiSend,
  FiImage,
  FiActivity,
  FiArrowRight,
  FiFileText,
  FiUpload,
  FiRefreshCw
} from 'react-icons/fi';

export default function ComplaintDetailsModal({ complaintId, initialComplaint, onClose, onUpdated }) {
  const { user } = useAuth();

  const [complaint, setComplaint] = useState(initialComplaint || null);
  const [loading, setLoading] = useState(!initialComplaint);
  const [activeTab, setActiveTab] = useState('overview'); // overview, audit, cpgrams, receipt
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Action dialog states
  const [showAssignDialog, setShowAssignDialog] = useState(false);
  const [assignees, setAssignees] = useState([]);
  const [selectedAssignee, setSelectedAssignee] = useState('');
  const [assignNotes, setAssignNotes] = useState('');

  const [showProgressDialog, setShowProgressDialog] = useState(false);
  const [progressStatus, setProgressStatus] = useState('IN_PROGRESS');
  const [progressNotes, setProgressNotes] = useState('');
  const [progressFiles, setProgressFiles] = useState([]);

  const [showResolutionDialog, setShowResolutionDialog] = useState(false);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [resolutionFiles, setResolutionFiles] = useState([]);

  const [showVerifyDialog, setShowVerifyDialog] = useState(false);
  const [verifyStatus, setVerifyStatus] = useState('GENUINE');
  const [verifyNotes, setVerifyNotes] = useState('');

  const [showPriorityDialog, setShowPriorityDialog] = useState(false);
  const [newPriority, setNewPriority] = useState('HIGH');
  const [priorityReason, setPriorityReason] = useState('');

  const [actionLoading, setActionLoading] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);

  const fetchComplaintDetails = async () => {
    const id = complaintId || initialComplaint?.id;
    if (!id) return;
    try {
      const res = await complaintsApi.getComplaintById(id);
      setComplaint(res.data || res);
    } catch (err) {
      console.error('Failed to load complaint details:', err);
      setError('Could not load complete complaint details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComplaintDetails();
  }, [complaintId]);

  const loadAssignees = async () => {
    try {
      const deptId = complaint?.departmentId;
      const res = await complaintsApi.getEligibleAssignees(deptId);
      const list = res.data || res || [];
      setAssignees(list);
      if (list.length > 0 && !selectedAssignee) {
        setSelectedAssignee(list[0].id);
      }
    } catch (err) {
      console.warn('Could not load eligible field workers:', err);
    }
  };

  useEffect(() => {
    if (showAssignDialog) {
      loadAssignees();
    }
  }, [showAssignDialog]);

  if (!complaint && loading) {
    return (
      <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.6)', zIndex: 1100, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ backgroundColor: '#ffffff', padding: '2rem', borderRadius: '8px', textAlign: 'center' }}>
          <div className="spinner-border text-primary" role="status" />
          <p style={{ marginTop: '0.75rem', fontSize: '0.875rem', color: 'var(--text-muted)' }}>Loading grievance record...</p>
        </div>
      </div>
    );
  }

  if (!complaint) return null;

  const roles = user?.roles ? user.roles.map(r => typeof r === 'string' ? r : r.name) : [];
  const isAdmin = roles.some(r => r.includes('ADMIN') || r.includes('COMMAND_OPERATOR'));
  const isOfficer = roles.some(r => r.includes('DEPARTMENT_OFFICER'));
  const isFieldWorker = roles.some(r => r.includes('FIELD_WORKER'));
  const isCitizen = roles.some(r => r.includes('CITIZEN')) && !isAdmin && !isOfficer && !isFieldWorker;

  // SLA formatting
  const remainingMins = complaint.remainingMinutes != null ? complaint.remainingMinutes : 0;
  const isResolvedOrClosed = ['RESOLVED', 'CLOSED'].includes(complaint.status);
  let slaBadgeText = 'ON TRACK';
  let slaBadgeBg = '#f0fdf4';
  let slaBadgeColor = '#15803d';

  if (isResolvedOrClosed) {
    slaBadgeText = 'SLA RESOLVED';
    slaBadgeBg = '#ecfdf5';
    slaBadgeColor = 'var(--accent-teal)';
  } else if (remainingMins < 0) {
    const overdueHrs = Math.abs(Math.floor(remainingMins / 60));
    const overdueMins = Math.abs(remainingMins % 60);
    slaBadgeText = `BREACHED (${overdueHrs}h ${overdueMins}m Overdue)`;
    slaBadgeBg = '#fef2f2';
    slaBadgeColor = '#b91c1c';
  } else {
    const hrs = Math.floor(remainingMins / 60);
    const mins = remainingMins % 60;
    if (remainingMins <= 120) {
      slaBadgeText = `AT RISK (${hrs}h ${mins}m left)`;
      slaBadgeBg = '#fff7ed';
      slaBadgeColor = '#c2410c';
    } else {
      slaBadgeText = `ON TRACK (${hrs}h ${mins}m left)`;
    }
  }

  // Verification badge colors
  const verStatus = complaint.complaintVerificationStatus || 'UNVERIFIED';
  let verBadgeBg = '#f1f5f9';
  let verBadgeColor = '#475569';
  if (verStatus === 'GENUINE') {
    verBadgeBg = '#f0fdf4';
    verBadgeColor = '#16a34a';
  } else if (verStatus.includes('REJECT') || verStatus === 'SPAM') {
    verBadgeBg = '#fef2f2';
    verBadgeColor = '#dc2626';
  } else if (verStatus === 'DUPLICATE') {
    verBadgeBg = '#fffbeb';
    verBadgeColor = '#d97706';
  }

  // Status badge colors
  const statusColors = {
    SUBMITTED: { bg: '#f1f5f9', color: '#475569' },
    ASSIGNED: { bg: '#eff6ff', color: '#2563eb' },
    IN_PROGRESS: { bg: '#fffbeb', color: '#b45309' },
    RESOLVED: { bg: '#f0fdf4', color: '#16a34a' },
    CLOSED: { bg: '#f8fafc', color: '#64748b' }
  };
  const sc = statusColors[complaint.status] || { bg: '#f1f5f9', color: '#475569' };

  // Handler: Assign Complaint
  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    if (!selectedAssignee) return;
    setActionLoading(true);
    setError(null);
    try {
      await complaintsApi.assignComplaint(complaint.id, {
        assignedToUserId: selectedAssignee,
        notes: assignNotes.trim()
      });
      setShowAssignDialog(false);
      setActionSuccess('Field worker assigned successfully.');
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Assignment failed.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handler: Update Progress
  const handleProgressSubmit = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('status', progressStatus);
      formData.append('progressNotes', progressNotes.trim());
      progressFiles.forEach(f => formData.append('files', f));

      await complaintsApi.updateProgressMultipart(complaint.id, formData);
      setShowProgressDialog(false);
      setProgressNotes('');
      setProgressFiles([]);
      setActionSuccess('Work progress recorded.');
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update progress.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handler: Submit Resolution
  const handleResolutionSubmit = async (e) => {
    e.preventDefault();
    if (!resolutionNotes.trim()) {
      setError('Please provide resolution notes.');
      return;
    }
    setActionLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('resolutionNotes', resolutionNotes.trim());
      resolutionFiles.forEach(f => formData.append('files', f));

      await complaintsApi.submitResolutionMultipart(complaint.id, formData);
      setShowResolutionDialog(false);
      setResolutionNotes('');
      setResolutionFiles([]);
      setActionSuccess('Resolution proof submitted for verification.');
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to submit resolution.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handler: Verify Complaint Status (Officer/Admin)
  const handleVerifySubmit = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError(null);
    try {
      await complaintsApi.verifyComplaint(complaint.id, {
        verificationStatus: verifyStatus,
        notes: verifyNotes.trim()
      });
      setShowVerifyDialog(false);
      setActionSuccess(`Complaint verified as ${verifyStatus}. Citizen reputation recalculated.`);
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Verification failed.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handler: Change Priority
  const handlePrioritySubmit = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError(null);
    try {
      await complaintsApi.changePriority(complaint.id, {
        priority: newPriority,
        reason: priorityReason.trim() || 'Command adjustment'
      });
      setShowPriorityDialog(false);
      setActionSuccess(`Priority changed to ${newPriority}. SLA deadline updated.`);
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to change priority.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handler: Verify Resolution (Officer decision)
  const handleResolutionDecision = async (decision) => {
    setActionLoading(true);
    setError(null);
    try {
      await complaintsApi.verifyResolution(complaint.id, {
        decision,
        notes: decision === 'ACCEPTED' ? 'Officer verified and closed.' : 'Resolution rejected, requires rework.'
      });
      setActionSuccess(`Resolution decision: ${decision}.`);
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record resolution decision.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handler: Close or Reopen (Admin / Citizen)
  const handleCloseOrReopen = async (action) => {
    setActionLoading(true);
    setError(null);
    try {
      await complaintsApi.closeOrReopen(complaint.id, {
        action,
        reason: action === 'CLOSE' ? 'Resolved to citizen satisfaction.' : 'Reopened for investigation.'
      });
      setActionSuccess(`Complaint ${action.toLowerCase()}ed.`);
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || `Failed to ${action.toLowerCase()} complaint.`);
    } finally {
      setActionLoading(false);
    }
  };

  // Handler: Download Receipt
  const handleDownloadReceipt = async () => {
    try {
      const response = await complaintsApi.downloadReceiptPdf(complaint.id);
      const blob = new Blob([response.data], { type: 'text/plain;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `CivicPulse-Receipt-${complaint.complaintNumber}.txt`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Receipt download error:', err);
    }
  };

  // Handler: Forward to Government Portal
  const handleForwardToCPGRAMS = async () => {
    setActionLoading(true);
    setError(null);
    try {
      await complaintsApi.forwardToGovernmentPortal(complaint.id, {
        targetPortal: 'CPGRAMS_SANDBOX',
        notes: 'Forwarded for central redressal integration'
      });
      setActionSuccess('Successfully forwarded to Central Government Grievance Portal (CPGRAMS Sandbox).');
      await fetchComplaintDetails();
      if (onUpdated) onUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to forward to Government Portal.');
    } finally {
      setActionLoading(false);
    }
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
          maxWidth: '850px',
          maxHeight: '94vh',
          backgroundColor: '#ffffff',
          borderRadius: '12px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid var(--border-light)',
            backgroundColor: '#f8fafc',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', flexWrap: 'wrap' }}>
              <span
                style={{
                  fontFamily: 'monospace',
                  fontSize: '1rem',
                  fontWeight: 800,
                  backgroundColor: '#ecfdf5',
                  color: 'var(--accent-teal)',
                  padding: '2px 8px',
                  borderRadius: '6px',
                  border: '1px solid #a7f3d0'
                }}
              >
                {complaint.complaintNumber}
              </span>
              <span
                style={{
                  padding: '2px 8px',
                  borderRadius: '9999px',
                  fontSize: '0.725rem',
                  fontWeight: 700,
                  backgroundColor: sc.bg,
                  color: sc.color,
                  border: `1px solid ${sc.color}33`
                }}
              >
                {complaint.status}
              </span>
              <span
                style={{
                  padding: '2px 8px',
                  borderRadius: '9999px',
                  fontSize: '0.725rem',
                  fontWeight: 700,
                  backgroundColor: verBadgeBg,
                  color: verBadgeColor,
                  border: `1px solid ${verBadgeColor}33`
                }}
              >
                {verStatus}
              </span>
              <span
                style={{
                  padding: '2px 8px',
                  borderRadius: '9999px',
                  fontSize: '0.725rem',
                  fontWeight: 700,
                  backgroundColor: slaBadgeBg,
                  color: slaBadgeColor,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px'
                }}
              >
                <FiClock size={11} />
                {slaBadgeText}
              </span>
            </div>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.4rem', margin: 0 }}>
              {complaint.title}
            </h2>
          </div>

          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-subtle)',
              cursor: 'pointer',
              padding: '6px',
            }}
          >
            <FiX size={20} />
          </button>
        </div>

        {/* Tab Navigation */}
        <div
          style={{
            display: 'flex',
            borderBottom: '1px solid var(--border-light)',
            backgroundColor: '#ffffff',
            padding: '0 1.5rem',
          }}
        >
          {[
            { id: 'overview', label: 'Overview & Evidence' },
            { id: 'audit', label: `Lifecycle Audit Trail (${complaint.statusHistory?.length || 0})` },
            { id: 'cpgrams', label: 'Government Portal (CPGRAMS)' },
            { id: 'receipt', label: 'Digital Receipt' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                padding: '0.75rem 1rem',
                border: 'none',
                background: 'none',
                borderBottom: activeTab === tab.id ? '2px solid var(--accent-primary)' : '2px solid transparent',
                color: activeTab === tab.id ? 'var(--accent-primary)' : 'var(--text-muted)',
                fontWeight: activeTab === tab.id ? 700 : 500,
                fontSize: '0.825rem',
                cursor: 'pointer',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Body Area */}
        <div style={{ padding: '1.5rem', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {error && (
            <div style={{ backgroundColor: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', padding: '0.65rem 0.85rem', borderRadius: '6px', fontSize: '0.825rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <FiAlertCircle size={15} />
              <span>{error}</span>
            </div>
          )}

          {actionSuccess && (
            <div style={{ backgroundColor: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0', padding: '0.65rem 0.85rem', borderRadius: '6px', fontSize: '0.825rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <FiCheckCircle size={15} />
              <span>{actionSuccess}</span>
            </div>
          )}

          {/* TAB 1: OVERVIEW & EVIDENCE */}
          {activeTab === 'overview' && (
            <>
              {/* Core Information Grid */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '0.75rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-light)',
                  borderRadius: '8px',
                  padding: '1rem',
                  fontSize: '0.825rem',
                }}
              >
                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem', textTransform: 'uppercase' }}>Department</span>
                  <div style={{ fontWeight: 600, color: 'var(--text-main)', marginTop: '2px' }}>
                    {complaint.departmentName || 'Public Works'} ({complaint.departmentCode || 'PWD'})
                  </div>
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem', textTransform: 'uppercase' }}>Assigned Field Crew</span>
                  <div style={{ fontWeight: 600, color: complaint.assignedToName ? 'var(--accent-teal)' : 'var(--text-subtle)', marginTop: '2px' }}>
                    {complaint.assignedToName ? `${complaint.assignedToName} (${complaint.assignedToRole || 'Worker'})` : 'Unassigned'}
                  </div>
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem', textTransform: 'uppercase' }}>Priority & SLA Target</span>
                  <div style={{ fontWeight: 600, color: 'var(--text-main)', marginTop: '2px' }}>
                    {complaint.priority} • {complaint.slaMinutes} Mins Window
                  </div>
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem', textTransform: 'uppercase' }}>Filing Citizen</span>
                  <div style={{ fontWeight: 600, color: 'var(--text-main)', marginTop: '2px' }}>
                    {complaint.citizenName} {complaint.citizenIsVerifiedBadge && '🏅'}
                  </div>
                  <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>{complaint.citizenPhone}</div>
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem', textTransform: 'uppercase' }}>Location & Coordinates</span>
                  <div style={{ fontWeight: 600, color: 'var(--text-main)', marginTop: '2px' }}>
                    <FiMapPin size={11} style={{ marginRight: '3px' }} />
                    {complaint.address || 'Smart City Zone'}
                  </div>
                  {complaint.latitude && (
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-subtle)', fontFamily: 'monospace' }}>
                      {complaint.latitude.toFixed(4)}, {complaint.longitude?.toFixed(4)}
                    </div>
                  )}
                </div>

                <div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem', textTransform: 'uppercase' }}>Filing Date</span>
                  <div style={{ fontWeight: 600, color: 'var(--text-main)', marginTop: '2px' }}>
                    {complaint.createdAt ? new Date(complaint.createdAt).toLocaleString() : 'N/A'}
                  </div>
                </div>
              </div>

              {/* Description */}
              <div>
                <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
                  Grievance Description
                </h4>
                <div
                  style={{
                    backgroundColor: '#ffffff',
                    border: '1px solid var(--border-light)',
                    borderRadius: '8px',
                    padding: '0.85rem 1rem',
                    fontSize: '0.85rem',
                    color: 'var(--text-main)',
                    lineHeight: 1.5,
                  }}
                >
                  {complaint.description || 'No additional description provided.'}
                </div>
              </div>

              {/* Attachments Section */}
              <div>
                <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
                  Citizen Photo Evidence ({complaint.attachments?.length || 0})
                </h4>
                {(complaint.attachments && complaint.attachments.length > 0) ? (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
                    {complaint.attachments.map(att => (
                      <div
                        key={att.id}
                        onClick={() => setPreviewImage(att.downloadUrl || `/api/complaints/attachments/${att.id}`)}
                        style={{
                          width: '110px',
                          height: '110px',
                          borderRadius: '8px',
                          overflow: 'hidden',
                          border: '1px solid var(--border-medium)',
                          cursor: 'pointer',
                          position: 'relative',
                          backgroundColor: '#f1f5f9',
                        }}
                        title={`Click to preview: ${att.originalFileName}`}
                      >
                        <img
                          src={att.downloadUrl || `/api/complaints/attachments/${att.id}`}
                          alt={att.originalFileName}
                          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                          onError={(e) => {
                            e.target.style.display = 'none';
                            e.target.parentNode.innerHTML = `<div style="padding: 10px; font-size: 11px; text-align: center; color: #64748b;">${att.originalFileName || 'Evidence'}</div>`;
                          }}
                        />
                      </div>
                    ))}
                  </div>
                ) : complaint.evidenceUrl ? (
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
                    <div
                      onClick={() => setPreviewImage(complaint.evidenceUrl)}
                      style={{
                        width: '110px',
                        height: '110px',
                        borderRadius: '8px',
                        overflow: 'hidden',
                        border: '1px solid var(--border-medium)',
                        cursor: 'pointer',
                        position: 'relative',
                        backgroundColor: '#f1f5f9',
                      }}
                      title="Primary Complaint Evidence Photo"
                    >
                      <img
                        src={complaint.evidenceUrl}
                        alt="Complaint Evidence"
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                        onError={(e) => {
                          e.target.style.display = 'none';
                          e.target.parentNode.innerHTML = `<div style="padding: 10px; font-size: 11px; text-align: center; color: #64748b;">Photo Evidence</div>`;
                        }}
                      />
                    </div>
                  </div>
                ) : (
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                    No photos were attached with this grievance.
                  </div>
                )}
              </div>

              {/* Field Resolution Proof Section */}
              {(complaint.resolutionNotes || complaint.resolutionEvidenceUrl || isResolvedOrClosed) && (
                <div
                  style={{
                    backgroundColor: '#f0fdf4',
                    border: '1px solid #bbf7d0',
                    borderRadius: '8px',
                    padding: '1rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <FiCheckCircle size={18} color="#16a34a" />
                      <h4 style={{ fontSize: '0.875rem', fontWeight: 700, color: '#166534', margin: 0 }}>
                        Field Resolution & Completion Report
                      </h4>
                    </div>
                    {complaint.resolvedAt && (
                      <span style={{ fontSize: '0.725rem', color: '#15803d' }}>
                        Completed: {new Date(complaint.resolvedAt).toLocaleString()}
                      </span>
                    )}
                  </div>

                  <p style={{ fontSize: '0.825rem', color: '#14532d', margin: '0 0 0.5rem 0', lineHeight: 1.5 }}>
                    {complaint.resolutionNotes || 'Resolution completed by assigned field worker.'}
                  </p>

                  {complaint.resolutionEvidenceUrl && (
                    <div style={{ marginTop: '0.5rem' }}>
                      <span style={{ fontSize: '0.725rem', color: '#166534', fontWeight: 600 }}>Resolution Evidence Photo:</span>
                      <div
                        onClick={() => setPreviewImage(complaint.resolutionEvidenceUrl)}
                        style={{
                          width: '120px',
                          height: '90px',
                          borderRadius: '6px',
                          overflow: 'hidden',
                          border: '1px solid #86efac',
                          marginTop: '4px',
                          cursor: 'pointer',
                        }}
                      >
                        <img
                          src={complaint.resolutionEvidenceUrl}
                          alt="Resolution Evidence"
                          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                        />
                      </div>
                    </div>
                  )}

                  {/* Officer Verification Buttons if RESOLVED */}
                  {complaint.status === 'RESOLVED' && (isOfficer || isAdmin) && (
                    <div style={{ marginTop: '0.75rem', paddingTop: '0.75rem', borderTop: '1px dashed #86efac', display: 'flex', gap: '0.75rem' }}>
                      <button
                        onClick={() => handleResolutionDecision('ACCEPTED')}
                        disabled={actionLoading}
                        className="btn btn-sm"
                        style={{ backgroundColor: '#16a34a', color: '#ffffff', fontWeight: 600, fontSize: '0.775rem' }}
                      >
                        ✓ Accept Resolution & Close Complaint
                      </button>
                      <button
                        onClick={() => handleResolutionDecision('REJECTED')}
                        disabled={actionLoading}
                        className="btn btn-sm"
                        style={{ backgroundColor: '#dc2626', color: '#ffffff', fontWeight: 600, fontSize: '0.775rem' }}
                      >
                        ✕ Reject & Send Back to In-Progress
                      </button>
                    </div>
                  )}
                </div>
              )}
            </>
          )}

          {/* TAB 2: AUDIT TRAIL TIMELINE */}
          {activeTab === 'audit' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Immutable lifecycle audit trail recorded under PostgreSQL with user identity, roles, and status transitions.
              </div>

              {complaint.statusHistory && complaint.statusHistory.length > 0 ? (
                <div style={{ position: 'relative', paddingLeft: '1.5rem', borderLeft: '2px solid var(--border-medium)', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                  {complaint.statusHistory.map((step, idx) => (
                    <div key={step.id || idx} style={{ position: 'relative' }}>
                      <div
                        style={{
                          position: 'absolute',
                          left: '-1.95rem',
                          top: '2px',
                          width: '14px',
                          height: '14px',
                          borderRadius: '50%',
                          backgroundColor: idx === 0 ? 'var(--accent-primary)' : '#94a3b8',
                          border: '2px solid #ffffff',
                        }}
                      />

                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <strong style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>
                          {step.newStatus || 'UPDATED'}
                        </strong>
                        {step.previousStatus && (
                          <span style={{ fontSize: '0.725rem', color: 'var(--text-muted)' }}>
                            (from {step.previousStatus})
                          </span>
                        )}
                        <span style={{ fontSize: '0.725rem', color: 'var(--text-subtle)', marginLeft: 'auto' }}>
                          {step.createdAt ? new Date(step.createdAt).toLocaleString() : 'Recent'}
                        </span>
                      </div>

                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                        Action by: <strong>{step.changedByName || 'System'}</strong> ({step.changedByRole || 'CITIZEN'})
                      </div>

                      {step.remarks && (
                        <div
                          style={{
                            marginTop: '0.35rem',
                            backgroundColor: '#f8fafc',
                            border: '1px solid var(--border-light)',
                            borderRadius: '6px',
                            padding: '0.45rem 0.75rem',
                            fontSize: '0.8rem',
                            color: 'var(--text-main)',
                          }}
                        >
                          {step.remarks}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)', fontSize: '0.825rem' }}>
                  No lifecycle history recorded yet.
                </div>
              )}
            </div>
          )}

          {/* TAB 3: GOVERNMENT PORTAL CPGRAMS */}
          {activeTab === 'cpgrams' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div
                style={{
                  backgroundColor: '#f0f9ff',
                  border: '1px solid #bae6fd',
                  borderRadius: '8px',
                  padding: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                }}
              >
                <FiShield size={24} color="var(--accent-primary)" />
                <div>
                  <div style={{ fontWeight: 700, color: 'var(--accent-teal)', fontSize: '0.9rem' }}>
                    Central Public Grievance Redress and Monitoring System (CPGRAMS Sandbox)
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#064E3B' }}>
                    Connects municipal grievances directly with the National Administrative Reform Cell.
                  </div>
                </div>
              </div>

              {complaint.externalTracking ? (
                <div style={{ border: '1px solid var(--border-light)', borderRadius: '8px', padding: '1.25rem', backgroundColor: '#ffffff' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                    <div>
                      <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Acknowledgement #</span>
                      <div style={{ fontFamily: 'monospace', fontWeight: 800, fontSize: '1.1rem', color: 'var(--accent-primary)' }}>
                        {complaint.externalTracking.acknowledgementNumber}
                      </div>
                    </div>
                    <div>
                      <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Government Status</span>
                      <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#166534' }}>
                        {complaint.externalTracking.externalStatus}
                      </div>
                    </div>
                  </div>

                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                    <div>Portal Adapter: <strong>{complaint.externalTracking.portalName || 'CPGRAMS Sandbox'}</strong></div>
                    <div>Nodal Officer: <strong>{complaint.externalTracking.officerInCharge || 'Central Grievance Cell'}</strong></div>
                    <div>Submitted Date: <strong>{complaint.externalTracking.submittedAt ? new Date(complaint.externalTracking.submittedAt).toLocaleString() : 'Recorded'}</strong></div>
                  </div>

                  <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-light)', display: 'flex', gap: '0.75rem' }}>
                    <button
                      onClick={async () => {
                        try {
                          await complaintsApi.syncExternalStatus(complaint.id);
                          await fetchComplaintDetails();
                          setActionSuccess('Synced latest status from CPGRAMS.');
                        } catch (e) {
                          setError('Sync failed.');
                        }
                      }}
                      className="btn btn-outline btn-sm"
                      style={{ fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                    >
                      <FiRefreshCw size={12} />
                      Sync Progress
                    </button>
                  </div>
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '2rem', border: '1px dashed var(--border-medium)', borderRadius: '8px' }}>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: '0 0 1rem 0' }}>
                    This complaint has not yet been submitted to the Central Government Grievance Portal.
                  </p>
                  <button
                    onClick={handleForwardToCPGRAMS}
                    disabled={actionLoading}
                    className="btn btn-primary"
                    style={{ backgroundColor: 'var(--accent-primary)', color: '#ffffff', fontWeight: 600, fontSize: '0.825rem' }}
                  >
                    Forward to CPGRAMS Sandbox
                  </button>
                </div>
              )}
            </div>
          )}

          {/* TAB 4: DIGITAL RECEIPT */}
          {activeTab === 'receipt' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div
                style={{
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-light)',
                  borderRadius: '8px',
                  padding: '1.25rem',
                  fontFamily: 'monospace',
                  fontSize: '0.8rem',
                  lineHeight: 1.6,
                  color: 'var(--text-main)',
                  whiteSpace: 'pre-wrap',
                }}
              >
{`================================================================================
CIVICPULSE SMART CITY COMMAND CENTRE
OFFICIAL CITIZEN GRIEVANCE ACKNOWLEDGEMENT RECEIPT
================================================================================

Receipt Number     : ${complaint.receipt?.receiptNumber || 'RCP-' + complaint.complaintNumber}
Complaint Sequence : ${complaint.complaintNumber}
Filing Date & Time : ${complaint.createdAt ? new Date(complaint.createdAt).toLocaleString() : 'N/A'}
Resolution Due     : ${complaint.slaDeadline ? new Date(complaint.slaDeadline).toLocaleString() : 'Standard SLA'}

CITIZEN DETAILS:
Citizen Name       : ${complaint.citizenName}
Registered Mobile  : ${complaint.citizenPhone}
Reputation Status  : ${complaint.citizenIsVerifiedBadge ? 'VERIFIED CITIZEN 🏅' : 'STANDARD CITIZEN'}

GRIEVANCE PARTICULARS:
Title              : ${complaint.title}
Category           : ${complaint.category}
Assigned Dept      : ${complaint.departmentName || 'Public Works'} (${complaint.departmentCode || 'PWD'})
Priority           : ${complaint.priority}
Guaranteed Window  : ${complaint.slaMinutes} Minutes

Notice:
Official digital record under Smart City Municipal Command Centre.
================================================================================`}
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                <button
                  onClick={handleDownloadReceipt}
                  className="btn btn-primary btn-sm"
                  style={{ display: 'flex', alignItems: 'center', gap: '5px', backgroundColor: 'var(--accent-primary)', color: '#ffffff' }}
                >
                  <FiDownload size={13} />
                  Download Official Receipt Document
                </button>
              </div>
            </div>
          )}

          {/* DIALOG POPOVERS FOR ACTIONS */}
          {/* 1. Assign Worker Dialog */}
          {showAssignDialog && (
            <div style={{ backgroundColor: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: '8px', padding: '1rem' }}>
              <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--accent-teal)', margin: '0 0 0.5rem 0' }}>
                Assign Complaint to Field Worker
              </h4>
              <form onSubmit={handleAssignSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Select Field Worker
                  </label>
                  <select
                    value={selectedAssignee}
                    onChange={(e) => setSelectedAssignee(e.target.value)}
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                    required
                  >
                    {assignees.map(a => (
                      <option key={a.id} value={a.id}>{a.fullName || a.username} ({a.role || 'FIELD_WORKER'})</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Assignment Instructions / Notes
                  </label>
                  <input
                    type="text"
                    value={assignNotes}
                    onChange={(e) => setAssignNotes(e.target.value)}
                    placeholder="Dispatch instructions, priority notes, etc."
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <button type="button" onClick={() => setShowAssignDialog(false)} className="btn btn-outline btn-sm">Cancel</button>
                  <button type="submit" disabled={actionLoading} className="btn btn-primary btn-sm">
                    Confirm Assignment
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* 2. Update Progress Dialog */}
          {showProgressDialog && (
            <div style={{ backgroundColor: '#fffbeb', border: '1px solid #fde68a', borderRadius: '8px', padding: '1rem' }}>
              <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: '#b45309', margin: '0 0 0.5rem 0' }}>
                Update Work Progress (Field Action)
              </h4>
              <form onSubmit={handleProgressSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Work Status
                  </label>
                  <select
                    value={progressStatus}
                    onChange={(e) => setProgressStatus(e.target.value)}
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                  >
                    <option value="IN_PROGRESS">In Progress (Field Work Underway)</option>
                    <option value="ASSIGNED">Assigned (En Route to Location)</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Progress Notes
                  </label>
                  <textarea
                    rows={2}
                    value={progressNotes}
                    onChange={(e) => setProgressNotes(e.target.value)}
                    placeholder="Describe field actions taken, equipment deployed, etc."
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Attach Site Photos
                  </label>
                  <input
                    type="file"
                    multiple
                    accept="image/jpeg,image/png,image/webp"
                    onChange={(e) => setProgressFiles(Array.from(e.target.files))}
                    style={{ fontSize: '0.8rem' }}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <button type="button" onClick={() => setShowProgressDialog(false)} className="btn btn-outline btn-sm">Cancel</button>
                  <button type="submit" disabled={actionLoading} className="btn btn-primary btn-sm" style={{ backgroundColor: '#d97706', color: '#ffffff' }}>
                    Save Progress Update
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* 3. Submit Resolution Dialog */}
          {showResolutionDialog && (
            <div style={{ backgroundColor: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px', padding: '1rem' }}>
              <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: '#166534', margin: '0 0 0.5rem 0' }}>
                Submit Final Resolution & Proof
              </h4>
              <form onSubmit={handleResolutionSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Resolution Notes & Remedy Taken <span style={{ color: '#dc2626' }}>*</span>
                  </label>
                  <textarea
                    rows={2}
                    value={resolutionNotes}
                    onChange={(e) => setResolutionNotes(e.target.value)}
                    placeholder="Describe how the issue was fixed, components replaced, clean-up completed..."
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Upload Resolution Proof Photo (JPEG, PNG, WebP)
                  </label>
                  <input
                    type="file"
                    multiple
                    accept="image/jpeg,image/png,image/webp"
                    onChange={(e) => setResolutionFiles(Array.from(e.target.files))}
                    style={{ fontSize: '0.8rem' }}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <button type="button" onClick={() => setShowResolutionDialog(false)} className="btn btn-outline btn-sm">Cancel</button>
                  <button type="submit" disabled={actionLoading} className="btn btn-primary btn-sm" style={{ backgroundColor: '#16a34a', color: '#ffffff' }}>
                    Submit Resolution
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* 4. Verify Grievance Dialog (Officer) */}
          {showVerifyDialog && (
            <div style={{ backgroundColor: '#f8fafc', border: '1px solid var(--border-medium)', borderRadius: '8px', padding: '1rem' }}>
              <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', margin: '0 0 0.5rem 0' }}>
                Department Officer Verification
              </h4>
              <form onSubmit={handleVerifySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Verification Decision
                  </label>
                  <select
                    value={verifyStatus}
                    onChange={(e) => setVerifyStatus(e.target.value)}
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                  >
                    <option value="GENUINE">GENUINE (Counts towards Verified Citizen Badge)</option>
                    <option value="REJECTED_SPAM">REJECTED SPAM (Invalid or malicious report)</option>
                    <option value="DUPLICATE">DUPLICATE (Already reported)</option>
                    <option value="UNVERIFIED">UNVERIFIED (Pending further review)</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Verification Remarks / Reason
                  </label>
                  <input
                    type="text"
                    value={verifyNotes}
                    onChange={(e) => setVerifyNotes(e.target.value)}
                    placeholder="Explanation for verification classification..."
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <button type="button" onClick={() => setShowVerifyDialog(false)} className="btn btn-outline btn-sm">Cancel</button>
                  <button type="submit" disabled={actionLoading} className="btn btn-primary btn-sm" style={{ backgroundColor: 'var(--accent-primary)', color: '#ffffff' }}>
                    Save Verification
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* 5. Change Priority Dialog (Admin) */}
          {showPriorityDialog && (
            <div style={{ backgroundColor: '#fff7ed', border: '1px solid #fed7aa', borderRadius: '8px', padding: '1rem' }}>
              <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: '#c2410c', margin: '0 0 0.5rem 0' }}>
                Adjust Priority & Recalculate SLA
              </h4>
              <form onSubmit={handlePrioritySubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    New Priority
                  </label>
                  <select
                    value={newPriority}
                    onChange={(e) => setNewPriority(e.target.value)}
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                  >
                    <option value="CRITICAL">Critical (4 Hours SLA)</option>
                    <option value="HIGH">High (12 Hours SLA)</option>
                    <option value="MEDIUM">Medium (24 Hours SLA)</option>
                    <option value="LOW">Low (48 Hours SLA)</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '4px' }}>
                    Administrative Reason <span style={{ color: '#dc2626' }}>*</span>
                  </label>
                  <input
                    type="text"
                    value={priorityReason}
                    onChange={(e) => setPriorityReason(e.target.value)}
                    placeholder="Reason for adjustment recorded into audit trail..."
                    className="form-control"
                    style={{ width: '100%', padding: '0.45rem', fontSize: '0.825rem' }}
                    required
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                  <button type="button" onClick={() => setShowPriorityDialog(false)} className="btn btn-outline btn-sm">Cancel</button>
                  <button type="submit" disabled={actionLoading} className="btn btn-primary btn-sm" style={{ backgroundColor: '#c2410c', color: '#ffffff' }}>
                    Apply Priority & SLA
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        {/* Action Toolbar */}
        <div
          style={{
            padding: '1rem 1.5rem',
            borderTop: '1px solid var(--border-light)',
            backgroundColor: '#f8fafc',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '0.5rem',
          }}
        >
          {/* Left Action Buttons depending on role */}
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {/* Officer / Admin: Assign Worker */}
            {(isOfficer || isAdmin) && (
              <button
                onClick={() => setShowAssignDialog(prev => !prev)}
                className="btn btn-outline btn-sm"
                style={{ fontSize: '0.775rem' }}
              >
                Assign Worker
              </button>
            )}

            {/* Officer / Admin: Verify Complaint */}
            {(isOfficer || isAdmin) && (
              <button
                onClick={() => setShowVerifyDialog(prev => !prev)}
                className="btn btn-outline btn-sm"
                style={{ fontSize: '0.775rem' }}
              >
                Verify Genuine/Spam
              </button>
            )}

            {/* Field Worker / Officer: Progress */}
            {(isFieldWorker || isOfficer || isAdmin) && complaint.status !== 'CLOSED' && (
              <button
                onClick={() => setShowProgressDialog(prev => !prev)}
                className="btn btn-outline btn-sm"
                style={{ fontSize: '0.775rem' }}
              >
                Update Progress
              </button>
            )}

            {/* Field Worker: Submit Resolution */}
            {(isFieldWorker || isOfficer || isAdmin) && complaint.status !== 'CLOSED' && complaint.status !== 'RESOLVED' && (
              <button
                onClick={() => setShowResolutionDialog(prev => !prev)}
                className="btn btn-sm"
                style={{ backgroundColor: '#16a34a', color: '#ffffff', fontWeight: 600, fontSize: '0.775rem' }}
              >
                Submit Resolution
              </button>
            )}

            {/* Admin: Priority */}
            {isAdmin && (
              <button
                onClick={() => setShowPriorityDialog(prev => !prev)}
                className="btn btn-outline btn-sm"
                style={{ fontSize: '0.775rem' }}
              >
                Change Priority
              </button>
            )}

            {/* Close / Reopen */}
            {(isAdmin || isCitizen) && (
              complaint.status === 'CLOSED' ? (
                <button
                  onClick={() => handleCloseOrReopen('REOPEN')}
                  className="btn btn-outline btn-sm"
                  style={{ fontSize: '0.775rem', color: '#b45309' }}
                >
                  Reopen Complaint
                </button>
              ) : (
                <button
                  onClick={() => handleCloseOrReopen('CLOSE')}
                  className="btn btn-outline btn-sm"
                  style={{ fontSize: '0.775rem', color: '#475569' }}
                >
                  Close Complaint
                </button>
              )
            )}
          </div>

          {/* Right Action: Close modal */}
          <div>
            <button
              onClick={onClose}
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '0.8rem' }}
            >
              Done
            </button>
          </div>
        </div>

        {/* Image Preview Popover */}
        {previewImage && (
          <div
            style={{
              position: 'fixed',
              inset: 0,
              backgroundColor: 'rgba(0,0,0,0.85)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 1200,
              padding: '2rem',
            }}
            onClick={() => setPreviewImage(null)}
          >
            <div style={{ position: 'relative', maxWidth: '90vw', maxHeight: '90vh' }}>
              <img
                src={previewImage}
                alt="Enlarged Preview"
                style={{ maxWidth: '100%', maxHeight: '85vh', borderRadius: '8px', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.5)' }}
              />
              <button
                onClick={() => setPreviewImage(null)}
                style={{
                  position: 'absolute',
                  top: '-35px',
                  right: '0',
                  background: 'none',
                  border: 'none',
                  color: '#ffffff',
                  fontSize: '1.25rem',
                  cursor: 'pointer'
                }}
              >
                <FiX size={24} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}


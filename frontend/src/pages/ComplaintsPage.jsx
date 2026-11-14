import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { complaintsApi } from '../api/complaintsApi';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/LoadingSpinner';
import UnifiedCitizenComplaintModal from '../components/UnifiedCitizenComplaintModal';
import ComplaintReceiptModal from '../components/ComplaintReceiptModal';
import ComplaintDetailsModal from '../components/ComplaintDetailsModal';
import ExternalGrievanceModal from '../components/ExternalGrievanceModal';
import {
  FiFileText,
  FiPlus,
  FiCheckCircle,
  FiSearch,
  FiShield,
  FiExternalLink,
  FiFilter,
  FiRefreshCw,
  FiAlertCircle,
  FiClock,
  FiEye,
  FiDownload
} from 'react-icons/fi';

const COMPLAINT_CATEGORIES = [
  { code: 'WATER_SUPPLY', label: 'Water Supply (WATER)' },
  { code: 'ROADS_AND_TRAFFIC', label: 'Roads & Infra (PWD)' },
  { code: 'ELECTRICITY', label: 'Electricity (ELECTRICAL)' },
  { code: 'SOLID_WASTE', label: 'Sanitation (SANITATION)' },
  { code: 'PUBLIC_HEALTH', label: 'Public Health (HEALTH)' },
  { code: 'TRAFFIC_MANAGEMENT', label: 'Traffic (TRAFFIC)' },
  { code: 'ENVIRONMENT', label: 'Environment (ENVIRONMENT)' },
  { code: 'EMERGENCY_SERVICES', label: 'Emergency (EMERGENCY)' },
  { code: 'GENERAL', label: 'General Civic Grievance' }
];

export const ComplaintsPage = () => {
  const { user, hasRole } = useAuth();
  const isOfficerOrAdmin = hasRole(['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER']);
  const isCitizen = hasRole(['CITIZEN']) && !hasRole(['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER']);

  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Filters
  const [filterVerification, setFilterVerification] = useState('');
  const [filterCategory, setFilterCategory] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterPriority, setFilterPriority] = useState('');
  const [filterSlaStatus, setFilterSlaStatus] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [showUnifiedModal, setShowUnifiedModal] = useState(false);
  const [receiptComplaint, setReceiptComplaint] = useState(null);
  const [detailsComplaint, setDetailsComplaint] = useState(null);
  const [trackingComplaintId, setTrackingComplaintId] = useState(null);

  const [searchParams] = useSearchParams();
  const directId = searchParams.get('id');

  useEffect(() => {
    if (directId) {
      if (complaints.length > 0) {
        const found = complaints.find(c => c.id === directId || c.complaintNumber === directId);
        if (found) {
          setDetailsComplaint(found);
          return;
        }
      }
      complaintsApi.getComplaintById(directId)
        .then(res => setDetailsComplaint(res.data || res))
        .catch(() => {});
    }
  }, [directId, complaints]);

  const fetchComplaints = async () => {
    try {
      setLoading(true);
      setErrorMsg('');
      const params = { size: 100 };
      if (filterVerification) params.verificationStatus = filterVerification;
      if (filterCategory) params.category = filterCategory;
      if (filterStatus) params.status = filterStatus;
      if (filterPriority) params.priority = filterPriority;
      if (filterSlaStatus) params.slaStatus = filterSlaStatus;
      if (searchQuery) params.search = searchQuery;

      const res = await complaintsApi.getComplaints(params);
      const raw = res.data?.content || res.data || res.content || (Array.isArray(res) ? res : []);
      setComplaints(Array.isArray(raw) ? raw : []);
    } catch (err) {
      console.warn('Could not fetch complaints', err);
      setErrorMsg('Failed to load complaints: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComplaints();
  }, [filterVerification, filterCategory, filterStatus, filterPriority, filterSlaStatus]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchComplaints();
  };

  return (
    <div className="container" style={{ padding: '1.5rem', maxWidth: '1280px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Banner */}
      <div className="cc-page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div className="cc-page-title-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="badge badge-low" style={{ fontSize: '0.75rem', fontWeight: 700 }}>
              MUNICIPAL GRIEVANCE CELL
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Logged in as: <strong>{user?.fullName || user?.username}</strong>
            </span>
          </div>
          <h1>Civic Grievances & Redressal Monitoring</h1>
          <p>Central intake for municipal complaints, guaranteed SLA tracking, field evidence, and CPGRAMS central redressals</p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          {isCitizen && (
            <button
              onClick={() => setShowUnifiedModal(true)}
              className="btn btn-primary"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                backgroundColor: 'var(--accent-primary)',
                borderColor: 'var(--accent-primary)',
                color: '#ffffff',
                fontWeight: 600,
                padding: '0.6rem 1.25rem',
              }}
            >
              <FiPlus size={16} />
              Lodge Civic Grievance
            </button>
          )}

          <button onClick={fetchComplaints} className="btn btn-outline" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <FiRefreshCw size={13} />
            Refresh
          </button>
        </div>
      </div>

      {successMsg && (
        <div style={{ backgroundColor: '#f0fdf4', color: '#166534', border: '1px solid #bbf7d0', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.825rem' }}>
          <FiCheckCircle style={{ marginRight: '6px', verticalAlign: 'middle' }} />
          {successMsg}
        </div>
      )}

      {errorMsg && (
        <div style={{ backgroundColor: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.825rem' }}>
          <FiAlertCircle style={{ marginRight: '6px', verticalAlign: 'middle' }} />
          {errorMsg}
        </div>
      )}

      {/* Filter and Search Bar */}
      <div
        className="cc-card"
        style={{
          padding: '1rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.75rem',
          backgroundColor: '#f8fafc',
        }}
      >
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '0.5rem', flex: '1 1 280px' }}>
          <div style={{ position: 'relative', width: '100%' }}>
            <FiSearch style={{ position: 'absolute', left: '10px', top: '10px', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Search by Record #, Title, Citizen, Address..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="form-control"
              style={{ paddingLeft: '32px', fontSize: '0.825rem', width: '100%' }}
            />
          </div>
          <button type="submit" className="btn btn-primary btn-sm" style={{ backgroundColor: 'var(--accent-primary)', color: '#ffffff' }}>
            Search
          </button>
        </form>

        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="form-control"
            style={{ fontSize: '0.8rem', padding: '0.45rem 0.65rem' }}
          >
            <option value="">All Categories</option>
            {COMPLAINT_CATEGORIES.map(c => (
              <option key={c.code} value={c.code}>{c.label}</option>
            ))}
          </select>

          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="form-control"
            style={{ fontSize: '0.8rem', padding: '0.45rem 0.65rem' }}
          >
            <option value="">All Statuses</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
            <option value="BREACHED">SLA Breached / Overdue</option>
          </select>

          <select
            value={filterPriority}
            onChange={(e) => setFilterPriority(e.target.value)}
            className="form-control"
            style={{ fontSize: '0.8rem', padding: '0.45rem 0.65rem' }}
          >
            <option value="">All Priorities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>

          <select
            value={filterSlaStatus}
            onChange={(e) => setFilterSlaStatus(e.target.value)}
            className="form-control"
            style={{ fontSize: '0.8rem', padding: '0.45rem 0.65rem' }}
          >
            <option value="">All SLA Statuses</option>
            <option value="ON_TRACK">On Track</option>
            <option value="AT_RISK">At Risk</option>
            <option value="BREACHED">Breached</option>
          </select>

          <select
            value={filterVerification}
            onChange={(e) => setFilterVerification(e.target.value)}
            className="form-control"
            style={{ fontSize: '0.8rem', padding: '0.45rem 0.65rem' }}
          >
            <option value="">All Verifications</option>
            <option value="GENUINE">Verified Genuine</option>
            <option value="UNVERIFIED">Unverified</option>
            <option value="REJECTED_SPAM">Rejected / Spam</option>
            <option value="DUPLICATE">Duplicate</option>
          </select>
        </div>
      </div>

      {/* Central Table */}
      <div className="cc-card">
        <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="cc-card-title">
            <FiFileText size={16} color="var(--accent-primary)" />
            Registered Complaints ({complaints.length})
          </span>
        </div>

        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center' }}>
            <LoadingSpinner text="Retrieving civic grievance register..." />
          </div>
        ) : complaints.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <FiFileText size={36} style={{ color: 'var(--text-subtle)', marginBottom: '0.5rem' }} />
            <div style={{ fontWeight: 600 }}>No complaints match the specified search or filter.</div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
              Clear your filters or lodge a new civic grievance.
            </p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="cc-table" style={{ width: '100%', textAlign: 'left', fontSize: '0.825rem' }}>
              <thead>
                <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-medium)' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Record #</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Title & Address</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Department</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Citizen</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Priority</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Verification</th>
                  <th style={{ padding: '0.75rem 1rem' }}>SLA Target</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {complaints.map(c => {
                  const remaining = c.remainingMinutes != null ? c.remainingMinutes : 0;
                  let slaColor = '#10B981';
                  let slaText = `${Math.floor(remaining / 60)}h ${Math.abs(remaining % 60)}m`;
                  if (['RESOLVED', 'CLOSED'].includes(c.status)) {
                    slaColor = 'var(--accent-teal)';
                    slaText = 'Resolved';
                  } else if (remaining < 0) {
                    slaColor = '#dc2626';
                    slaText = `Overdue (${Math.abs(Math.floor(remaining / 60))}h)`;
                  } else if (remaining <= 120) {
                    slaColor = '#ea580c';
                  }

                  return (
                    <tr key={c.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                      <td style={{ padding: '0.75rem 1rem', fontFamily: 'monospace', fontWeight: 800, color: 'var(--accent-primary)' }}>
                        {c.complaintNumber}
                      </td>
                      <td style={{ padding: '0.75rem 1rem', maxWidth: '240px' }}>
                        <div style={{ fontWeight: 600, color: 'var(--text-main)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {c.title}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{c.address}</div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>
                        {c.departmentCode || 'PWD'}
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <div>{c.citizenName} {c.citizenIsVerifiedBadge && '🏅'}</div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{c.citizenPhone}</div>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span
                          style={{
                            fontSize: '0.675rem',
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: '4px',
                            backgroundColor: c.priority === 'CRITICAL' ? '#fef2f2' : c.priority === 'HIGH' ? '#fff7ed' : '#f1f5f9',
                            color: c.priority === 'CRITICAL' ? '#dc2626' : c.priority === 'HIGH' ? '#ea580c' : '#334155'
                          }}
                        >
                          {c.priority}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span
                          style={{
                            fontSize: '0.675rem',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '9999px',
                            backgroundColor: '#f1f5f9',
                            color: '#334155',
                            border: '1px solid var(--border-light)'
                          }}
                        >
                          {c.status}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <span
                          style={{
                            fontSize: '0.675rem',
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: '4px',
                            backgroundColor: c.complaintVerificationStatus === 'GENUINE' ? '#f0fdf4' : '#f8fafc',
                            color: c.complaintVerificationStatus === 'GENUINE' ? '#16a34a' : '#64748b'
                          }}
                        >
                          {c.complaintVerificationStatus || 'UNVERIFIED'}
                        </span>
                      </td>
                      <td style={{ padding: '0.75rem 1rem', fontWeight: 600, color: slaColor }}>
                        {slaText}
                      </td>
                      <td style={{ padding: '0.75rem 1rem' }}>
                        <button
                          onClick={() => setDetailsComplaint(c)}
                          className="btn btn-outline btn-sm"
                          style={{ fontSize: '0.725rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                        >
                          <FiEye size={12} />
                          Details
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Unified Submission Modal */}
      {showUnifiedModal && isCitizen && (
        <UnifiedCitizenComplaintModal
          onClose={() => setShowUnifiedModal(false)}
          onSuccess={(created) => {
            setReceiptComplaint(created);
            fetchComplaints();
          }}
        />
      )}

      {/* Digital Acknowledgement Receipt Modal */}
      {receiptComplaint && (
        <ComplaintReceiptModal
          complaint={receiptComplaint}
          onClose={() => setReceiptComplaint(null)}
          onTrack={(c) => {
            setDetailsComplaint(c);
          }}
        />
      )}

      {/* Complaint Details 360 Modal */}
      {detailsComplaint && (
        <ComplaintDetailsModal
          complaintId={detailsComplaint.id}
          initialComplaint={detailsComplaint}
          onClose={() => setDetailsComplaint(null)}
          onUpdated={() => {
            fetchComplaints();
          }}
        />
      )}
    </div>
  );
};

export default ComplaintsPage;

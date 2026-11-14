import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { weatherApi } from '../../api/weatherApi';
import { complaintsApi } from '../../api/complaintsApi';
import UnifiedCitizenComplaintModal from '../UnifiedCitizenComplaintModal';
import ComplaintReceiptModal from '../ComplaintReceiptModal';
import ComplaintDetailsModal from '../ComplaintDetailsModal';
import VerifiedCitizenBadge from '../VerifiedCitizenBadge';
import ExternalGrievanceModal from '../ExternalGrievanceModal';
import LoadingSpinner from '../LoadingSpinner';
import {
  FiAlertCircle,
  FiFileText,
  FiMapPin,
  FiCloud,
  FiWind,
  FiPlus,
  FiCheckCircle,
  FiRefreshCw,
  FiShield,
  FiHelpCircle,
  FiExternalLink,
  FiAward,
  FiPhone,
  FiDownload,
  FiClock,
  FiEye
} from 'react-icons/fi';

export default function CitizenDashboardView({ user }) {
  const [loading, setLoading] = useState(true);
  const [myComplaints, setMyComplaints] = useState([]);
  const [reputation, setReputation] = useState(null);
  const [weather, setWeather] = useState(null);
  const [airQuality, setAirQuality] = useState(null);

  // Modals state
  const [showUnifiedModal, setShowUnifiedModal] = useState(false);
  const [receiptComplaint, setReceiptComplaint] = useState(null);
  const [detailsComplaint, setDetailsComplaint] = useState(null);
  const [trackingComplaintId, setTrackingComplaintId] = useState(null);

  // Status Filter Tab
  const [statusFilter, setStatusFilter] = useState('ALL');

  const loadCitizenData = async () => {
    setLoading(true);
    try {
      const [compRes, repRes, weatherRes, aqiRes] = await Promise.allSettled([
        complaintsApi.getComplaints({ size: 100 }),
        complaintsApi.getMyReputation().catch(() => null),
        weatherApi.getCurrentWeather(30.7333, 76.7794).catch(() => null),
        weatherApi.getAirQuality(30.7333, 76.7794).catch(() => null),
      ]);

      if (compRes.status === 'fulfilled') {
        const raw = compRes.value?.content || compRes.value?.data || compRes.value || [];
        setMyComplaints(Array.isArray(raw) ? raw : []);
      }
      if (repRes.status === 'fulfilled' && repRes.value) {
        setReputation(repRes.value?.data || repRes.value);
      }
      if (weatherRes.status === 'fulfilled' && weatherRes.value) {
        setWeather(weatherRes.value?.data || weatherRes.value);
      }
      if (aqiRes.status === 'fulfilled' && aqiRes.value) {
        setAirQuality(aqiRes.value?.data || aqiRes.value);
      }
    } catch (err) {
      console.error('Failed to load citizen data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCitizenData();
  }, []);

  const genuineCount = reputation?.genuineComplaintCount ?? user?.genuineComplaintCount ?? 0;
  const isVerifiedBadge = reputation?.verifiedCitizenBadge ?? user?.isVerifiedCitizen ?? false;
  const neededForBadge = Math.max(0, 5 - genuineCount);

  // Complaints filter tabs
  const filteredComplaints = myComplaints.filter((c) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'PENDING') return ['SUBMITTED', 'ASSIGNED'].includes(c.status);
    if (statusFilter === 'IN_PROGRESS') return c.status === 'IN_PROGRESS';
    if (statusFilter === 'RESOLVED') return c.status === 'RESOLVED';
    if (statusFilter === 'CLOSED') return c.status === 'CLOSED';
    return true;
  });

  const pendingCount = myComplaints.filter(c => ['SUBMITTED', 'ASSIGNED'].includes(c.status)).length;
  const inProgressCount = myComplaints.filter(c => c.status === 'IN_PROGRESS').length;
  const resolvedCount = myComplaints.filter(c => c.status === 'RESOLVED').length;
  const closedCount = myComplaints.filter(c => c.status === 'CLOSED').length;

  const externalGrievances = myComplaints.filter(c => c.externalTracking);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Banner */}
      <div className="cc-page-header">
        <div className="cc-page-title-group">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', flexWrap: 'wrap' }}>
            <span className="badge badge-low" style={{ fontSize: '0.75rem', fontWeight: 700 }}>
              CITIZEN RESIDENT PORTAL
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Welcome, <strong>{user?.fullName || user?.username}</strong>
            </span>
            {isVerifiedBadge && (
              <VerifiedCitizenBadge isVerified={true} genuineCount={genuineCount} size="sm" />
            )}
          </div>
          <h1>Civic Grievances, Incident Reporting & Public Safety</h1>
          <p>
            Report municipal issues with photo evidence, track real-time field progress with SLA guarantees, and monitor CPGRAMS government redressals
          </p>
        </div>

        {/* Single Unified Grievance Button */}
        <div>
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
              boxShadow: '0 4px 6px -1px rgba(2, 132, 199, 0.25)',
            }}
          >
            <FiPlus size={16} />
            Lodge Civic Grievance
          </button>
        </div>
      </div>

      {/* Feature 1 Notice: If citizen mobile is unverified */}
      {user && !user.isMobileVerified && (
        <div
          style={{
            backgroundColor: 'rgba(245, 158, 11, 0.12)',
            color: '#b45309',
            border: '1px solid rgba(245, 158, 11, 0.3)',
            padding: '0.85rem 1.15rem',
            borderRadius: '8px',
            fontSize: '0.825rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <FiPhone size={18} color="#d97706" />
            <div>
              <strong>Mobile Number Verification Required: </strong>
              To prevent fraudulent filings, citizens must verify their mobile phone number via secure OTP before reporting incidents or filing complaints.
            </div>
          </div>
          <Link
            to="/settings"
            className="btn btn-outline btn-sm"
            style={{ whiteSpace: 'nowrap', borderColor: '#d97706', color: '#b45309', fontWeight: 600 }}
          >
            Verify Mobile OTP
          </Link>
        </div>
      )}

      {/* Advisory & Weather & Reputation Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr', gap: '1rem' }}>
        {/* Verified Citizen Reputation Card */}
        <div
          className="cc-card"
          style={{
            padding: '1.15rem',
            display: 'flex',
            alignItems: 'center',
            gap: '1rem',
            border: isVerifiedBadge ? '1px solid rgba(245, 158, 11, 0.4)' : '1px solid var(--border-light)',
          }}
        >
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '10px',
              backgroundColor: isVerifiedBadge ? 'rgba(245, 158, 11, 0.15)' : '#ecfdf5',
              color: isVerifiedBadge ? '#d97706' : '#10B981',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '1.35rem',
              flexShrink: 0,
            }}
          >
            <FiAward />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Citizen Reputation</span>
              {isVerifiedBadge && (
                <span className="badge badge-low" style={{ fontSize: '0.65rem', padding: '1px 6px' }}>
                  VERIFIED CITIZEN
                </span>
              )}
            </div>
            <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-main)' }}>
              {genuineCount} Genuine Complaints
            </div>
            <div style={{ fontSize: '0.725rem', color: isVerifiedBadge ? '#15803d' : '#b45309', marginTop: '2px' }}>
              {isVerifiedBadge ? (
                'Verified Citizen Badge Active • Recognized Municipal Contributor'
              ) : (
                `${neededForBadge} more verified complaint${neededForBadge === 1 ? '' : 's'} needed for Verified Citizen Badge`
              )}
            </div>
          </div>
        </div>

        {/* City Weather Advisory Card */}
        <div className="cc-card" style={{ padding: '1.15rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '8px', backgroundColor: '#ecfdf5', color: 'var(--accent-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.25rem' }}>
            <FiCloud />
          </div>
          <div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>City Weather Advisory</div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-main)' }}>
              {weather?.temperature != null ? `${weather.temperature}°C` : '24.5°C'}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-subtle)' }}>
              {weather?.weatherDescription || 'Partly Cloudy • Humidity 55%'}
            </div>
          </div>
        </div>

        {/* Air Quality Index Card */}
        <div className="cc-card" style={{ padding: '1.15rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '8px', backgroundColor: '#fef3c7', color: '#d97706', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.25rem' }}>
            <FiWind />
          </div>
          <div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Air Quality Index (AQI)</div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#d97706' }}>
              {airQuality?.aqi != null ? `AQI ${airQuality.aqi}` : 'AQI 68 (Moderate)'}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-subtle)' }}>
              PM2.5: {airQuality?.pm25 != null ? `${airQuality.pm25} µg/m³` : '28 µg/m³'} • Normal Air
            </div>
          </div>
        </div>
      </div>

      {/* External Government Portal (CPGRAMS) Redressals */}
      {externalGrievances.length > 0 && (
        <div className="cc-card">
          <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="cc-card-title">
              <FiShield size={16} color="var(--accent-primary)" />
              Central Government Grievance Portal Tracking (CPGRAMS)
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {externalGrievances.map((c) => {
              const ext = c.externalTracking;
              return (
                <div
                  key={c.id}
                  style={{
                    border: '1px solid var(--border-light)',
                    borderRadius: '8px',
                    padding: '0.85rem 1rem',
                    backgroundColor: '#ffffff',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: '1rem',
                    flexWrap: 'wrap',
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                      <strong style={{ fontSize: '0.9rem', color: 'var(--text-main)' }}>
                        {c.title || c.complaintNumber}
                      </strong>
                      <span
                        style={{
                          fontSize: '0.7rem',
                          backgroundColor: 'rgba(16, 185, 129, 0.1)',
                          color: 'var(--accent-teal)',
                          border: '1px solid rgba(16, 185, 129, 0.3)',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontWeight: 700,
                          fontFamily: 'monospace',
                        }}
                      >
                        {ext.acknowledgementNumber}
                      </span>
                    </div>

                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                      <span>Portal: <strong>{ext.portalName || ext.portalCode}</strong></span>
                      <span>Nodal Officer: <strong>{ext.officerInCharge || 'Central Grievance Cell'}</strong></span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span
                      style={{
                        padding: '3px 8px',
                        borderRadius: '9999px',
                        fontSize: '0.7rem',
                        fontWeight: 700,
                        backgroundColor: ext.externalStatus === 'RESOLVED' ? '#ecfdf5' : '#f0fdf4',
                        color: ext.externalStatus === 'RESOLVED' ? '#065f46' : 'var(--accent-teal)',
                        border: `1px solid ${ext.externalStatus === 'RESOLVED' ? '#a7f3d0' : '#bbf7d0'}`,
                      }}
                    >
                      {ext.externalStatus}
                    </span>

                    <button
                      onClick={() => setTrackingComplaintId(c.id)}
                      className="btn btn-outline btn-sm"
                      style={{ fontSize: '0.725rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                    >
                      <FiExternalLink size={12} />
                      Track Progress
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Main Two Column Section: Citizen Complaints + Emergency Map */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.8fr 1.2fr', gap: '1.5rem' }}>
        {/* Left Column: My Reported Grievances & Municipal Complaints */}
        <div className="cc-card">
          <div className="cc-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <span className="cc-card-title">
              <FiFileText size={16} color="var(--accent-primary)" />
              My Reported Grievances ({myComplaints.length})
            </span>
            <button onClick={loadCitizenData} className="btn btn-outline btn-sm" style={{ fontSize: '0.725rem' }}>
              <FiRefreshCw size={11} style={{ marginRight: '4px' }} />
              Refresh
            </button>
          </div>

          {/* Status Filter Tabs */}
          <div style={{ display: 'flex', gap: '0.35rem', padding: '0.65rem 1rem', borderBottom: '1px solid var(--border-light)', overflowX: 'auto', backgroundColor: '#f8fafc' }}>
            {[
              { id: 'ALL', label: `All (${myComplaints.length})` },
              { id: 'PENDING', label: `Pending (${pendingCount})` },
              { id: 'IN_PROGRESS', label: `In Progress (${inProgressCount})` },
              { id: 'RESOLVED', label: `Resolved (${resolvedCount})` },
              { id: 'CLOSED', label: `Closed (${closedCount})` },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setStatusFilter(tab.id)}
                style={{
                  padding: '4px 10px',
                  borderRadius: '6px',
                  border: statusFilter === tab.id ? '1px solid var(--accent-primary)' : '1px solid var(--border-light)',
                  backgroundColor: statusFilter === tab.id ? 'rgba(2, 132, 199, 0.1)' : '#ffffff',
                  color: statusFilter === tab.id ? 'var(--accent-primary)' : 'var(--text-muted)',
                  fontSize: '0.725rem',
                  fontWeight: statusFilter === tab.id ? 700 : 500,
                  cursor: 'pointer',
                  whiteSpace: 'nowrap'
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Complaints List Area */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', padding: '1rem' }}>
            {loading ? (
              <div style={{ padding: '2rem', textAlign: 'center' }}>
                <LoadingSpinner />
                <p style={{ marginTop: '0.5rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>Retrieving your filed complaints...</p>
              </div>
            ) : filteredComplaints.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '2.5rem', color: 'var(--text-muted)' }}>
                <FiFileText size={32} style={{ color: 'var(--text-subtle)', marginBottom: '0.5rem' }} />
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>No complaints found in this view</div>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', margin: '0.35rem 0 1rem 0' }}>
                  Click below to submit an issue with photos and receive an official digital receipt.
                </p>
                <button
                  onClick={() => setShowUnifiedModal(true)}
                  className="btn btn-primary btn-sm"
                  style={{ backgroundColor: 'var(--accent-primary)', color: '#ffffff', fontWeight: 600 }}
                >
                  Lodge Civic Grievance
                </button>
              </div>
            ) : (
              filteredComplaints.map((c) => {
                const remaining = c.remainingMinutes != null ? c.remainingMinutes : 0;
                let slaBadge = { label: 'ON TRACK', bg: '#f0fdf4', color: '#16a34a' };
                if (['RESOLVED', 'CLOSED'].includes(c.status)) {
                  slaBadge = { label: 'RESOLVED', bg: '#ecfdf5', color: '#065f46' };
                } else if (remaining < 0) {
                  slaBadge = { label: 'SLA BREACHED', bg: '#fef2f2', color: '#dc2626' };
                } else if (remaining <= 120) {
                  slaBadge = { label: 'SLA AT RISK', bg: '#fff7ed', color: '#ea580c' };
                }

                let verBadge = { label: c.complaintVerificationStatus || 'UNVERIFIED', bg: '#f1f5f9', color: '#475569' };
                if (c.complaintVerificationStatus === 'GENUINE') {
                  verBadge = { label: 'GENUINE', bg: '#f0fdf4', color: '#16a34a' };
                } else if (c.complaintVerificationStatus?.includes('REJECT') || c.complaintVerificationStatus === 'SPAM') {
                  verBadge = { label: 'REJECTED', bg: '#fef2f2', color: '#dc2626' };
                }

                return (
                  <div
                    key={c.id}
                    style={{
                      border: '1px solid var(--border-light)',
                      borderRadius: '8px',
                      padding: '1rem',
                      backgroundColor: '#ffffff',
                      boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.05)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '0.65rem',
                      transition: 'border-color 0.15s ease',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.75rem', flexWrap: 'wrap' }}>
                      <div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem', flexWrap: 'wrap' }}>
                          <span
                            style={{
                              fontFamily: 'monospace',
                              fontWeight: 800,
                              fontSize: '0.85rem',
                              color: 'var(--accent-primary)',
                              backgroundColor: '#e0f2fe',
                              padding: '1px 6px',
                              borderRadius: '4px',
                            }}
                          >
                            {c.complaintNumber}
                          </span>
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
                          <span
                            style={{
                              fontSize: '0.675rem',
                              fontWeight: 700,
                              padding: '2px 8px',
                              borderRadius: '9999px',
                              backgroundColor: verBadge.bg,
                              color: verBadge.color,
                            }}
                          >
                            {verBadge.label}
                          </span>
                          <span
                            style={{
                              fontSize: '0.675rem',
                              fontWeight: 700,
                              padding: '2px 8px',
                              borderRadius: '9999px',
                              backgroundColor: slaBadge.bg,
                              color: slaBadge.color,
                              display: 'flex',
                              alignItems: 'center',
                              gap: '3px'
                            }}
                          >
                            <FiClock size={10} />
                            {slaBadge.label}
                          </span>
                        </div>

                        <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', margin: '0.2rem 0' }}>
                          {c.title}
                        </h3>

                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                          <span>Dept: <strong>{c.departmentName || 'Public Works'} ({c.departmentCode || 'PWD'})</strong></span>
                          <span>Priority: <strong>{c.priority}</strong></span>
                          <span>
                            <FiMapPin size={11} style={{ marginRight: '2px', verticalAlign: 'middle' }} />
                            {c.address}
                          </span>
                        </div>
                      </div>

                      {/* Action buttons on card */}
                      <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                        <button
                          onClick={() => {
                            setDetailsComplaint(c);
                          }}
                          className="btn btn-outline btn-sm"
                          style={{ fontSize: '0.725rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                        >
                          <FiEye size={12} />
                          Details & Progress
                        </button>
                        <button
                          onClick={() => {
                            setReceiptComplaint(c);
                          }}
                          className="btn btn-outline btn-sm"
                          style={{ fontSize: '0.725rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                        >
                          <FiDownload size={12} />
                          Receipt
                        </button>
                      </div>
                    </div>

                    {c.description && (
                      <div style={{ fontSize: '0.775rem', color: 'var(--text-muted)', lineClamp: 2, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                        {c.description}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right Column: Public Safety Map & Emergency Helplines */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiMapPin size={15} color="var(--accent-primary)" />
                Public Safety & Municipal Infrastructure
              </span>
            </div>
            <p style={{ fontSize: '0.775rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
              Real-time interactive city command map displaying civic work zones, monitored water pipelines, power stations, and public works.
            </p>
            <Link to="/map" className="btn btn-primary btn-sm" style={{ width: '100%', textAlign: 'center' }}>
              Explore Interactive Map →
            </Link>
          </div>

          <div className="cc-card">
            <div className="cc-card-header">
              <span className="cc-card-title">
                <FiHelpCircle size={15} color="var(--accent-teal)" />
                Municipal Emergency Helplines
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', fontSize: '0.8rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0.6rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '4px' }}>
                <span>Police / Law Enforcement</span>
                <strong>112</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0.6rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '4px' }}>
                <span>Fire & Rescue Squad</span>
                <strong>101</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0.6rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '4px' }}>
                <span>Ambulance & Medical Emergency</span>
                <strong>108</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0.4rem 0.6rem', backgroundColor: 'var(--bg-subtle)', borderRadius: '4px' }}>
                <span>Municipal Grievance Helpline</span>
                <strong>1800-180-2026</strong>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Unified Complaint Submission Modal */}
      {showUnifiedModal && (
        <UnifiedCitizenComplaintModal
          onClose={() => setShowUnifiedModal(false)}
          onSuccess={(newComplaint) => {
            setReceiptComplaint(newComplaint);
            loadCitizenData();
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

      {/* Detailed Complaint 360 Modal */}
      {detailsComplaint && (
        <ComplaintDetailsModal
          complaintId={detailsComplaint.id}
          initialComplaint={detailsComplaint}
          onClose={() => setDetailsComplaint(null)}
          onUpdated={() => {
            loadCitizenData();
          }}
        />
      )}

      {/* External CPGRAMS Tracking Modal */}
      {trackingComplaintId && (
        <ExternalGrievanceModal
          complaintId={trackingComplaintId}
          onClose={() => setTrackingComplaintId(null)}
          onUpdated={() => loadCitizenData()}
        />
      )}
    </div>
  );
}

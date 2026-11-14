import React, { useState } from 'react';
import { complaintsApi } from '../api/complaintsApi';
import { useAuth } from '../context/AuthContext';
import {
  FiX,
  FiSend,
  FiUploadCloud,
  FiImage,
  FiTrash2,
  FiMapPin,
  FiAlertCircle,
  FiClock,
  FiShield,
  FiCheckCircle
} from 'react-icons/fi';

const CATEGORIES = [
  {
    code: 'WATER_SUPPLY',
    name: 'Water Supply & Sewerage',
    dept: 'Water Supply & Sewerage Board (WATER)',
    subcategories: ['Pipeline Leakage', 'No Water Supply', 'Contaminated Water', 'Low Pressure', 'Sewage Overflow', 'Damaged Water Meter']
  },
  {
    code: 'ROADS_AND_TRAFFIC',
    name: 'Roads & Infrastructure (PWD)',
    dept: 'Public Works Department (PWD)',
    subcategories: ['Pothole / Crater', 'Damaged Road Surface', 'Broken Footpath', 'Fallen Tree / Branch', 'Unfinished Roadwork', 'Open Manhole']
  },
  {
    code: 'ELECTRICITY',
    name: 'Electricity & Lighting',
    dept: 'Electricity Distribution Board (ELECTRICAL)',
    subcategories: ['Streetlight Not Working', 'Power Outage', 'Hanging Electric Cable', 'Transformer Sparking', 'Damaged Electric Pole']
  },
  {
    code: 'SOLID_WASTE',
    name: 'Solid Waste & Sanitation',
    dept: 'Municipal Sanitation & Waste (SANITATION)',
    subcategories: ['Garbage Dump Overflow', 'Irregular Waste Collection', 'Debris on Public Road', 'Public Toilet Cleaning', 'Dead Animal Removal']
  },
  {
    code: 'PUBLIC_HEALTH',
    name: 'Public Health & Disease Control',
    dept: 'Department of Public Health (HEALTH)',
    subcategories: ['Mosquito Breeding / Dengue Risk', 'Stray Dog Menace', 'Food Safety Issue', 'Chemical / Drainage Pollution', 'Stagnant Dirty Water']
  },
  {
    code: 'TRAFFIC_MANAGEMENT',
    name: 'Traffic & Road Safety',
    dept: 'Traffic Management Bureau (TRAFFIC)',
    subcategories: ['Traffic Signal Out of Order', 'Illegal Parking Obstruction', 'Missing Road Sign / Divider', 'Traffic Congestion Point']
  },
  {
    code: 'ENVIRONMENT',
    name: 'Environment & Pollution',
    dept: 'Pollution Control & Ecology (ENVIRONMENT)',
    subcategories: ['Air / Smoke Emission', 'Loud Noise / Industrial Violation', 'Illegal Tree Felling', 'Industrial Effluent Discharge']
  },
  {
    code: 'EMERGENCY_SERVICES',
    name: 'Emergency & Public Safety',
    dept: 'Emergency Response Command (EMERGENCY)',
    subcategories: ['Structural Collapse Hazard', 'Fire / Hazard Report', 'Flooding Hazard', 'Immediate Public Danger']
  },
  {
    code: 'GENERAL',
    name: 'General Civic Grievance',
    dept: 'Public Works Department (PWD)',
    subcategories: ['Public Property Encroachment', 'Municipal Office Grievance', 'Other Civic Issue']
  }
];

const PRIORITY_SLAS = {
  CRITICAL: { label: 'Critical (4 Hours SLA)', color: '#b91c1c', bg: '#fef2f2', border: '#fecaca' },
  HIGH: { label: 'High (12 Hours SLA)', color: '#c2410c', bg: '#fff7ed', border: '#fed7aa' },
  MEDIUM: { label: 'Medium (24 Hours SLA)', color: '#b45309', bg: '#fffbeb', border: '#fde68a' },
  LOW: { label: 'Low (48 Hours SLA)', color: '#15803d', bg: '#f0fdf4', border: '#bbf7d0' }
};

export default function UnifiedCitizenComplaintModal({ onClose, onSuccess }) {
  const { user, hasRole } = useAuth();
  const isCitizen = hasRole(['CITIZEN']) && !hasRole(['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER']);

  const [categoryCode, setCategoryCode] = useState('ROADS_AND_TRAFFIC');
  const [subcategory, setSubcategory] = useState('Pothole / Crater');
  const [priority, setPriority] = useState('MEDIUM');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [address, setAddress] = useState('Sector 17 Plaza, Central Smart City Zone');
  const [latitude, setLatitude] = useState(30.7333);
  const [longitude, setLongitude] = useState(76.7794);
  const [citizenPhone, setCitizenPhone] = useState(user?.mobileNumber || user?.phone || '');
  const [files, setFiles] = useState([]);
  const [previews, setPreviews] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const selectedCategory = CATEGORIES.find(c => c.code === categoryCode) || CATEGORIES[0];

  const handleCategoryChange = (e) => {
    const code = e.target.value;
    setCategoryCode(code);
    const cat = CATEGORIES.find(c => c.code === code);
    if (cat && cat.subcategories.length > 0) {
      setSubcategory(cat.subcategories[0]);
    }
  };

  const handleFileChange = (e) => {
    const selectedFiles = Array.from(e.target.files);
    if (!selectedFiles.length) return;

    const validFiles = [];
    const newPreviews = [];

    for (const file of selectedFiles) {
      if (file.size > 10 * 1024 * 1024) {
        setError(`File "${file.name}" exceeds 10MB maximum limit.`);
        continue;
      }
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
        setError(`File "${file.name}" is not a supported image (JPEG, PNG, WebP only).`);
        continue;
      }
      validFiles.push(file);
      newPreviews.push({
        name: file.name,
        size: (file.size / 1024).toFixed(1) + ' KB',
        url: URL.createObjectURL(file)
      });
    }

    setFiles(prev => [...prev, ...validFiles]);
    setPreviews(prev => [...prev, ...newPreviews]);
  };

  const removeFile = (index) => {
    setFiles(prev => prev.filter((_, i) => i !== index));
    setPreviews(prev => {
      const item = prev[index];
      if (item?.url) URL.revokeObjectURL(item.url);
      return prev.filter((_, i) => i !== index);
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!title.trim() && !subcategory) {
      setError('Please provide a brief title or select a subcategory.');
      return;
    }

    if (!description.trim() || description.trim().length < 10) {
      setError('Please enter a detailed description of at least 10 characters.');
      return;
    }

    setLoading(true);

    try {
      const finalTitle = title.trim() || `${subcategory} - ${address.split(',')[0] || selectedCategory.name}`;

      const formData = new FormData();
      formData.append('title', finalTitle);
      formData.append('category', selectedCategory.code);
      formData.append('subcategory', subcategory);
      formData.append('priority', priority);
      formData.append('description', description.trim());
      formData.append('address', address.trim());
      formData.append('latitude', latitude.toString());
      formData.append('longitude', longitude.toString());
      formData.append('citizenPhone', citizenPhone);

      files.forEach(file => {
        formData.append('files', file);
      });

      const response = await complaintsApi.createComplaintMultipart(formData);
      const createdComplaint = response.data || response;

      if (onSuccess) {
        onSuccess(createdComplaint);
      }
      onClose();
    } catch (err) {
      console.error('Unified complaint submission error:', err);
      const msg = err.response?.data?.message || err.message || 'Failed to submit grievance.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  if (!isCitizen) {
    return (
      <div
        style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(15, 23, 42, 0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1050,
          padding: '1rem',
        }}
        onClick={onClose}
      >
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '2rem',
            borderRadius: '10px',
            maxWidth: '440px',
            textAlign: 'center',
            boxShadow: '0 20px 25px -5px rgba(0,0,0,0.2)'
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div style={{ color: '#dc2626', marginBottom: '0.75rem', fontSize: '2rem' }}>⚠️</div>
          <h3 style={{ color: '#0f172a', fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem' }}>
            Citizen-Only Grievance Intake
          </h3>
          <p style={{ fontSize: '0.825rem', color: '#64748b', lineHeight: 1.5, marginBottom: '1.25rem' }}>
            The civic grievance submission portal is strictly designated for verified Citizen accounts. Operational and administrative staff manage grievances from their respective Command Centre consoles.
          </p>
          <button onClick={onClose} className="btn btn-secondary" style={{ padding: '0.5rem 1.25rem' }}>
            Dismiss
          </button>
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1050,
        padding: '1rem',
      }}
      onClick={onClose}
    >
      <div
        className="cc-modal"
        style={{
          width: '100%',
          maxWidth: '720px',
          maxHeight: '92vh',
          display: 'flex',
          flexDirection: 'column',
          backgroundColor: '#ffffff',
          borderRadius: '10px',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1)',
          overflow: 'hidden'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid var(--border-light)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            backgroundColor: '#f8fafc'
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span style={{ color: 'var(--accent-primary)', fontSize: '1.2rem', display: 'flex' }}>
                <FiShield />
              </span>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-main)', margin: 0 }}>
                Unified Citizen Grievance Portal
              </h2>
            </div>
            <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Direct intake to CivicPulse Command Centre with automated routing & guaranteed SLA tracking
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-subtle)',
              cursor: 'pointer',
              padding: '6px',
              borderRadius: '6px'
            }}
          >
            <FiX size={20} />
          </button>
        </div>

        {/* Modal Body */}
        <form onSubmit={handleSubmit} style={{ overflowY: 'auto', padding: '1.5rem', flex: 1, display: 'flex', flexDirection: 'column', gap: '1.2rem' }}>
          {error && (
            <div
              style={{
                backgroundColor: '#fef2f2',
                color: '#991b1b',
                border: '1px solid #fecaca',
                padding: '0.75rem 1rem',
                borderRadius: '6px',
                fontSize: '0.825rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}
            >
              <FiAlertCircle size={16} style={{ flexShrink: 0 }} />
              <span>{error}</span>
            </div>
          )}

          {/* Department Auto-Routing Banner */}
          <div
            style={{
              backgroundColor: '#f0f9ff',
              border: '1px solid #bae6fd',
              borderRadius: '8px',
              padding: '0.85rem 1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '1rem'
            }}
          >
            <div>
              <span style={{ fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.5px', color: 'var(--accent-teal)', fontWeight: 600 }}>
                Automated Department Routing
              </span>
              <div style={{ fontWeight: 600, color: '#0c4a6e', fontSize: '0.875rem' }}>
                {selectedCategory.dept}
              </div>
            </div>
            <div
              style={{
                padding: '4px 10px',
                borderRadius: '6px',
                backgroundColor: PRIORITY_SLAS[priority].bg,
                color: PRIORITY_SLAS[priority].color,
                border: `1px solid ${PRIORITY_SLAS[priority].border}`,
                fontSize: '0.75rem',
                fontWeight: 700,
                display: 'flex',
                alignItems: 'center',
                gap: '4px'
              }}
            >
              <FiClock size={12} />
              {PRIORITY_SLAS[priority].label}
            </div>
          </div>

          {/* Category & Subcategory Selection */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
                Grievance Category <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <select
                value={categoryCode}
                onChange={handleCategoryChange}
                className="form-control"
                style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
                required
              >
                {CATEGORIES.map(c => (
                  <option key={c.code} value={c.code}>{c.name}</option>
                ))}
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
                Subcategory Issue <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <select
                value={subcategory}
                onChange={(e) => setSubcategory(e.target.value)}
                className="form-control"
                style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
                required
              >
                {selectedCategory.subcategories.map(sub => (
                  <option key={sub} value={sub}>{sub}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Priority & Mobile Number */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
                Urgency / Priority Level <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="form-control"
                style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
              >
                <option value="LOW">Low (SLA: 48 Hours)</option>
                <option value="MEDIUM">Medium (SLA: 24 Hours) - Standard</option>
                <option value="HIGH">High (SLA: 12 Hours) - Severe Impact</option>
                <option value="CRITICAL">Critical (SLA: 4 Hours) - Emergency / Safety</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
                Citizen Registered Mobile <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <input
                type="text"
                value={citizenPhone}
                onChange={(e) => setCitizenPhone(e.target.value)}
                placeholder="+919876543210"
                className="form-control"
                style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
                required
              />
            </div>
          </div>

          {/* Grievance Title */}
          <div>
            <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
              Brief Headline / Title
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={`E.g., Severe ${subcategory.toLowerCase()} near Market Road`}
              className="form-control"
              style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
            />
          </div>

          {/* Description */}
          <div>
            <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
              Grievance Description <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide exact details of the issue, landmarks, severity, and any public safety risks..."
              className="form-control"
              style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem', resize: 'vertical' }}
              required
            />
          </div>

          {/* Location & Coordinates */}
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
                <FiMapPin size={12} style={{ marginRight: '4px' }} />
                Incident Address / Landmark <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <input
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="Street address or ward landmark"
                className="form-control"
                style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
                required
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                Latitude
              </label>
              <input
                type="number"
                step="0.0001"
                value={latitude}
                onChange={(e) => setLatitude(parseFloat(e.target.value) || 30.7333)}
                className="form-control"
                style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                Longitude
              </label>
              <input
                type="number"
                step="0.0001"
                value={longitude}
                onChange={(e) => setLongitude(parseFloat(e.target.value) || 76.7794)}
                className="form-control"
                style={{ width: '100%', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-medium)', fontSize: '0.85rem' }}
              />
            </div>
          </div>

          {/* Photo Evidence Upload */}
          <div>
            <label style={{ display: 'block', fontSize: '0.785rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.35rem' }}>
              Photo Evidence (JPEG, PNG, WebP up to 10MB)
            </label>
            <div
              style={{
                border: '2px dashed var(--border-medium)',
                borderRadius: '8px',
                padding: '1.25rem',
                textAlign: 'center',
                backgroundColor: '#f8fafc',
                cursor: 'pointer',
                position: 'relative'
              }}
            >
              <input
                type="file"
                multiple
                accept="image/jpeg,image/png,image/webp"
                onChange={handleFileChange}
                style={{
                  position: 'absolute',
                  inset: 0,
                  opacity: 0,
                  cursor: 'pointer',
                  width: '100%',
                  height: '100%'
                }}
              />
              <FiUploadCloud size={28} color="var(--accent-primary)" style={{ marginBottom: '0.35rem' }} />
              <div style={{ fontSize: '0.825rem', fontWeight: 600, color: 'var(--text-main)' }}>
                Click or drag & drop photos here to attach proof
              </div>
              <div style={{ fontSize: '0.725rem', color: 'var(--text-subtle)', marginTop: '2px' }}>
                Clear photos help field crews locate and resolve grievances faster
              </div>
            </div>

            {/* Thumbnails preview */}
            {previews.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginTop: '0.75rem' }}>
                {previews.map((item, idx) => (
                  <div
                    key={idx}
                    style={{
                      position: 'relative',
                      width: '80px',
                      height: '80px',
                      borderRadius: '6px',
                      overflow: 'hidden',
                      border: '1px solid var(--border-medium)'
                    }}
                  >
                    <img
                      src={item.url}
                      alt={item.name}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                    <button
                      type="button"
                      onClick={() => removeFile(idx)}
                      style={{
                        position: 'absolute',
                        top: '2px',
                        right: '2px',
                        backgroundColor: 'rgba(0, 0, 0, 0.7)',
                        color: '#ffffff',
                        border: 'none',
                        borderRadius: '50%',
                        width: '20px',
                        height: '20px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        cursor: 'pointer'
                      }}
                      title="Remove image"
                    >
                      <FiTrash2 size={11} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Modal Footer Actions */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '0.75rem',
              paddingTop: '1rem',
              borderTop: '1px solid var(--border-light)'
            }}
          >
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="btn btn-outline"
              style={{ padding: '0.6rem 1.25rem', fontSize: '0.85rem' }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
              style={{
                padding: '0.6rem 1.5rem',
                fontSize: '0.85rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                backgroundColor: 'var(--accent-primary)',
                borderColor: 'var(--accent-primary)',
                color: '#ffffff',
                fontWeight: 600
              }}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
                  Generating Acknowledgement...
                </>
              ) : (
                <>
                  <FiSend size={15} />
                  Submit Grievance & Get Receipt
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}


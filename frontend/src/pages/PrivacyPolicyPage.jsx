import React from 'react';
import { Link } from 'react-router-dom';
import { FiShield, FiLock, FiDatabase, FiEye, FiArrowLeft, FiCheckCircle } from 'react-icons/fi';

export const PrivacyPolicyPage = () => {
  return (
    <div style={{ maxWidth: '900px', margin: '0 auto', padding: '2.5rem 1.5rem', color: 'var(--text-primary)' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <Link to="/dashboard" className="btn btn-outline btn-sm" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
          <FiArrowLeft size={14} /> Back to Command Centre
        </Link>
      </div>

      <div className="cc-card" style={{ padding: '2.5rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1.5rem', marginBottom: '2rem' }}>
          <div style={{ width: '48px', height: '48px', borderRadius: '8px', background: 'var(--accent-primary-light, #e0f2fe)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-primary)' }}>
            <FiShield size={26} />
          </div>
          <div>
            <h1 style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Municipal Command Centre Privacy & Data Governance Policy
            </h1>
            <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
              CivicPulse Smart City Platform — Compliance with Municipal Data Protection Standards
            </p>
          </div>
        </div>

        <section style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)' }}>
            <FiLock size={16} color="var(--accent-primary)" /> 1. Scope and Purpose
          </h2>
          <p style={{ fontSize: '0.9rem', lineHeight: '1.6', color: 'var(--text-secondary)' }}>
            The CivicPulse Smart City Command Centre processes municipal infrastructure events, automated IoT environmental sensor telemetry, citizen grievance reports, and emergency dispatch workflows. This platform operates strictly for municipal public safety, resource dispatch, and urban infrastructure maintenance.
          </p>
        </section>

        <section style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)' }}>
            <FiDatabase size={16} color="var(--accent-teal)" /> 2. Data We Ingest and Process
          </h2>
          <ul style={{ fontSize: '0.9rem', lineHeight: '1.7', color: 'var(--text-secondary)', paddingLeft: '1.5rem' }}>
            <li><strong>Citizen Incident Submissions:</strong> Location coordinates, hazard type descriptions, contact phone/email for service tracking, and optional photographic evidence.</li>
            <li><strong>IoT Sensor Telemetry:</strong> Real-time air quality (PM2.5, PM10, AQI), weather observations (temperature, precipitation, wind speed), storm water drainage levels, traffic signal states, and fill status of municipal waste bins.</li>
            <li><strong>Operational Dispatch Records:</strong> Response action tickets, SLA tracking timelines, department assignments, and resolution notes logged by authorized municipal officers.</li>
          </ul>
        </section>

        <section style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)' }}>
            <FiEye size={16} color="var(--accent-primary)" /> 3. Data Protection and Security Controls
          </h2>
          <ul style={{ fontSize: '0.9rem', lineHeight: '1.7', color: 'var(--text-secondary)', paddingLeft: '1.5rem' }}>
            <li><strong>Cryptographic Password Protection:</strong> All user credentials are salted and hashed utilizing the BCrypt algorithm. Plain-text passwords are never stored or transmitted.</li>
            <li><strong>Role-Based Access Control (RBAC):</strong> Strict role segregation ensures citizens, field workers, department officers, and operators access only the datasets necessary for their civic role.</li>
            <li><strong>Audit Trail Immutability:</strong> All administrative changes, status transitions, SLA escalations, and incident assignments are logged with cryptographic non-repudiation in audit records with zero credential leakage.</li>
            <li><strong>Zero Commercial Monetization:</strong> CivicPulse does not sell, license, or share citizen information with commercial advertising entities or third-party behavioral trackers.</li>
          </ul>
        </section>

        <section style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)' }}>
            <FiCheckCircle size={16} color="#15803d" /> 4. Geographic Information Systems (GIS) Safeguards
          </h2>
          <p style={{ fontSize: '0.9rem', lineHeight: '1.6', color: 'var(--text-secondary)' }}>
            Geographic coordinates recorded for municipal incidents and emergency resources are stored in secure PostGIS spatial tables. Spatial queries are restricted to authenticated operational sessions to ensure field staff and citizen safety during ongoing emergency incidents.
          </p>
        </section>

        <div style={{ marginTop: '2.5rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
          <div>CivicPulse Platform Version 1.0 (Production Release)</div>
          <div>Last Reviewed: Municipal Operations Committee, October 2026</div>
        </div>
      </div>
    </div>
  );
};

export default PrivacyPolicyPage;

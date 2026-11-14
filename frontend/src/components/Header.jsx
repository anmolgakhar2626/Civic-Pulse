import React, { useState, useEffect } from 'react';
import { Activity, ShieldAlert, Radio, Server, CheckCircle2 } from 'lucide-react';

export default function Header({ systemHealth, activeTab, setActiveTab }) {
  const [currentTime, setCurrentTime] = useState(new Date().toLocaleTimeString());

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date().toLocaleTimeString());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const isHealthy = systemHealth?.status === 'UP';

  return (
    <>
      <header className="header">
        <div className="header-brand">
          <span className="brand-badge">GOV-OPS</span>
          <div>
            <div className="brand-title">CivicPulse Command Centre</div>
            <div className="brand-subtitle">Unified Smart City Operations Platform & Rule Engine</div>
          </div>
        </div>

        <div className="header-status">
          <div className={`status-indicator ${isHealthy ? 'status-online' : 'kpi-critical'}`}>
            <span className="status-dot"></span>
            <span>{isHealthy ? 'SYSTEM OPERATIONAL' : 'SYSTEM DEGRADED'}</span>
          </div>
          <div className="clock-display">
            {currentTime} UTC+5:30
          </div>
        </div>
      </header>

      <nav className="nav-tabs">
        <button
          className={`nav-tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          <Activity size={16} /> Overview & Feed
        </button>
        <button
          className={`nav-tab-btn ${activeTab === 'incidents' ? 'active' : ''}`}
          onClick={() => setActiveTab('incidents')}
        >
          <ShieldAlert size={16} /> Incident Management
        </button>
        <button
          className={`nav-tab-btn ${activeTab === 'rule-engine' ? 'active' : ''}`}
          onClick={() => setActiveTab('rule-engine')}
        >
          <Activity size={16} /> Priority Rule Engine
        </button>
        <button
          className={`nav-tab-btn ${activeTab === 'telemetry' ? 'active' : ''}`}
          onClick={() => setActiveTab('telemetry')}
        >
          <Radio size={16} /> IoT Telemetry & APIs
        </button>
        <button
          className={`nav-tab-btn ${activeTab === 'system' ? 'active' : ''}`}
          onClick={() => setActiveTab('system')}
        >
          <Server size={16} /> Infrastructure & Health
        </button>
      </nav>
    </>
  );
}

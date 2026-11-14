import React from 'react';
import { Link } from 'react-router-dom';
import { FiShield, FiActivity, FiMapPin, FiCpu, FiExternalLink, FiGithub } from 'react-icons/fi';
import { LuCloudSun } from 'react-icons/lu';

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="public-footer">
      <div className="footer-top-accent"></div>
      <div className="footer-container">
        {/* Brand & Mission Column */}
        <div className="footer-column footer-brand-column">
          <div className="footer-brand">
            <div className="brand-logo-icon">
              <FiShield size={20} />
            </div>
            <span className="brand-name">Civic<span className="text-emerald">Pulse</span></span>
          </div>
          <p className="footer-desc">
            Enterprise Smart City Command Centre unifying real-time IoT environmental telemetry,
            AI incident prioritization, spatial GIS maps, and automated municipal emergency dispatch.
          </p>
          <div className="footer-jurisdiction">
            <FiMapPin size={14} className="text-emerald" />
            <span>Jurisdiction: Chandigarh Smart City Grid</span>
          </div>
          <div className="footer-status-tag">
            <span className="telemetry-pulse-dot"></span>
            <span>All 6 Municipal Core Microservices Operational</span>
          </div>
        </div>

        {/* Quick Links */}
        <div className="footer-column">
          <h4 className="footer-heading">Platform Navigation</h4>
          <ul className="footer-links">
            <li><Link to="/">Home Overview</Link></li>
            <li><Link to="/about">About CivicPulse</Link></li>
            <li>
              <Link to="/weather" className="footer-weather-link">
                <LuCloudSun size={15} />
                <span>Weather & Air Quality</span>
              </Link>
            </li>
            <li><Link to="/login">Command Centre Login</Link></li>
            <li><Link to="/signup">Citizen Grievance Portal</Link></li>
            <li><Link to="/privacy">Data Governance & Privacy</Link></li>
          </ul>
        </div>

        {/* Operations & Monitoring */}
        <div className="footer-column">
          <h4 className="footer-heading">Municipal Operations</h4>
          <ul className="footer-links">
            <li><Link to="/dashboard">Operational Dashboard</Link></li>
            <li><Link to="/map">Live GIS Spatial Map</Link></li>
            <li><Link to="/incidents">Incident Management & Triage</Link></li>
            <li><Link to="/sensors">IoT Environmental Grid</Link></li>
            <li><Link to="/sla">SLA Escalation Board</Link></li>
            <li><Link to="/analytics">City Workload Analytics</Link></li>
          </ul>
        </div>

        {/* Technology & Standards */}
        <div className="footer-column">
          <h4 className="footer-heading">Technology Standards</h4>
          <p className="footer-tech-text">
            Architected with Spring Boot 3.3, Java 21 LTS, PostgreSQL 16 + PostGIS, Eclipse Mosquitto MQTT,
            Open-Meteo REST APIs, and React 18.
          </p>
          <div className="footer-security-badges">
            <span className="sec-badge">ISO/IEC 27001 Ready</span>
            <span className="sec-badge">BCrypt & JWT RBAC</span>
            <span className="sec-badge">PostGIS Spatial</span>
          </div>
        </div>
      </div>

      <div className="footer-bottom-bar">
        <div className="footer-bottom-container">
          <p className="footer-copy">
            © {currentYear} CivicPulse Command Centre. Smart City Operations Platform.
          </p>
          <div className="footer-bottom-links">
            <Link to="/privacy">Privacy Policy</Link>
            <span className="separator">•</span>
            <Link to="/about#team">Project Authors</Link>
            <span className="separator">•</span>
            <Link to="/weather">Weather Station</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}


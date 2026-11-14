import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  FiShield,
  FiActivity,
  FiMapPin,
  FiArrowRight,
  FiCheckCircle,
  FiCpu,
  FiClock,
  FiBarChart2,
  FiTool,
  FiUsers,
  FiLayers,
  FiRadio,
  FiTruck,
  FiAlertTriangle,
  FiCompass,
  FiGitPullRequest,
  FiSend
} from 'react-icons/fi';

// 5 High-resolution authentic smart-city incident & operations imagery
const BACKGROUND_SCENARIOS = [
  {
    id: 'traffic-cctv',
    imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=2000&q=80',
    caption: 'Arterial Corridor & CCTV Surveillance Grid'
  },
  {
    id: 'incident-room',
    imageUrl: 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=2000&q=80',
    caption: 'Unified Municipal Command & Incident Triage Console'
  },
  {
    id: 'iot-drainage',
    imageUrl: 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=2000&q=80',
    caption: 'IoT Environmental Sensors & Storm Drainage Telemetry'
  },
  {
    id: 'fleet-dispatch',
    imageUrl: 'https://images.unsplash.com/photo-1587740896339-968561b8c41e?auto=format&fit=crop&w=2000&q=80',
    caption: 'Emergency Field Response & SLA-Guided Dispatch'
  },
  {
    id: 'city-grid',
    imageUrl: 'https://images.unsplash.com/photo-1477959858617-67f30bc75b82?auto=format&fit=crop&w=2000&q=80',
    caption: 'Metropolitan Spatial Grid & Incident Density GIS'
  }
];

export default function HomePage() {
  const { isAuthenticated } = useAuth();
  const [currentBgIndex, setCurrentBgIndex] = useState(0);

  // Requirement 1: Restore continuous automatic background image carousel.
  // Changes every 2.5 seconds, loops indefinitely, smooth crossfade, no pause after 1st cycle.
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentBgIndex((prev) => (prev + 1) % BACKGROUND_SCENARIOS.length);
    }, 2500);

    return () => clearInterval(timer);
  }, []);

  // 6 Core Features strictly matching municipal incident & complaint response requirements
  const coreFeatures = [
    {
      id: 'incident-mgmt',
      title: 'Unified Incident Management',
      description: 'Centralized tracking of city incidents and citizen complaints across departments with end-to-end lifecycle governance.',
      icon: <FiAlertTriangle size={22} />,
      link: '/incidents',
      linkText: 'Track Active Incidents',
      badge: 'CORE INCIDENT TRIAGE'
    },
    {
      id: 'iot-alerts',
      title: 'Real-Time IoT Alerts',
      description: 'Monitor sensor alerts and environmental anomalies including drainage overflow, air quality spikes, and infrastructure faults.',
      icon: <FiCpu size={22} />,
      link: '/sensors',
      linkText: 'Inspect IoT Telemetry',
      badge: 'EDGE SENSOR TELEMETRY'
    },
    {
      id: 'sla-tracking',
      title: 'SLA & Severity Tracking',
      description: 'Identify critical incidents, overdue cases and response risks with automated breach calculations and escalation protocols.',
      icon: <FiClock size={22} />,
      link: '/sla',
      linkText: 'Review SLA Adherence',
      badge: 'COMPLIANCE GOVERNANCE'
    },
    {
      id: 'dept-coordination',
      title: 'Departmental Coordination',
      description: 'Assign incidents to responsible departments and track ownership between Traffic, Sanitation, Electrical, and Public Works.',
      icon: <FiUsers size={22} />,
      link: '/dashboard',
      linkText: 'View Department Workload',
      badge: 'INTER-AGENCY WORKFLOW'
    },
    {
      id: 'location-monitoring',
      title: 'Location-Based Monitoring',
      description: 'Understand incident locations and geographical distribution with interactive PostGIS spatial clustering and heatmaps.',
      icon: <FiMapPin size={22} />,
      link: '/map',
      linkText: 'Explore Spatial GIS Map',
      badge: 'GEOGRAPHICAL DISTRIBUTION'
    },
    {
      id: 'response-tracking',
      title: 'Response Status Tracking',
      description: 'Track incidents from reporting to assignment, action and resolution with verifiable proof and immutable audit logs.',
      icon: <FiCheckCircle size={22} />,
      link: '/complaints',
      linkText: 'Verify Resolution Flow',
      badge: 'END-TO-END LIFECYCLE'
    }
  ];

  // Incident & Operational Pillar Visuals
  const operationalPillars = [
    {
      id: 'traffic-mobility',
      title: 'Arterial Traffic & Corridor Incidents',
      subtitle: 'Surveillance of bottlenecks, signals, and transit routes',
      imageUrl: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=900&q=80',
      badge: 'TRAFFIC OPERATIONS',
      icon: <FiRadio size={20} />,
      metrics: [
        { label: 'Active Congestion', value: '4 Corridors' },
        { label: 'Signal Priority', value: 'Green Wave Ready' },
        { label: 'Incident Triage', value: '< 2 Mins' }
      ],
      description: 'Synchronized corridor camera feeds and vehicle flow detectors pinpoint road blockages and dispatch traffic officers instantaneously.',
      link: '/map',
      linkText: 'Inspect Traffic Incidents'
    },
    {
      id: 'cctv-command',
      title: 'Central Command & Incident Triage',
      subtitle: 'Unified operator consoles and cross-agency coordination',
      imageUrl: 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=900&q=80',
      badge: 'OPERATIONS HUB',
      icon: <FiLayers size={20} />,
      metrics: [
        { label: 'Connected Feeds', value: '18 Hubs' },
        { label: 'Event Ingestion', value: 'Real-Time STOMP' },
        { label: 'Audit Compliance', value: '100% Logged' }
      ],
      description: 'Consolidated dashboard enabling command operators to prioritize citizen grievances and telemetry alarms in a single pane of glass.',
      link: '/dashboard',
      linkText: 'Open Command Console'
    },
    {
      id: 'environmental-iot',
      title: 'Environmental & Hazard Telemetry',
      subtitle: 'Stormwater drainage levels, AQI spikes & hazard alerts',
      imageUrl: 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80',
      badge: 'HAZARD MONITORING',
      icon: <FiCpu size={20} />,
      metrics: [
        { label: 'Drainage Status', value: '18% Capacity' },
        { label: 'Air Quality (AQI)', value: 'Good (42)' },
        { label: 'Sensor Network', value: '99.4% Uptime' }
      ],
      description: 'Autonomous sensor alerts flag waterlogging, electrical circuit failure, and air hazards to trigger preventive municipal work orders.',
      link: '/sensors',
      linkText: 'View Sensor Alerts'
    },
    {
      id: 'field-dispatch',
      title: 'Emergency Field Response & Resolution',
      subtitle: 'Work order routing with verifiable resolution logs',
      imageUrl: 'https://images.unsplash.com/photo-1587740896339-968561b8c41e?auto=format&fit=crop&w=900&q=80',
      badge: 'FIELD RESPONSE',
      icon: <FiTruck size={20} />,
      metrics: [
        { label: 'Field Units Active', value: '38 Deployed' },
        { label: 'SLA Adherence', value: '98.4%' },
        { label: 'Resolution Rate', value: '92% On-Schedule' }
      ],
      description: 'Departmental field crews receive prioritized tasks with geolocation, repair documentation requirements, and instant citizen notification.',
      link: '/incidents',
      linkText: 'Inspect Response Units'
    }
  ];

  return (
    <div className="home-page-root">
      {/* ======================================================================
          HERO SECTION (Continuous Background Carousel, Smooth Transitions)
          ====================================================================== */}
      <section className="hero-section">
        {/* Continuous Automatic Crossfade Background Carousel */}
        <div className="hero-realistic-backdrop" aria-hidden="true">
          {BACKGROUND_SCENARIOS.map((scenario, index) => (
            <div
              key={scenario.id}
              className={`hero-backdrop-slide ${index === currentBgIndex ? 'active' : ''}`}
              style={{ backgroundImage: `url(${scenario.imageUrl})` }}
            />
          ))}
          <div className="hero-backdrop-gradient" />
        </div>

        {/* Hero Content */}
        <div className="hero-content-wrapper">
          <div className="hero-badge-pill">
            <span className="telemetry-pulse-dot" />
            <span>MUNICIPAL INCIDENT COMMAND • CHANDIGARH REGION</span>
          </div>

          {/* Heading strictly matching user prompt */}
          <h1 className="hero-main-heading">
            One Platform. <span className="hero-emerald-gradient">Complete City Response.</span>
          </h1>

          {/* Description strictly matching user prompt */}
          <p className="hero-subtitle">
            CivicPulse unifies city incidents, citizen complaints, real-time IoT alerts and departmental actions into one intelligent command centre. Monitor severity, SLA risks, location, ownership and response status through a single operational view.
          </p>

          {/* 3 Buttons strictly matching user prompt */}
          <div className="hero-cta-group">
            <Link
              to={isAuthenticated ? '/dashboard' : '/login'}
              className="btn-hero-primary"
            >
              <span>Launch Command Centre</span>
              <FiArrowRight size={17} />
            </Link>

            <Link to="/signup" className="btn-hero-secondary">
              <FiSend size={16} />
              <span>Report an Issue</span>
            </Link>

            <Link to="/map" className="btn-hero-weather">
              <FiMapPin size={17} />
              <span>View Live Monitoring</span>
            </Link>
          </div>
        </div>
      </section>

      {/* ======================================================================
          OPERATIONAL TELEMETRY TICKER (Incident & Complaint Focused)
          ====================================================================== */}
      <section className="telemetry-ticker-section">
        <div className="ticker-container">
          <div className="ticker-card">
            <div className="ticker-icon bg-emerald-subtle text-emerald">
              <FiAlertTriangle size={20} />
            </div>
            <div className="ticker-data">
              <span className="ticker-value">28</span>
              <span className="ticker-label">Total Reported Incidents</span>
            </div>
          </div>

          <div className="ticker-card">
            <div className="ticker-icon bg-emerald-subtle text-emerald">
              <FiTool size={20} />
            </div>
            <div className="ticker-data">
              <span className="ticker-value">12</span>
              <span className="ticker-label">Open Citizen Complaints</span>
            </div>
          </div>

          <div className="ticker-card">
            <div className="ticker-icon bg-emerald-subtle text-emerald">
              <FiCpu size={20} />
            </div>
            <div className="ticker-data">
              <span className="ticker-value">3</span>
              <span className="ticker-label">Critical IoT Alerts</span>
            </div>
          </div>

          <div className="ticker-card">
            <div className="ticker-icon bg-emerald-subtle text-emerald">
              <FiClock size={20} />
            </div>
            <div className="ticker-data">
              <span className="ticker-value">2 At-Risk</span>
              <span className="ticker-label">SLA Escalation Queue</span>
            </div>
          </div>

          <div className="ticker-card">
            <div className="ticker-icon bg-emerald-subtle text-emerald">
              <FiTruck size={20} />
            </div>
            <div className="ticker-data">
              <span className="ticker-value">38 Units</span>
              <span className="ticker-label">Active Field Response Crews</span>
            </div>
          </div>
        </div>
      </section>

      {/* ======================================================================
          CORE FEATURES (Strictly 6 Mandatory Problem-Statement Capabilities)
          ====================================================================== */}
      <section className="features-preview-section">
        <div className="section-container">
          <div className="section-header-group">
            <span className="section-eyebrow">CENTRALIZED COMMAND CAPABILITIES</span>
            <h2 className="section-heading">Engineered for Municipal Incident Response</h2>
            <p className="section-subtext">
              Eliminate disconnected departmental workflows with end-to-end incident visibility, SLA accountability, and real-time sensor integration.
            </p>
          </div>

          <div className="features-grid">
            {coreFeatures.map((feat) => (
              <div key={feat.id} className="home-feature-card">
                <div className="home-feature-icon-wrapper">
                  {feat.icon}
                </div>
                <span className="text-xs font-semibold text-emerald tracking-wide uppercase mb-1 block">
                  {feat.badge}
                </span>
                <h3 className="home-feature-title">{feat.title}</h3>
                <p className="home-feature-desc">{feat.description}</p>
                <Link to={feat.link} className="feature-card-link">
                  <span>{feat.linkText}</span>
                  <FiArrowRight size={14} />
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ======================================================================
          OPERATIONAL PILLARS (Realistic Smart-City Infrastructure Visuals)
          ====================================================================== */}
      <section className="operational-pillars-section">
        <div className="section-container">
          <div className="section-header-group">
            <span className="section-eyebrow">DEPARTMENTAL COORDINATION</span>
            <h2 className="section-heading">Cross-Agency Incident Governance</h2>
            <p className="section-subtext">
              Real-world systems working in unison across traffic management, environmental hazard monitoring, citizen grievance handling, and field crews.
            </p>
          </div>

          <div className="pillars-grid">
            {operationalPillars.map((pillar) => (
              <div key={pillar.id} className="pillar-card">
                <div className="pillar-image-container">
                  <img
                    src={pillar.imageUrl}
                    alt={pillar.title}
                    className="pillar-image"
                    loading="lazy"
                  />
                  <div className="pillar-image-overlay" />
                  <span className="pillar-badge-tag">{pillar.badge}</span>
                </div>

                <div className="pillar-content">
                  <div className="pillar-header-row">
                    <div className="pillar-icon-box">{pillar.icon}</div>
                    <h3 className="pillar-title">{pillar.title}</h3>
                  </div>

                  <p className="pillar-desc">{pillar.description}</p>

                  <div className="pillar-metrics-row">
                    {pillar.metrics.map((m, mIdx) => (
                      <div key={mIdx} className="pillar-metric-chip">
                        <span className="chip-label">{m.label}</span>
                        <span className="chip-val">{m.value}</span>
                      </div>
                    ))}
                  </div>

                  <div className="pillar-footer">
                    <Link to={pillar.link} className="pillar-action-link">
                      <span>{pillar.linkText}</span>
                      <FiArrowRight size={14} />
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ======================================================================
          HOW IT WORKS (Unified Incident Lifecycle Pipeline)
          ====================================================================== */}
      <section className="how-it-works-section">
        <div className="section-container">
          <div className="section-header-group light">
            <span className="section-eyebrow text-mint">END-TO-END WORKFLOW</span>
            <h2 className="section-heading text-white">How CivicPulse Resolves Incidents</h2>
            <p className="section-subtext text-slate-300">
              From initial citizen reporting or IoT detection to verified departmental resolution.
            </p>
          </div>

          <div className="pipeline-steps-grid">
            <div className="pipeline-step-card">
              <div className="pipeline-step-badge">01</div>
              <h4 className="pipeline-step-title">Ingest &amp; Geocode</h4>
              <p className="pipeline-step-desc">
                Incidents, complaints, and MQTT sensor alerts are ingested with PostGIS coordinates, photos, and automated severity indexing.
              </p>
            </div>

            <div className="pipeline-step-card">
              <div className="pipeline-step-badge">02</div>
              <h4 className="pipeline-step-title">SLA &amp; Department Routing</h4>
              <p className="pipeline-step-desc">
                The rule engine evaluates severity, calculates SLA deadlines, and assigns ownership to Traffic, Sanitation, or Electrical units.
              </p>
            </div>

            <div className="pipeline-step-card">
              <div className="pipeline-step-badge">03</div>
              <h4 className="pipeline-step-title">Field Action &amp; Closure</h4>
              <p className="pipeline-step-desc">
                Dispatched crews resolve issues on-site, upload verification evidence, and close tickets with complete audit traceability.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ======================================================================
          CTA BANNER (Municipal Command Access)
          ====================================================================== */}
      <section className="home-cta-section">
        <div className="section-container">
          <div className="cta-banner-card">
            <div className="cta-banner-content">
              <span className="cta-badge">MUNICIPAL COMMAND ACCESS</span>
              <h2 className="cta-title">Streamline Your Municipal Incident Operations</h2>
              <p className="cta-desc">
                Access the operational portal using an authorized municipal role (Admin, Operator, Traffic Officer, Field Worker)
                or submit a public issue through the citizen grievance portal.
              </p>
              <div className="cta-button-row">
                <Link to="/login" className="btn-cta-primary">
                  <span>Sign In to Command Centre</span>
                  <FiArrowRight size={16} />
                </Link>
                <Link to="/signup" className="btn-cta-secondary">
                  <span>File Citizen Grievance</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

import React from 'react';
import { Link } from 'react-router-dom';
import {
  FiShield,
  FiTarget,
  FiCheckCircle,
  FiActivity,
  FiTool,
  FiUsers,
  FiBarChart2,
  FiLayers,
  FiCpu,
  FiArrowRight,
  FiGithub,
  FiLinkedin,
  FiMail,
  FiCode,
  FiCompass,
  FiServer
} from 'react-icons/fi';
import { LuCloudSun, LuTrafficCone } from 'react-icons/lu';

export default function AboutPage() {
  // Project authors / team members configuration
  const teamMembers = [
    {
      id: 1,
      name: 'Anmol Kumar',
      role: 'Project Lead & Full-Stack Architect',
      initials: 'AK',
      bio: 'Architected and built CivicPulse, unifying real-time IoT sensor telemetry, deterministic rule engines, PostGIS spatial mapping, and Spring Security RBAC.',
      skills: ['Java 21', 'Spring Boot 3.3', 'React 18', 'PostgreSQL / PostGIS', 'MQTT / STOMP', 'Docker'],
      github: 'https://github.com',
      linkedin: 'https://linkedin.com',
      email: 'anmol@civicpulse.smartcity.gov'
    },
    {
      id: 2,
      name: 'CivicPulse Engineering Team',
      role: 'Municipal Systems & Edge Telemetry Group',
      initials: 'CP',
      bio: 'Focused on resilient edge sensor ingestion, Open-Meteo environmental adapters, SLA escalation timers, and high-performance React visualization components.',
      skills: ['Distributed Systems', 'IoT Edge Telemetry', 'Spatial Analysis', 'REST & WebSockets', 'Automated Testing'],
      github: 'https://github.com',
      linkedin: 'https://linkedin.com',
      email: 'engineering@civicpulse.smartcity.gov'
    }
  ];

  return (
    <div className="about-page-root">
      {/* ======================================================================
          HERO BANNER
          ====================================================================== */}
      <section className="about-hero-section">
        <div className="section-container">
          <div className="about-hero-content">
            <span className="section-eyebrow text-mint">MUNICIPAL COMMAND CENTRE SPECIFICATION</span>
            <h1 className="about-hero-title">
              About <span className="hero-emerald-gradient">CivicPulse</span>
            </h1>
            <p className="about-hero-subtitle">
              CivicPulse is an enterprise-grade Smart City Command Centre engineered to bridge
              urban infrastructure, environmental telemetry, and municipal emergency dispatch through technology.
            </p>
          </div>
        </div>
      </section>

      {/* ======================================================================
          A. ABOUT CIVICPULSE & B. OUR MISSION
          ====================================================================== */}
      <section className="about-mission-section">
        <div className="section-container">
          <div className="mission-grid">
            <div className="mission-card about-card">
              <div className="card-badge-icon bg-emerald-subtle text-emerald">
                <FiShield size={24} />
              </div>
              <h2 className="mission-card-title">What is CivicPulse?</h2>
              <p className="mission-card-text">
                CivicPulse serves as the centralized brain for municipal governance. In modern cities,
                critical events originate from multiple fragmented sources: roadside IoT sensors, traffic cameras,
                weather radars, citizen complaint portals, and field workers.
              </p>
              <p className="mission-card-text">
                CivicPulse breaks down departmental silos by aggregating all urban signals into a single
                high-availability operational console, applying deterministic rules to prioritize incidents,
                and orchestrating rapid resource response.
              </p>
            </div>

            <div className="mission-card about-card">
              <div className="card-badge-icon bg-emerald-subtle text-emerald">
                <FiTarget size={24} />
              </div>
              <h2 className="mission-card-title">Our Core Mission</h2>
              <p className="mission-card-text">
                Our mission is to make urban centers <strong>smarter, safer, cleaner, and drastically more efficient</strong>.
                By converting passive data streams into proactive municipal actions, CivicPulse empowers city authorities
                to solve infrastructure disruptions before they escalate into civic crises.
              </p>
              <ul className="mission-bullet-list">
                <li><FiCheckCircle className="bullet-icon text-emerald" /> <span><strong>Smarter:</strong> Rule-based incident prioritization without human bias.</span></li>
                <li><FiCheckCircle className="bullet-icon text-emerald" /> <span><strong>Safer:</strong> Sub-minute emergency dispatch and proactive hazard alerts.</span></li>
                <li><FiCheckCircle className="bullet-icon text-emerald" /> <span><strong>Cleaner:</strong> Real-time air quality index and smart waste overflow monitors.</span></li>
                <li><FiCheckCircle className="bullet-icon text-emerald" /> <span><strong>More Efficient:</strong> Strict SLA timers and automated escalation tiers.</span></li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* ======================================================================
          C. OUR KEY FEATURES
          ====================================================================== */}
      <section className="about-features-section">
        <div className="section-container">
          <div className="section-header-group">
            <span className="section-eyebrow">SYSTEM CAPABILITIES</span>
            <h2 className="section-heading">Key Operational Features</h2>
            <p className="section-subtext">
              Engineered with six dedicated sub-systems built for 24/7 municipal uptime.
            </p>
          </div>

          <div className="about-features-grid">
            {/* 1. Smart Traffic Monitoring */}
            <div className="about-feature-box">
              <div className="feature-box-icon text-emerald">
                <LuTrafficCone size={28} />
              </div>
              <h3 className="feature-box-title">Smart Traffic Monitoring</h3>
              <p className="feature-box-desc">
                Continuous ingestion of road corridor sensors and intersection signals. Provides dynamic arterial
                flow analysis, congestion zone warnings, and automated emergency green-wave routing for first responders.
              </p>
              <div className="feature-box-pills">
                <span>Signal Optimization</span>
                <span>Arterial Flow</span>
                <span>Corridor Routing</span>
              </div>
            </div>

            {/* 2. Weather Monitoring */}
            <div className="about-feature-box">
              <div className="feature-box-icon text-emerald">
                <LuCloudSun size={28} />
              </div>
              <h3 className="feature-box-title">Weather & Environmental Monitoring</h3>
              <p className="feature-box-desc">
                Direct integration with Open-Meteo REST APIs for temperature, humidity, wind gusts, and precipitation.
                Continuously tracks PM2.5, PM10, and European AQI to safeguard citizen respiratory health.
              </p>
              <div className="feature-box-pills">
                <span>Open-Meteo REST</span>
                <span>AQI Telemetry</span>
                <span>Flood Alerts</span>
              </div>
            </div>

            {/* 3. Public Infrastructure Management */}
            <div className="about-feature-box">
              <div className="feature-box-icon text-emerald">
                <FiTool size={28} />
              </div>
              <h3 className="feature-box-title">Public Infrastructure Management</h3>
              <p className="feature-box-desc">
                Supervisory tracking of city assets including storm drainage gates, street lighting circuits,
                smart waste bins with ultrasonic fill sensors, and municipal electrical transformer health.
              </p>
              <div className="feature-box-pills">
                <span>Drainage Telemetry</span>
                <span>Smart Waste</span>
                <span>Power Nodes</span>
              </div>
            </div>

            {/* 4. Citizen Services */}
            <div className="about-feature-box">
              <div className="feature-box-icon text-emerald">
                <FiUsers size={28} />
              </div>
              <h3 className="feature-box-title">Citizen Services & Grievances</h3>
              <p className="feature-box-desc">
                Intuitive grievance reporting portal allowing residents to submit location coordinates, category,
                description, and photo proof. Features real-time SMS OTP two-factor verification and status tracking.
              </p>
              <div className="feature-box-pills">
                <span>Public Portal</span>
                <span>SMS 2FA</span>
                <span>Live Ticket SLA</span>
              </div>
            </div>

            {/* 5. Real-Time City Telemetry */}
            <div className="about-feature-box">
              <div className="feature-box-icon text-emerald">
                <FiActivity size={28} />
              </div>
              <h3 className="feature-box-title">Real-Time City Telemetry</h3>
              <p className="feature-box-desc">
                High-throughput MQTT broker (Mosquitto) handles sub-second sensor streams. STOMP WebSockets
                broadcast threshold crossings instantly to all active operator terminals with zero page reloads.
              </p>
              <div className="feature-box-pills">
                <span>Eclipse Mosquitto</span>
                <span>STOMP WebSockets</span>
                <span>Threshold Engine</span>
              </div>
            </div>

            {/* 6. Smart City Analytics */}
            <div className="about-feature-box">
              <div className="feature-box-icon text-emerald">
                <FiBarChart2 size={28} />
              </div>
              <h3 className="feature-box-title">Smart City Analytics & Intelligence</h3>
              <p className="feature-box-desc">
                Relational and time-series aggregation calculating departmental resolution times, incident density maps,
                SLA compliance rates, and historical seasonal trend comparisons with injection-safe CSV export.
              </p>
              <div className="feature-box-pills">
                <span>PostgreSQL Metrics</span>
                <span>SLA Compliance</span>
                <span>Sanitized CSV Export</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ======================================================================
          D. HOW IT WORKS
          ====================================================================== */}
      <section className="about-how-it-works-section">
        <div className="section-container">
          <div className="section-header-group">
            <span className="section-eyebrow">OPERATIONAL WORKFLOW</span>
            <h2 className="section-heading">How Data, Monitoring & City Management Work Together</h2>
            <p className="section-subtext">
              A transparent, closed-loop pipeline ensuring every municipal anomaly is verified, dispatched, and resolved.
            </p>
          </div>

          <div className="workflow-steps-container">
            <div className="workflow-step-item">
              <div className="step-number-badge">1</div>
              <div className="step-content-box">
                <h4 className="step-title">Multi-Source Ingestion</h4>
                <p className="step-desc">
                  IoT edge sensors publish environmental and infrastructure readings over MQTT. Concurrently, citizens
                  report physical road hazards and grievances via the web portal. All inputs enter a unified validation pipeline.
                </p>
              </div>
            </div>

            <div className="workflow-step-item">
              <div className="step-number-badge">2</div>
              <div className="step-content-box">
                <h4 className="step-title">Deterministic Rule Evaluation & Spatial Mapping</h4>
                <p className="step-desc">
                  Incoming events are plotted on the PostGIS GIS grid. The Priority Rule Engine scores the event based
                  on severity, hazard radius, infrastructure criticality, and population exposure to assign a priority tier (Critical, High, Medium, Low).
                </p>
              </div>
            </div>

            <div className="workflow-step-item">
              <div className="step-number-badge">3</div>
              <div className="step-content-box">
                <h4 className="step-title">Automated Department Assignment & SLA Timers</h4>
                <p className="step-desc">
                  The auto-assignment engine routes the work order to the responsible department (Traffic, Drainage, Sanitation, Power).
                  A countdown timer starts based on the configured SLA policy (e.g. 30m response / 120m resolution for Critical incidents).
                </p>
              </div>
            </div>

            <div className="workflow-step-item">
              <div className="step-number-badge">4</div>
              <div className="step-content-box">
                <h4 className="step-title">Field Execution, Verification & Immutable Audit</h4>
                <p className="step-desc">
                  Assigned field crews execute on-site work orders and log completion status. The system verifies resolution,
                  captures actual response duration, stops the SLA timer, and creates an immutable audit trail entry with secret sanitization.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ======================================================================
          E. PROJECT OBJECTIVES
          ====================================================================== */}
      <section className="about-objectives-section">
        <div className="section-container">
          <div className="section-header-group">
            <span className="section-eyebrow">STRATEGIC GOALS</span>
            <h2 className="section-heading">Project Objectives & Civic Benefits</h2>
            <p className="section-subtext">
              Transforming traditional municipal bureaucracies into responsive, data-driven smart cities.
            </p>
          </div>

          <div className="objectives-grid">
            <div className="objective-card">
              <span className="objective-number">01</span>
              <h3 className="objective-title">Eliminate Information Fragmentation</h3>
              <p className="objective-desc">
                Provide unified municipal command so operators never have to toggle between disparate legacy systems to assess city health.
              </p>
            </div>

            <div className="objective-card">
              <span className="objective-number">02</span>
              <h3 className="objective-title">Objective, Rule-Based Triage</h3>
              <p className="objective-desc">
                Replace manual guesswork with transparent, documented mathematical weights that guarantee urgent incidents receive immediate priority.
              </p>
            </div>

            <div className="objective-card">
              <span className="objective-number">03</span>
              <h3 className="objective-title">Rigorous SLA Accountability</h3>
              <p className="objective-desc">
                Enforce contractual response standards with automated multi-tier escalation warnings before breach deadlines elapse.
              </p>
            </div>

            <div className="objective-card">
              <span className="objective-number">04</span>
              <h3 className="objective-title">Empower Citizens with Transparency</h3>
              <p className="objective-desc">
                Give residents complete visibility into municipal grievance resolution timelines, fostering civic trust and active participation.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ======================================================================
          F. SEPARATE AUTHOR / TEAM MEMBERS SECTION (Individual Profile Cards)
          ====================================================================== */}
      <section className="about-team-section" id="team">
        <div className="section-container">
          <div className="section-header-group">
            <span className="section-eyebrow">PROJECT CONTRIBUTORS</span>
            <h2 className="section-heading">Project Authors & Team Members</h2>
            <p className="section-subtext">
              Dedicated architects and developers behind the CivicPulse Smart City platform.
            </p>
          </div>

          <div className="team-cards-grid">
            {teamMembers.map((member) => (
              <div key={member.id} className="author-profile-card">
                <div className="author-avatar-wrapper">
                  <div className="author-avatar-circle">
                    <span>{member.initials}</span>
                  </div>
                  <div className="author-status-indicator" title="Active Project Contributor"></div>
                </div>

                <div className="author-details">
                  <h3 className="author-name">{member.name}</h3>
                  <div className="author-role-badge">{member.role}</div>

                  <p className="author-bio">{member.bio}</p>

                  <div className="author-skills-section">
                    <span className="skills-heading">Technical Focus:</span>
                    <div className="skills-badge-list">
                      {member.skills.map((skill, sIdx) => (
                        <span key={sIdx} className="author-skill-pill">{skill}</span>
                      ))}
                    </div>
                  </div>

                  <div className="author-social-links">
                    {member.github && (
                      <a
                        href={member.github}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="author-social-btn"
                        title="GitHub Profile"
                      >
                        <FiGithub size={16} />
                        <span>GitHub</span>
                      </a>
                    )}
                    {member.linkedin && (
                      <a
                        href={member.linkedin}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="author-social-btn"
                        title="LinkedIn Profile"
                      >
                        <FiLinkedin size={16} />
                        <span>LinkedIn</span>
                      </a>
                    )}
                    {member.email && (
                      <a
                        href={`mailto:${member.email}`}
                        className="author-social-btn"
                        title="Email Contact"
                      >
                        <FiMail size={16} />
                        <span>Contact</span>
                      </a>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ======================================================================
          CALL TO ACTION
          ====================================================================== */}
      <section className="about-cta-footer">
        <div className="section-container">
          <div className="about-cta-card">
            <h3 className="about-cta-title">Explore the Command Centre in Real Time</h3>
            <p className="about-cta-text">
              Log in to the operational portal to view live GIS spatial incidents, test rule evaluation, or monitor IoT sensors.
            </p>
            <div className="about-cta-actions">
              <Link to="/login" className="btn-hero-primary">
                <span>Sign In to Command Centre</span>
                <FiArrowRight size={16} />
              </Link>
              <Link to="/weather" className="btn-hero-weather">
                <LuCloudSun size={18} />
                <span>Open Weather Station</span>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}


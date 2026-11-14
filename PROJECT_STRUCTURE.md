# CivicPulse Smart City Command Centre — Project Structure & File Guide

This document describes the directory structure, file organization, and architectural layout of the CivicPulse codebase.

---

## Root Directory (`/`)

```text
CivicPulse/
├── backend/                  # Spring Boot 3.3.4 (Java 21) backend application
├── frontend/                 # React 18 + Vite 5 frontend application
├── database/                 # Schema definitions, PostGIS queries, safe seeds & docs
├── docs/                     # Architectural, workflow, API, and deployment documentation
├── mosquitto/                # Eclipse Mosquitto MQTT broker configuration
├── docker-compose.yml        # Multi-container orchestration (6 services)
├── README.md                 # Primary project overview, setup, and execution manual
├── PROJECT_STRUCTURE.md      # This file
├── PRD.md                    # Product Requirements Document
├── .gitignore                # Source control exclusion rules
└── .env.example              # Safe configuration template with placeholders
```

---

## 1. Backend Architecture (`backend/`)

```text
backend/
├── pom.xml                               # Maven project dependencies and build configuration
├── Dockerfile                            # Multi-stage Eclipse Temurin JRE container build
├── src/
│   ├── main/
│   │   ├── java/com/smartcity/
│   │   │   ├── CivicPulseApplication.java # Spring Boot application entry point
│   │   │   │
│   │   │   ├── airquality/               # Open-Meteo Air Quality ingestion and models
│   │   │   │   ├── AirQualityClient.java
│   │   │   │   ├── AirQualityController.java
│   │   │   │   ├── AirQualityReading.java
│   │   │   │   └── AirQualityService.java
│   │   │   │
│   │   │   ├── analytics/                # Operational analytics and KPI aggregations
│   │   │   │   ├── AnalyticsController.java
│   │   │   │   ├── AnalyticsService.java
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── assignment/               # Automated response planning and SOP actions
│   │   │   │   ├── AssignmentEngine.java
│   │   │   │   └── ResponseActionService.java
│   │   │   │
│   │   │   ├── audit/                    # Immutable security audit log with secret redaction
│   │   │   │   ├── AuditLogController.java
│   │   │   │   ├── AuditLogService.java
│   │   │   │   └── AuditLog.java
│   │   │   │
│   │   │   ├── common/                   # Shared response wrappers and exceptions
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── PagedResponse.java
│   │   │   │
│   │   │   ├── complaint/                # Core Grievance Redressal Module
│   │   │   │   ├── ComplaintController.java     # REST API endpoints (file intake, triage, filters)
│   │   │   │   ├── ComplaintService.java        # Grievance service interface
│   │   │   │   ├── ComplaintServiceImpl.java    # Lifecycle transitions, reputation engine, SLA check
│   │   │   │   ├── ComplaintDao.java            # Data access contract with overloaded queries
│   │   │   │   ├── ComplaintJdbcRepository.java # Dynamic SQL query builder & PostGIS mapping
│   │   │   │   ├── ComplaintAttachment.java     # Evidence photo entity
│   │   │   │   ├── ComplaintReceipt.java        # Digital acknowledgement receipt entity
│   │   │   │   ├── FileStorageService.java      # Disk persistence for citizen uploads
│   │   │   │   └── dto/                         # Complaint request and response DTOs
│   │   │   │
│   │   │   ├── config/                   # Spring configuration beans
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── RestClientConfig.java
│   │   │   │   └── SwaggerConfig.java
│   │   │   │
│   │   │   ├── gis/                      # PostGIS spatial analysis and GeoJSON builders
│   │   │   │   ├── GisController.java
│   │   │   │   └── GisService.java
│   │   │   │
│   │   │   ├── grievance/portal/         # CP-GRAMS national portal integration sandbox
│   │   │   │   ├── GovernmentGrievanceController.java
│   │   │   │   └── GovernmentGrievanceService.java
│   │   │   │
│   │   │   ├── incident/                 # Municipal emergency incident management
│   │   │   │   ├── IncidentController.java
│   │   │   │   ├── IncidentService.java
│   │   │   │   ├── Incident.java
│   │   │   │   └── IncidentRepository.java
│   │   │   │
│   │   │   ├── iot/                      # MQTT telemetry ingestion and anomaly checks
│   │   │   │   ├── MqttSubscriber.java
│   │   │   │   ├── SensorThresholdEvaluator.java
│   │   │   │   └── IotEvent.java
│   │   │   │
│   │   │   ├── notification/             # Multi-channel notification dispatch
│   │   │   │   ├── NotificationController.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   └── Notification.java
│   │   │   │
│   │   │   ├── priority/                 # Multi-factor Priority Rule Engine
│   │   │   │   ├── PriorityRuleEngine.java
│   │   │   │   └── PriorityRule.java
│   │   │   │
│   │   │   ├── security/                 # Spring Security 6, JWT, and OTP authentication
│   │   │   │   ├── AuthController.java          # Login, Register, OTP endpoints
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── AuthServiceImpl.java         # Citizen OTP login and session creation
│   │   │   │   ├── OtpController.java           # OTP generation and mobile verification
│   │   │   │   ├── OtpService.java
│   │   │   │   ├── OtpServiceImpl.java          # SHA-256 OTP hashing and cooldown rules
│   │   │   │   ├── MockDevSmsGatewayProvider.java # Local demo OTP handler
│   │   │   │   ├── TwilioSmsGatewayProvider.java  # Real Twilio SMS client
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── SecurityDataInitializer.java # Demo account seeder
│   │   │   │
│   │   │   ├── sla/                      # SLA Management and Background Escalation Daemon
│   │   │   │   ├── SLAManager.java              # 30-second periodic SLA evaluation scheduler
│   │   │   │   ├── SLAService.java
│   │   │   │   ├── SLAServiceImpl.java          # Dynamic SLA calculation and escalation tiers
│   │   │   │   ├── SlaController.java           # Overview, active, breached, and policy APIs
│   │   │   │   ├── SlaJdbcRepository.java       # SLA record database persistence
│   │   │   │   └── SLAPolicy.java
│   │   │   │
│   │   │   ├── user/                     # Users, Roles, Departments, and Teams
│   │   │   │   ├── UserController.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── User.java
│   │   │   │   ├── Role.java
│   │   │   │   ├── Department.java
│   │   │   │   └── Team.java
│   │   │   │
│   │   │   ├── weather/                  # Open-Meteo Weather integration and jobs
│   │   │   │   ├── WeatherClient.java
│   │   │   │   ├── WeatherController.java
│   │   │   │   └── WeatherService.java
│   │   │   │
│   │   │   └── websocket/                # STOMP WebSocket broker and event publishers
│   │   │       ├── WebSocketConfig.java
│   │   │       └── WebSocketEventPublisher.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml           # Spring Boot configuration profiles (dev, prod)
│   │       └── db/migration/             # 16 Flyway database migration scripts (V1 to V16)
│   │
│   └── test/                             # JUnit 5 and Mockito automated test suites
```

---

## 2. Frontend Architecture (`frontend/`)

```text
frontend/
├── package.json                          # NPM dependencies (React 18, Vite, Leaflet, Axios, StompJS)
├── package-lock.json                     # Deterministic dependency lockfile
├── vite.config.js                        # Vite build configuration with /api and /ws proxies
├── index.html                            # HTML5 entry template
├── nginx.conf                            # Production Nginx reverse proxy configuration
├── Dockerfile                            # Multi-stage production Nginx container build
├── src/
│   ├── main.jsx                          # React 18 DOM mount point
│   ├── App.jsx                           # Application routing and role-based route guards
│   ├── index.css                         # Municipal Command Centre pure CSS design system
│   │
│   ├── api/                              # Modular Axios HTTP clients
│   │   ├── client.js                     # Base Axios instance with Bearer token interceptor
│   │   ├── authApi.js                    # Login, Register, OTP APIs
│   │   ├── complaintsApi.js              # Grievance CRUD, assignments, receipts, filters
│   │   ├── incidentsApi.js               # Emergency incident APIs
│   │   ├── slaApi.js                     # SLA overview, active, breached, policies APIs
│   │   ├── usersApi.js                   # Administrative user and RBAC management
│   │   └── analyticsApi.js               # Performance metric APIs
│   │
│   ├── context/                          # Global React Context providers
│   │   ├── AuthContext.jsx               # JWT token storage, user session, hasRole helpers
│   │   └── WebSocketContext.jsx          # Live STOMP WebSocket event listener (6 topics)
│   │
│   ├── layouts/
│   │   └── CommandCentreLayout.jsx       # Persistent municipal sidebar, telemetry topbar
│   │
│   ├── pages/                            # Full-page route views
│   │   ├── LoginPage.jsx                 # Municipal single sign-on & Citizen OTP login
│   │   ├── DashboardPage.jsx             # Role-specific dashboard router
│   │   ├── ComplaintsPage.jsx            # Citizen grievance register with priority/SLA filters
│   │   ├── SlaBoardPage.jsx              # Real-time SLA compliance & escalation kanban
│   │   ├── IncidentsPage.jsx             # Emergency incident triage board
│   │   ├── IncidentDetailPage.jsx       # Incident lifecycle, responder SOPs, actions
│   │   ├── LiveMapPage.jsx               # Full-screen GIS Leaflet interactive map
│   │   ├── SensorsPage.jsx               # Real-time IoT telemetry grid & simulator
│   │   ├── AnalyticsPage.jsx             # Central performance charts and workload trends
│   │   ├── SettingsPage.jsx              # System health diagnostics & user management
│   │   └── PrivacyPolicyPage.jsx         # Municipal data compliance disclosure
│   │
│   └── components/                       # Reusable UI widgets and modals
│       ├── KpiCard.jsx                   # Interactive metric card supporting click-to-filter
│       ├── LoadingSpinner.jsx            # Standard municipal loading indicator
│       ├── PriorityBadge.jsx             # Color-coded CRITICAL, HIGH, MEDIUM, LOW badge
│       ├── SlaTimer.jsx                  # Overdue countdown and target badge
│       ├── EmptyState.jsx                # Friendly fallback view for empty lists
│       ├── ComplaintDetailsModal.jsx     # Comprehensive grievance inspector & assignment dialog
│       ├── ComplaintReceiptModal.jsx     # Official digital acknowledgement receipt viewer
│       ├── UnifiedCitizenComplaintModal.jsx # Citizen grievance submission form with photo intake
│       └── dashboards/
│           ├── AdminDashboardView.jsx    # Command Console with Department Matrix & Quick Pills
│           ├── DepartmentOfficerDashboardView.jsx # Nodal officer redressal queue
│           ├── FieldWorkerDashboardView.jsx       # Field worker mobile dispatch board
│           └── CitizenDashboardView.jsx           # Citizen tracking view with Verified Badge
```

---

## 3. Database Assets (`database/`)

```text
database/
├── DATABASE_SCHEMA.md                    # Detailed schema reference for all 34 tables & Mongo collections
├── schema.sql                            # Clean PostgreSQL DDL schema export
├── seeds.sql                             # Standard safe seed data (roles, departments, demo users)
└── postgis_queries.sql                   # Geospatial PostGIS query reference (radius, nearest, GeoJSON)
```

---

## 4. Documentation Directory (`docs/`)

```text
docs/
├── ARCHITECTURE.md                       # Comprehensive architecture diagrams and layer definitions
├── WORKFLOW.md                           # Sequence and state diagrams for complaints and SLA
├── API_DOCUMENTATION.md                  # Complete REST API reference with endpoints and payloads
└── DEPLOYMENT_GUIDE.md                   # Step-by-step setup, Docker Compose, and execution manual
```

---

## 5. MQTT Configuration (`mosquitto/`)

```text
mosquitto/
└── config/
    └── mosquitto.conf                    # Mosquitto 2.0 configuration with anonymous access for local IoT
```

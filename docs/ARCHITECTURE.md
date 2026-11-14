# CivicPulse Smart City Command Centre — System Architecture

## 1. High-Level Architecture Diagram

```text
                                  [ CITIZEN & STAFF CLIENTS ]
                                               │
                        ┌──────────────────────┴──────────────────────┐
                        ▼                                             ▼
             Web Browser (Desktop / Mobile)                 Command Centre Video Wall
            React 18 + Vite + Leaflet GIS                 React 18 Analytics & Live Map
                        │                                             │
                        └──────────────────────┬──────────────────────┘
                                               │ HTTP REST (JSON / Multipart)
                                               │ WebSocket STOMP (SockJS)
                                               ▼
             ┌─────────────────────────────────────────────────────────────────┐
             │            NGINX REVERSE PROXY & GATEWAY (Port 80)              │
             │           - Static Asset Caching                                │
             │           - API Gateway Reverse Proxy (/api -> :8080)           │
             │           - WebSocket Upgrade Handlers (/ws -> :8080)           │
             └─────────────────────────────────┬───────────────────────────────┘
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                       SPRING BOOT 3.3.4 COMMAND CENTRE BACKEND                              │
│                                                                                             │
│  ┌───────────────────────┐  ┌────────────────────────┐  ┌────────────────────────────────┐  │
│  │ Security & Auth       │  │ Grievance Redressal    │  │ Incident & SLA Engine          │  │
│  │ - JWT Filter (HMAC)   │  │ - Complaint Controller │  │ - Priority Rule Engine         │  │
│  │ - BCrypt Auth Provider│  │ - Multipart File Store │  │ - Background SLA Daemon (30s)  │  │
│  │ - Mobile OTP Service  │  │ - Assignee Fallback    │  │ - Auto-Escalation (L1, L2, L3) │  │
│  │ - RBAC Access Control │  │ - Status History Audit │  │ - Standard Operating Actions   │  │
│  └───────────────────────┘  └────────────────────────┘  └────────────────────────────────┘  │
│  ┌───────────────────────┐  ┌────────────────────────┐  ┌────────────────────────────────┐  │
│  │ IoT Telemetry Engine  │  │ GIS Spatial Engine     │  │ Real-Time Broadcast            │  │
│  │ - MQTT Client (Paho)  │  │ - PostGIS Geometry     │  │ - WebSocket Event Publisher    │  │
│  │ - Sensor Thresholds   │  │ - Radius Buffer Search │  │ - STOMP In-Memory Broker       │  │
│  │ - Anomaly Detection   │  │ - GeoJSON Feature Agg  │  │ - Multi-Channel Notifications  │  │
│  └───────────────────────┘  └────────────────────────┘  └────────────────────────────────┘  │
└──────────────┬───────────────────────────────┬───────────────────────────────┬──────────────┘
               │ JDBC / Hibernate              │ Spring Data Mongo             │ Spring Redis
               ▼                               ▼                               ▼
┌───────────────────────────────┐ ┌──────────────────────────────┐ ┌───────────────────────────┐
│     PostgreSQL 16 + PostGIS   │ │          MongoDB 7.0         │ │          Redis 7.2        │
│ - 34 Relational / GIS Tables  │ │ - 8 Time-Series Collections  │ │ - Session Cache           │
│ - Complaint Lifecycle Records │ │ - High-Frequency IoT Readings│ │ - API Rate Limiting Cache │
│ - User Accounts & Roles       │ │ - Environmental PM2.5 / PM10 │ │ - Live Metric Aggregations│
│ - SLA Policies & Escalations  │ │ - Weather Telemetry Caches   │ │ - Operational Temp States │
│ - City Assets & Spatial Index │ │ - Raw MQTT Anomaly Logs      │ │                           │
└───────────────────────────────┘ └──────────────────────────────┘ └───────────────────────────┘
               ▲
               │ MQTT Telemetry Ingestion (tcp://mosquitto:1883)
┌──────────────┴────────────────────────────────────────────────┐
│               ECLIPSE MOSQUITTO 2.0 MQTT BROKER               │
│ - Subscribes to: `city/sensors/#`, `city/traffic/#`, `city/bin` │
│ - Ingests: Ultrasonic Bins, Water Pressure, Air Monitors      │
└───────────────────────────────────────────────────────────────┘
```

---

## 2. Layered Component Responsibilities

### 2.1 Presentation Tier (Frontend)
- **Single Page Application (SPA)**: Built with React 18 and Vite 5 for instant Hot Module Replacement (HMR) and optimized minified production bundles.
- **Routing & RBAC Protection**: React Router v6 enforcing role guards across:
  - `ADMIN`: Full command console, department matrix, user RBAC management, diagnostics.
  - `COMMAND_OPERATOR`: Real-time incident triage, live GIS map, IoT grid, manual dispatch.
  - `DEPARTMENT_OFFICER`: Nodal departmental work queue, evidence review, task assignment, verification.
  - `FIELD_WORKER`: My assignments queue, progress updates, resolution proof upload.
  - `CITIZEN`: Grievance submission form, personal tracking, digital receipt downloads, Verified Citizen badge.
- **GIS Cartography**: Leaflet and OpenStreetMap rendering custom SVG map markers for incidents, infrastructure, and sensor clusters.
- **Real-Time Client**: StompJS over SockJS maintaining persistent bi-directional communication with automatic exponential-backoff reconnect.

### 2.2 Application Tier (Backend)
- **Spring Boot 3.3.4 (Java 21 LTS)**: Enterprise Java application configured with stateless REST controllers and asynchronous event listeners.
- **Spring Security 6 & JWT**:
  - Stateless authentication using HMAC-SHA384 tokens.
  - BCrypt password hashing ($2a$10$).
  - Granular `@PreAuthorize("hasRole('...')")` on sensitive mutations.
- **Automated SLA Escalation Daemon**:
  - Scheduled background worker (`SLAManager.java`) executing every 30 seconds.
  - Evaluates both incident SLA records and complaint SLA deadlines against `CURRENT_TIMESTAMP`.
  - Atomically transitions overdue items to `BREACHED`, writes audit trail entries, and pushes real-time WebSocket notifications.
- **Grievance Intake & File Storage Service**:
  - Multi-part photo intake with filename hashing, path traversal guards, and automatic directory creation under `backend/uploads/complaints/`.
  - Content-type validation accepting JPEG, PNG, and PDF evidence documents.

### 2.3 Persistence Tier (Polyglot)
- **PostgreSQL 16 + PostGIS 3.4**:
  - Strict ACID guarantees for civic transactions.
  - GiST indexing for O(log N) spatial nearest-neighbor and boundary calculations.
  - Flyway migration control ensuring versioned, reproducible schemas across environments.
- **MongoDB 7.0**:
  - Document storage optimized for high write throughput of IoT sensor streams.
  - TTL (Time-To-Live) indexing for automatic telemetry data retention.
- **Redis 7.2**:
  - Key-value in-memory caching reducing relational query load for dashboard KPIs.
  - Real-time token revocation and OTP verification rate limiting.

---

## 3. Real-Time WebSocket & STOMP Topology

Clients establish a SockJS WebSocket connection to `/ws` and subscribe to municipal topics:

| STOMP Topic | Publishing Trigger | Payload Contents | Target Consumers |
|---|---|---|---|
| `/topic/complaints` | Grievance created, status changed, worker assigned | Full `ComplaintResponse` DTO | Admin, Officers, Citizen Owner |
| `/topic/incidents` | Incident raised, prioritized, dispatched | Incident record, severity tier | Operators, Command Center |
| `/topic/sla` | 30s SLA evaluation, deadline breach, escalation | Incident ID, SLA status, remaining minutes, escalation level | Department Officers, Admins |
| `/topic/sensors` | MQTT reading breaches threshold | Sensor ID, reading value, anomaly type | IoT Monitoring Grid |
| `/topic/alerts` | Critical urban hazard detected | Severity, alert message, coordinates | All active personnel |
| `/topic/notifications` | Direct notification created | Recipient ID, title, message, link | User Notification Flyout |

---

## 4. Role-Based Access Control (RBAC) Matrix

| Feature / Endpoint | ADMIN | COMMAND_OPERATOR | DEPARTMENT_OFFICER | FIELD_WORKER | CITIZEN |
|---|:---:|:---:|:---:|:---:|:---:|
| Lodge Citizen Grievance | ❌ | ❌ | ❌ | ❌ | ✅ |
| View Own Citizen Grievances | ❌ | ❌ | ❌ | ❌ | ✅ |
| View All Complaints (City-wide) | ✅ | ✅ | ❌ (Own Dept Only) | ❌ (Assigned Only) | ❌ |
| Assign Field Worker | ✅ | ✅ | ✅ | ❌ | ❌ |
| Update Progress & Upload Proof | ✅ | ✅ | ✅ | ✅ | ❌ |
| Verify Resolution & Close Grievance | ✅ | ✅ | ✅ | ❌ | ❌ |
| Verify Citizen Report (Genuine/Spam) | ✅ | ❌ | ✅ | ❌ | ❌ |
| View Admin Command Matrix & RBAC | ✅ | ❌ | ❌ | ❌ | ❌ |
| Manage SLA Policies | ✅ | ❌ | ❌ | ❌ | ❌ |
| Access SLA Escalation Board | ✅ | ✅ | ✅ | ❌ | ❌ |
| Emergency Dispatch & Response Plans | ✅ | ✅ | ❌ | ❌ | ❌ |
| IoT Telemetry & Sensor Calibration | ✅ | ✅ | ❌ | ❌ | ❌ |

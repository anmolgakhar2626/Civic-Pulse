# CivicPulse — Smart City Command Centre (Production Release)

> **CivicPulse** is an enterprise-grade municipal command centre platform unifying urban governance, citizen complaint services, smart mobility, environmental telemetry, waste management, emergency dispatch, and automated rule-based incident prioritization. Built strictly in compliance with municipal operations standards.

---

## 1. Architectural Overview

```text
               +-------------------------------------------------------+
               |            React 18 Frontend (Vite + CSS)             |
               |      Port: 5173 (Dev) / Port: 80 (Nginx Production)    |
               +---------------------------+---------------------------+
                                           |
                              REST APIs / WebSocket (STOMP)
                                           v
               +-------------------------------------------------------+
               |         Java 21 + Spring Boot 3.3 Backend             |
               |                     Port: 8080                        |
               +-------+--------------------+-------------------+------+
                       |                    |                   |
            +----------v---------+   +------v-------+    +------v------+
            | PostgreSQL+PostGIS |   |   MongoDB    |    |    Redis    |
            |     Port: 5432     |   |  Port: 27017 |    |  Port: 6379 |
            +--------------------+   +--------------+    +-------------+

 IoT Telemetry Sensors / Simulator ──> Mosquitto MQTT (Port: 1883) ──> Spring Boot Ingestion
 External Weather & Air Quality    ──> Open-Meteo REST APIs        ──> Spring Boot Integration
```

---

## 2. Complete Technology Stack

| Layer | Technologies & Frameworks |
|---|---|
| **Backend Core** | Java 21 LTS, Spring Boot 3.3.4, Spring Web, Spring Security 6, Spring Data JPA, Spring Data MongoDB, Spring Data Redis |
| **Real-Time & Messaging** | Spring WebSocket, STOMP over SockJS, Eclipse Paho MQTT Client, Eclipse Mosquitto 2.0 |
| **Databases** | PostgreSQL 16 + PostGIS 3.4 (Relational & Spatial GIS), MongoDB 7.0 (IoT Telemetry & Weather Logs), Redis 7.2 (Caching) |
| **Database Migrations** | Flyway Database Migrations (`V1` to `V8`) |
| **Security & Auditing** | Stateless JWT (HMAC-SHA384), BCrypt Hashing, Role-Based Access Control (RBAC), Immutable Audit Logging with Secret Redaction |
| **Frontend** | React 18, Vite, React Router v6, Leaflet & React-Leaflet, Chart.js & React-Chartjs-2, StompJS & SockJS-Client, React Icons |
| **Styling & Design** | Pure Vanilla CSS Design System, Responsive Layout, WCAG Accessible Contrast, Municipal Theme (Neutral Slate & Teal Accents) |
| **Containerization** | Docker, Multi-stage Dockerfiles (Temurin 21 JRE, Nginx 1.27 Alpine), Docker Compose v3.8 |
| **Monitoring & Quality** | Spring Boot Actuator (`/actuator/health` with DB, Mongo, Redis, MQTT indicators), JUnit 5, Mockito |

---

## 3. Directory Structures

### 3.1 Backend Architecture (`backend/`)
```text
backend/src/main/java/com/smartcity/
├── CivicPulseApplication.java          # Spring Boot main bootstrap class
├── airquality/                         # Open-Meteo Air Quality client, service, MongoDB models
├── analytics/                          # Real database aggregation analytics engine & REST APIs
├── assignment/                         # Auto-assignment rule engine & response actions
├── audit/                              # Unified immutable audit logging service with secret sanitization
├── common/                             # ApiResponse, GlobalExceptionHandler, ErrorResponse, PagedResponse
├── config/                             # AsyncConfig, RedisConfig, RestClientConfig, SwaggerConfig
├── gis/                                # PostGIS spatial queries & GeoJSON feature collections
├── incident/                           # Incident domain entity, lifecycle management, repository, controller
├── iot/                                # MQTT subscriber, sensor threshold engine, MongoDB reading models
├── notification/                       # Notification abstractions (IN_APP, EMAIL, SMS) & rules
├── priority/                           # Context-aware Priority Rule Engine & test suites
├── security/                           # JwtAuthenticationFilter, JwtTokenProvider, SecurityConfig, Initializer
├── sla/                                # SLA calculator, background evaluation timer, escalation engine
├── user/                               # User, Role, Department, and Team entities & repositories
├── weather/                            # Open-Meteo Weather client, scheduled jobs, MongoDB storage
└── websocket/                          # STOMP WebSocket configuration & event publisher
```

### 3.2 Frontend Architecture (`frontend/`)
```text
frontend/src/
├── App.jsx                             # Router setup with role guards & public routes
├── main.jsx                            # React 18 bootstrap
├── index.css                           # Complete Municipal Command Centre design system
├── api/                                # Modular Axios API clients (auth, incidents, analytics, notifications, etc.)
├── context/
│   ├── AuthContext.jsx                 # JWT session management & role verification
│   └── WebSocketContext.jsx            # Live STOMP listener for all 6 command topics
├── layouts/
│   └── CommandCentreLayout.jsx         # Sidebar navigation, topbar telemetry & notification flyout
├── pages/
│   ├── LoginPage.jsx                   # Municipal single sign-on form
│   ├── DashboardPage.jsx               # Executive command centre dashboard with live metrics
│   ├── IncidentsPage.jsx               # Filterable incident grid with creation modal
│   ├── IncidentDetailPage.jsx         # Lifecycle tracker, SLA timer, assignment, action logs
│   ├── LiveMapPage.jsx                 # Full-screen GIS Leaflet map with spatial layers
│   ├── SensorsPage.jsx                 # Real-time IoT sensor telemetry grid & simulation triggers
│   ├── SlaBoardPage.jsx                # SLA escalation kanban board with countdown meters
│   ├── ComplaintsPage.jsx              # Citizen grievance tracking portal
│   ├── AnalyticsPage.jsx               # Real database charts, trends & department workload
│   ├── SettingsPage.jsx                # System status, health diagnostics, role management
│   └── PrivacyPolicyPage.jsx           # Municipal data governance & compliance policy
└── components/                         # LoadingSpinner, AlertToast, Modals, Leaflet icons
```

---

## 4. Database Architecture & Migrations

CivicPulse uses a polyglot persistence architecture optimized for performance and data integrity:

### 4.1 PostgreSQL + PostGIS (Relational & Spatial Core)
Managed via Flyway migrations under `backend/src/main/resources/db/migration/`:
- `V1__initial_schema.sql`: Core users, roles, departments, teams, and initial schema.
- `V2__incident_management.sql`: Incident entity, status histories, and indexes.
- `V3__priority_engine.sql`: Priority rules, criteria weights, and priority audit tables.
- `V4__sla_and_assignment.sql`: SLA configurations, active SLA tracking, department assignment rules.
- `V5__response_actions.sql`: Field response actions and work orders.
- `V6__gis_spatial_enhancements.sql`: PostGIS spatial points (`GEOMETRY(Point, 4326)`), spatial indexes (`GIST`), municipal infrastructure layers.
- `V7__external_data_and_thresholds.sql`: Environmental threshold configurations and trigger history.
- `V8__notifications_and_audit_enhancements.sql`: PostgreSQL storage for notifications (IN_APP, EMAIL, SMS) and enhanced indexes.
- `V9__auth_users_schema.sql`: Direct `role` column, `enabled` account status flag, and performance indexes on `LOWER(email)`, `LOWER(username)`, `role`, and `enabled`.
- `V10__incident_management_schema.sql`: Core incidents table optimization, explicit columns for category, severity, status, location, reporting, and JDBC indexes.
- `V11__sla_policies_and_escalation_schema.sql`: SLA policies by category and severity, first response and resolution time limits, incident SLA tracking, warning/breach states, and escalation history.
- `V12__infrastructure_and_map_schema.sql`: Municipal infrastructure assets, emergency shelters, air quality and weather monitoring station registries.
- `V13__notifications_enhancements.sql`: Municipal notification center schema, recipient targeting, notification types, severity levels, deterministic deduplication, and audit timestamps.
- `V14__demo_users_and_role_associations.sql`: Idempotent seed users (`admin`, `operator`, `officer_traffic`, `field_worker`, `citizen`), role associations, and department mapping.
- `V15__mentor_feedback_enhancements.sql`: Enhanced verification history, reputation score tracking, genuine complaint metrics, and audit timestamps.
- `V16__unified_complaint_lifecycle_and_portals.sql`: Unified complaint sequence, department foreign keys, assignee mapping, photo attachments table (`complaint_attachments`), and digital acknowledgement receipts (`complaint_receipts`).

### 4.2 Users Table Relational Schema (PostgreSQL)

| Column | Data Type | Constraints & Defaults | Description |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` | Unique municipal user identifier |
| `full_name` | `VARCHAR(150)` | `NOT NULL` | Citizen or operator full name |
| `email` | `VARCHAR(150)` | `NOT NULL UNIQUE` | Unique email address (case-insensitive indexed) |
| `username` | `VARCHAR(100)` | `UNIQUE` | Optional unique handle (defaults to email) |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | BCrypt password hash (salt rounds = 10) |
| `role` | `VARCHAR(50)` | `NOT NULL DEFAULT 'CITIZEN'` | Municipal role (`ADMIN`, `COMMAND_OPERATOR`, `DEPARTMENT_OFFICER`, `FIELD_WORKER`, `CITIZEN`) |
| `enabled` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Account active/disabled status |
| `phone` | `VARCHAR(50)` | `NULL` | Optional contact telephone number |
| `department_id` | `UUID` | `REFERENCES departments(id) ON DELETE SET NULL` | Assigned department (for municipal staff) |
| `team_id` | `UUID` | `REFERENCES teams(id) ON DELETE SET NULL` | Assigned response team |
| `role_id` | `UUID` | `REFERENCES roles(id) ON DELETE SET NULL` | Backwards-compatible foreign key |
| `active` | `BOOLEAN` | `DEFAULT TRUE` | Backward-compatibility alias for `enabled` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Account creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Last profile/password update timestamp |

### 4.2 MongoDB (High-Velocity Edge Data)
- `sensor_readings`: Raw time-series IoT telemetry from MQTT edge sensors.
- `iot_events`: Threshold breach alarms and sensor threshold crossing history.
- `weather_observations`: Real historical weather snapshots from Open-Meteo.
- `air_quality_observations`: Real historical PM2.5, PM10, and AQI snapshots.

### 4.3 Redis (High-Performance Caching)
- Caching live Open-Meteo weather and air quality responses to prevent third-party rate limiting.
- Session invalidation and rapid geospatial state lookup.

---

## 5. Authentication, Spring Security & JDBC Database Integration

### 5.1 Architecture & Workflow

```text
  React 18 Frontend                   Spring Security Filter Chain                   PostgreSQL Database
 (Vite + CivicPulse UI)                         (Backend)                            (PostGIS Enabled)
         |                                          |                                        |
         | --- 1. POST /api/auth/register --------> |                                        |
         |    { fullName, email, password, role }   |                                        |
         |                                          | --- 2. BCrypt.hashpw(raw, salt)        |
         |                                          | --- 3. UserJdbcRepository.save() ----> |
         |                                          |        INSERT INTO users (...)         |
         |                                          |        VALUES (?, ?, ?, ...)           |
         | <--- 4. HTTP 201 Created --------------- | <----- Row inserted (Password hash)    |
         |    { id, email, fullName, role }         |                                        |
         |    (ZERO passwordHash returned)          |                                        |
         |                                          |                                        |
         | --- 5. POST /api/auth/login -----------> |                                        |
         |    { username/email, password }          |                                        |
         |                                          | --- 6. UserJdbcRepository.findBy... -> |
         |                                          |        SELECT ... WHERE LOWER(email)=? |
         |                                          | <----- User Record Retrieved --------- |
         |                                          | --- 7. Check user.isEnabled() == true  |
         |                                          | --- 8. BCrypt.matches(raw, hash)       |
         |                                          | --- 9. JwtUtils.generateJwtToken()     |
         | <--- 10. HTTP 200 OK (JWT Access Token)  |                                        |
         |                                          |                                        |
         | --- 11. GET /api/auth/me (Bearer JWT) -> | --- 12. AuthTokenFilter validates JWT  |
         |                                          | --- 13. Populates SecurityContext      |
         | <--- 14. HTTP 200 OK (User Profile) ---- |                                        |
         |                                          |                                        |
         | --- 15. POST /api/auth/logout ---------> | --- 16. AuditLog("LOGOUT") recorded    |
         | <--- 17. HTTP 200 OK ("Logged out") ---- |                                        |
         |     (Client purges tokens from storage)  |                                        |
```

### 5.2 Key Security Implementation Details
- **Explicit Parameterized SQL via JdbcTemplate**: Spring Data JPA / Hibernate is **not** used as the persistence layer for users or authentication. All user database interactions are implemented in `UserJdbcRepository` (implementing `UserDao` and `UserRepository`) with strictly parameterized `JdbcTemplate` queries (`?` placeholders).
- **BCrypt Password Hashing**: Passwords are cryptographically salted and hashed using Spring Security's `BCryptPasswordEncoder` (10 rounds). Plaintext passwords are never stored in the database and never logged.
- **Zero Information Leakage**: Password hashes are stripped at the service layer and never exposed in any DTO response. `UserSummaryResponse` contains only non-sensitive user attributes.
- **Enabled / Disabled Account Enforcement**: Account status is stored in PostgreSQL as `enabled BOOLEAN`. If an administrator disables an account (`enabled = false`), Spring Security immediately blocks authentication attempts with HTTP 401 and an `ACCOUNT_DISABLED` error message.
- **Role-Based Authorization (RBAC)**: All dashboard and municipal API endpoints enforce `@EnableMethodSecurity` and `requestMatchers` in `SecurityConfig`. Ordinary users (`CITIZEN`) cannot access operator, department, or admin endpoints.
- **Safe Logout & Session Persistence**: The frontend persists active JWT sessions across page reloads via `localStorage` and immediately verifies them against `GET /api/auth/me`. On logout (`POST /api/auth/logout`), the backend writes an audit log, the client purges all stored tokens, and redirects safely to `/login`.
- **CORS Configuration**: Explicit origin whitelist (`app.cors.allowed-origins`), credentials support, and authorized method restrictions (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`).

### 5.3 Database Demonstration: Parameterized JDBC Queries

Every persistence operation for users and authentication executes explicit parameterized SQL via `UserJdbcRepository`:

#### 1. Registration (INSERT Parameterized Query)
```sql
-- Executed on POST /api/auth/register
INSERT INTO users (
    id, full_name, email, username, password_hash, role, enabled, phone,
    department_id, team_id, role_id, active, created_at, updated_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```
- Parameters: `[UUID, fullName, email.toLowerCase(), username, bcryptHash, role, enabled, phone, deptId, teamId, roleId, active, createdAt, updatedAt]`
- Duplicate email constraints are caught and translated into `DuplicateResourceException` with HTTP 409 Conflict.

#### 2. User Lookup (SELECT Parameterized Queries)
```sql
-- Executed on GET /api/auth/me and authentication user details lookup:
-- Lookup by ID:
SELECT id, full_name, email, username, password_hash, role, enabled, phone,
       department_id, team_id, role_id, active, created_at, updated_at
FROM users WHERE id = ?;

-- Lookup by Email (case-insensitive):
SELECT id, full_name, email, username, password_hash, role, enabled, phone,
       department_id, team_id, role_id, active, created_at, updated_at
FROM users WHERE LOWER(email) = LOWER(?);

-- Lookup by Username or Email (supports multi-format login):
SELECT id, full_name, email, username, password_hash, role, enabled, phone,
       department_id, team_id, role_id, active, created_at, updated_at
FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?);
```

#### 3. Account / Profile Updates (UPDATE Parameterized Queries)
```sql
-- Executed on PUT /api/auth/profile:
UPDATE users SET full_name = ?, phone = ?, updated_at = ? WHERE id = ?;

-- Executed on PUT /api/auth/change-password:
UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?;

-- Executed on account enable / disable:
UPDATE users SET enabled = ?, active = ?, updated_at = ? WHERE id = ?;

-- Executed on municipal role promotion / reassignment:
UPDATE users SET role = ?, updated_at = ? WHERE id = ?;
```

#### 4. BCrypt Password Hashes in PostgreSQL
A sample record in the PostgreSQL `users` table:
```text
id            : 8c347b59-a1b2-4d33-9128-b0d3e52f671c
full_name     : Jane Citizen
email         : jane.citizen@smartcity.gov
username      : jane.citizen
password_hash : $2a$10$wK1V.3V017zFvF5z4lJ1lOPs8nK/8rX7h5d4M9gH6eI8tq3xR2sK.
role          : CITIZEN
enabled       : true
active        : true
created_at    : 2026-10-28 10:15:00+00
updated_at    : 2026-10-28 10:15:00+00
```

### 5.4 Municipal Role Permissions

| Role | Access Scope |
|---|---|
| **ADMIN** | Full system authority. User & role management, department configuration, priority rules, system diagnostics, and audit logs. |
| **COMMAND_OPERATOR** | Live incident dispatch, priority evaluation, department auto-assignment, SLA escalation monitoring, live GIS map, and alerts. |
| **DEPARTMENT_OFFICER** | Department-specific incident triage, work orders, field crew assignments, and status updates. |
| **FIELD_WORKER** | Read-only view of assigned tasks, on-site response action updates, and task completion notes. |
| **CITIZEN** | Citizen grievance filing, personal complaint tracking, public safety map, and municipal privacy policy. |

---

## 6. Authentication UI & Design System

The frontend provides a fully responsive authentication suite styled with the CivicPulse design system palette (`#f8fafc`, `#0284c7`, `#0f172a`, `#cbd5e1`):

1. **Sign-Up Page (`/signup`, `/register`)**:
   - Fields: Full Name, Email Address, Username, Phone Number (optional), Role selector, Password, and Confirm Password.
   - Real-time client-side validation: Name length, RFC 5322 email regex, minimum 6 character password, and matching password verification.
   - Interactive password visibility toggles (`FiEye` / `FiEyeOff`) on both password fields.
   - Visual states: Loading spinner/disabled button during dispatch, error banner on conflict/invalid input, and success alert before redirecting to `/login`.

2. **Login Page (`/login`)**:
   - Fields: Username or Email address, Password.
   - Password visibility toggle.
   - Form validation with inline error hints.
   - Loading, success, and error feedback with accessible alerts.
   - Link to citizen sign-up page.
   - Quick Demo Role selector buttons for fast testing of `ADMIN`, `COMMAND_OPERATOR`, `DEPARTMENT_OFFICER`, `FIELD_WORKER`, and `CITIZEN`.

3. **Protected Dashboard Routes (`<ProtectedRoute>`)**:
   - Guards all Command Centre routes (`/dashboard`, `/incidents`, `/map`, `/sensors`, `/sla`, `/complaints`, `/analytics`, `/settings`).
   - Unauthenticated visitors are automatically redirected to `/login`.
   - Role enforcement restricts sensitive sub-routes according to user permissions.

4. **Safe Logout**:
   - Topbar user menu and layout sidebar include a prominent Logout button.
   - Notifies backend via `POST /api/auth/logout` to record an immutable audit log.
   - Purges JWT and session state from `localStorage` and redirects safely to `/login`.

---

## 6. Notification System

A vendor-agnostic notification engine supporting multi-channel delivery:
- **IN_APP**: Real-time notification badge counter and flyout in the topbar.
- **EMAIL**: Clean email sender abstraction (`EmailNotificationSender`).
- **SMS**: Clean vendor-agnostic SMS sender abstraction (`SmsNotificationSender`).

### Notification Dispatch Rules:
1. **CRITICAL Incident**: Dispatched immediately to **Command Centre** (`COMMAND_OPERATOR`) and the **Responsible Department** (`DEPARTMENT_OFFICER`).
2. **SLA AT_RISK**: Dispatched to the **Assigned Department** (`DEPARTMENT_OFFICER`) before breach occurs.
3. **SLA BREACHED**: Dispatched simultaneously to the **Department Officer** and the **Command Operator**.
4. **LEVEL_3 Escalation**: Dispatched directly to **ADMIN** and **Command Centre** for executive intervention.

---

## 7. Real Database Analytics

The `/api/analytics/*` suite performs calculations using PostgreSQL and MongoDB data (zero mock data):
- **Overview Metrics**: Total incidents, active incidents, resolved incidents, critical counts, SLA compliance percentage, sensor counts, and alert volumes.
- **Incident Trends**: Grouped by date, status, priority tier, ingestion source, and department.
- **Department Workload**: Per-department active count, resolved count, average resolution time, and compliance rate.
- **IoT Trends**: Sensor readings and alert histograms over time.
- **Date Filtering**: Supports `ALL`, `30D`, and `7D` date ranges across all analytics endpoints.

---

## 8. REST API Reference Summary

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Register citizen or municipal account (201 Created)
- `POST /api/auth/login` — Authenticate with username or email and receive JWT bearer token (200 OK)
- `POST /api/auth/logout` — Invalidate user session and record audit log (200 OK)
- `GET /api/auth/me` — Retrieve verified profile of currently authenticated user (200 OK)
- `PUT /api/auth/profile` — Update user profile details via parameterized JDBC update (200 OK)
- `PUT /api/auth/change-password` — Verify old password and set new BCrypt hash via parameterized JDBC update (200 OK)

### Incidents (`/api/incidents`)
- `GET /api/incidents` — Paginated, filterable incident directory
- `POST /api/incidents` — Create municipal incident (auto-generates tracking number)
- `GET /api/incidents/{id}` — Incident details by UUID
- `PUT /api/incidents/{id}` — Update mutable incident details
- `PATCH /api/incidents/{id}/status` — Transition status (`DETECTED` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`)
- `POST /api/incidents/{id}/evaluate-priority` — Trigger Priority Rule Engine
- `POST /api/incidents/{id}/auto-assign` — Evaluate department assignment rules
- `POST /api/incidents/{id}/assign` — Manually assign department and team
- `GET /api/incidents/{id}/history` — Immutable lifecycle audit trail

### Notifications (`/api/notifications`)
- `GET /api/notifications` — Retrieve user & operational notifications (`unreadOnly=true` supported)
- `PATCH /api/notifications/{id}/read` — Mark single notification as READ
- `PATCH /api/notifications/read-all` — Mark all user notifications as READ

### City Analytics (`/api/analytics`)
- `GET /api/analytics/overview` — High-level KPI summary & workload
- `GET /api/analytics/incidents` — Breakdown by status, priority, source & daily trends
- `GET /api/analytics/sla` — Compliance rate, breach counts & escalation levels
- `GET /api/analytics/departments` — Departmental performance & incident counts
- `GET /api/analytics/sensors` — IoT grid telemetry & threshold alert counts

### GIS & Spatial Services (`/api/gis`)
- `GET /api/gis/incidents` — All incidents as GeoJSON FeatureCollection
- `GET /api/gis/sensors` — All IoT sensors as GeoJSON FeatureCollection
- `GET /api/gis/infrastructure` — Traffic signals, waste bins & public assets as GeoJSON
- `GET /api/gis/nearby-incidents` — PostGIS spatial radius search (`latitude`, `longitude`, `radius`)
- `GET /api/gis/nearby-resources` — Emergency services radius search

### Environmental Telemetry (`/api/weather`, `/api/air-quality`)
- `GET /api/weather/current` — Current temperature, humidity, precipitation & wind from Open-Meteo
- `GET /api/air-quality/current` — Current PM2.5, PM10 & AQI observations from Open-Meteo

### Audit Logging (`/api/audit-logs`)
- `GET /api/audit-logs` — Paginated audit log with secret redaction
- `GET /api/audit-logs/{entityType}/{entityId}` — Audit trail for specific entity

### Actuator Monitoring (`/actuator`)
- `GET /actuator/health` — Safe health status verifying App, PostgreSQL, MongoDB, Redis, and MQTT

---

## 9. Environment Variables Reference

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC Connection URL | `jdbc:postgresql://localhost:5432/civicpulse_db` |
| `DATABASE_USERNAME` | PostgreSQL User | `civicpulse` |
| `DATABASE_PASSWORD` | PostgreSQL Password | `civicpulse_secret` |
| `JWT_SECRET` | Base64-encoded HMAC-SHA384 Secret Key | `c3VwZXJzZWNyZXRqd3RzZWNyZXRrZXlmb3JzbWFydGNpdHljb21tYW5kY2VudHJlY2l2aWNwdWxzZQ==` |
| `MONGO_URI` | MongoDB Connection URI | `mongodb://localhost:27017/civicpulse` |
| `REDIS_HOST` | Redis Server Hostname | `localhost` |
| `REDIS_PORT` | Redis Server Port | `6379` |
| `MQTT_BROKER_URL` | Mosquitto MQTT Broker URL | `tcp://localhost:1883` |
| `MQTT_CLIENT_ID` | MQTT Client Identifier | `civicpulse-backend` |
| `ADMIN_USERNAME` | Seeded Admin Username | `admin` |
| `ADMIN_PASSWORD` | Seeded Admin Password | `Admin@CivicPulse2026` |
| `OPEN_METEO_WEATHER_URL` | Open-Meteo Weather API URL | `https://api.open-meteo.com/v1/forecast` |
| `OPEN_METEO_AIR_QUALITY_URL` | Open-Meteo Air Quality API URL | `https://air-quality-api.open-meteo.com/v1/air-quality` |

---

## 10. Local Setup & Running Guide

### 10.1 Prerequisites
- **Java 21 LTS** (`openjdk 21`)
- **Maven 3.9+**
- **Node.js 20+** & **npm**
- **PostgreSQL 16** with **PostGIS** extension
- **MongoDB 7.0+**
- **Redis 7.2+**
- **Mosquitto MQTT Broker 2.0+**

### 10.2 Database Preparation
Ensure PostgreSQL database `civicpulse_db` exists with PostGIS enabled:
```sql
CREATE DATABASE civicpulse_db;
\c civicpulse_db
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 10.3 Run Backend Tests & Start Backend
```bash
cd backend

# Execute complete test suite (305 unit & integration tests)
mvn clean test

# Build production JAR
mvn clean package -DskipTests

# Start Spring Boot application
java -jar target/civicpulse-backend-1.0.0-SNAPSHOT.jar
# (Application starts on http://localhost:8080)
```

### 10.4 Run Frontend Tests & Start Frontend
```bash
cd frontend

# Install dependencies
npm install

# Build production distribution
npm run build

# Start local Vite development server
npm run dev
# (Frontend starts on http://localhost:5173)
```

---

## 11. Docker Deployment Guide

The platform includes a multi-service `docker-compose.yml` that provisions the entire stack with health checks, container dependency trees, and persistent named volumes.

### 11.1 Build & Start Containers
```bash
# Build and run all services in detached mode
docker compose up -d --build

# View container status and health
docker compose ps
```

### 11.2 Service Architecture in Docker
| Service Name | Image | Port | Health Check |
|---|---|---|---|
| `postgres` | `postgis/postgis:16-3.4` | `5432` | `pg_isready` |
| `mongodb` | `mongo:7.0` | `27017` | `mongosh db.adminCommand('ping')` |
| `redis` | `redis:7.2-alpine` | `6379` | `redis-cli ping` |
| `mosquitto` | `eclipse-mosquitto:2.0` | `1883`, `9001` | TCP socket |
| `backend` | Temurin 21 JRE (Multi-stage) | `8080` | `curl -f /actuator/health` |
| `frontend` | Nginx 1.27 (Multi-stage) | `80` | `wget -q /` |

### 11.3 Stop Stack
```bash
docker compose down
```

---

## 12. Pre-Seeded Demonstration Accounts

For municipal testing and role evaluation:

| Username | Password | Role | Intended Dashboard / Capabilities |
|---|---|---|---|
| `admin` | `Admin@CivicPulse2026` | `ADMIN` | Full Command Centre, Analytics, Settings, Audit Logs |
| `operator` | `Admin@CivicPulse2026` | `COMMAND_OPERATOR` | Live Incident Management, Dispatch, SLA Board |
| `officer_traffic` | `Admin@CivicPulse2026` | `DEPARTMENT_OFFICER` | Traffic Department Work Orders, SLA Monitoring |
| `field_worker` | `Admin@CivicPulse2026` | `FIELD_WORKER` | Assigned Field Tasks & Response Actions |
| `citizen` | `Admin@CivicPulse2026` | `CITIZEN` | Citizen Complaint Submissions & Status Tracking |

---

## 13. Interactive Multi-Module Demo Workflow

Execute this 6-module demonstration sequence in the browser (`https://localhost:5173`) or via REST endpoints:

### Module 1: Authentication & Role Permissions
1. Navigate to `https://localhost:5173/login`.
2. Click **Operator** quick-login button (`operator` / `Admin@CivicPulse2026`) or sign up a new account via `/signup`.
3. Verify password visibility toggle (`FiEye`/`FiEyeOff`).
4. Notice seamless authentication, issuance of stateless JWT token stored in client storage, and redirection to `/dashboard`.
5. Verify role gating: As a `CITIZEN`, settings and department management routes are hidden; as an `ADMIN` or `COMMAND_OPERATOR`, full operational panels are available.

### Module 2: Incident Management Lifecycle & Filters
1. Open **Incidents** (`/incidents`) to view real incidents retrieved via PostgreSQL JDBC.
2. Click **Report Incident**; enter title, category (`Flood`, `Fire`, `Traffic`), severity (`HIGH` or `CRITICAL`), and coordinates.
3. Submit and observe the incident appear immediately with a unique `INC-YYYYMMDD-XXXXXXXX` identifier and `DETECTED` / `OPEN` status.
4. Filter by severity (`CRITICAL`), category, and search text using the live debounce filters.
5. Click on the incident to view `IncidentDetailPage`; update status (`DETECTED` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`) and view the immutable status history audit log below.

### Module 3: SLA Tracking, Warnings & Automatic Escalation
1. Open **SLA Tracker** (`/sla`) or view the SLA section on an incident card.
2. An SLA policy is automatically assigned upon incident creation according to severity (e.g., Critical = 30m response / 120m resolution limit).
3. Observe the real-time SLA status indicator (`ON_TRACK`, `WARNING`, `BREACHED`).
4. As deadlines approach or elapse, the background `SLAManager` automatically marks warnings, triggers level 1/2/3 escalations, and updates the database records.
5. On status transition to `RESOLVED`, the actual resolution time is captured and SLA status is marked `COMPLETED`.

### Module 4: Interactive GIS Map & Infrastructure Monitoring
1. Open **Live GIS Map** (`/map`).
2. Interactive Leaflet map initializes centered on the municipality with OpenStreetMap tiles.
3. Observe colored severity pins for real incidents:
   - Red: Critical / Extreme
   - Orange: High
   - Yellow: Medium
   - Green: Low / Resolved
4. Click on an incident pin to reveal its popup card with category, severity, status, and direct link to details.
5. Use the Layer Toggle controls in the upper-right corner to toggle:
   - **Incidents Layer** (with clustering)
   - **Infrastructure Assets** (traffic lights, pump stations, power nodes)
   - **Emergency Shelters** (capacity and occupancy indicators)
   - **Environmental Stations** (air quality and weather monitoring)

### Module 5: Analytics Dashboard & City Intelligence
1. Open **Analytics** (`/analytics`).
2. View key municipal KPIs: Total Incidents, Active Load, Resolved Rate, SLA Compliance %, and Infrastructure Health Score.
3. Interactive Chart.js graphs render:
   - Daily incident trend lines
   - Incident volume distribution by category (pie chart)
   - Severity and status breakdowns
   - Average response and resolution times by department
4. Filter data using time range selectors (`7D`, `30D`, `ALL`).
5. Click **Export CSV**: The generated file downloads instantly with sanitized cell values, shielding against CSV formula injection (`=`, `+`, `-`, `@` characters escaped with single quotes).

### Module 6: Real-Time Alerts & Notification Center
1. Observe the notification bell icon in the top navigation bar.
2. Unread notification count badge updates in real time via WebSocket STOMP (`/topic/notifications` and `/topic/alerts`).
3. Click the bell icon to open the flyout or navigate to `/notifications`.
4. Test notifications:
   - System-wide broadcast alerts for critical incidents
   - Direct user notifications for task assignments and SLA breach warnings
5. Click **Mark as Read** on individual notifications or **Mark All as Read** to update persistence in PostgreSQL.
6. Delete or dismiss notifications with role authorization checks preventing cross-user data tampering.

---

## 14. End-to-End Operational Pipeline

```text
CITIZEN / OPERATOR
      ↓
POST /api/incidents (Incident Creation)
      ↓
POST /api/incidents/{id}/evaluate-priority (Priority Rule Engine)
      ↓
Priority Tier Assigned (CRITICAL / HIGH / MEDIUM / LOW)
      ↓
SLA Target Created (Deadline calculated from Priority SLA Policy)
      ↓
POST /api/incidents/{id}/auto-assign (Auto-Assignment Engine)
      ↓
Department & Response Team Assigned
      ↓
WebSocket STOMP Broadcast (/topic/incidents, /topic/alerts)
      ↓
React Command Centre Updates Instantly
      ↓
SLA Evaluator Runs (SLAManager background timer)
      ↓
AT_RISK / BREACHED Detected
      ↓
Notification Created (IN_APP, EMAIL, SMS)
      ↓
Level 1 / 2 / 3 Escalation
      ↓
Incident Resolved (PATCH /api/incidents/{id}/status)
      ↓
Immutable Audit Log Recorded (Zero secret leakage)
      ↓
Analytics APIs Updated (GET /api/analytics/*)
```

---

## 15. Verification Commands & Testing Instructions

### 15.1 Execute Backend Test Suite
```bash
cd backend

# Run the complete test suite (305 unit and database integration tests):
mvn clean test

# Run specific integration test suites:
mvn test -Dtest=AuthControllerSecurityIntegrationTest,UserJdbcRepositoryTest,AuthServiceTest,JwtUtilsTest
mvn test -Dtest=IncidentIntegrationTest,SlaTrackingAndEscalationTest,AnalyticsControllerTest,NotificationControllerTest
```

### 15.2 Build Frontend & Backend Packages
```bash
# Backend package
cd backend && mvn clean package -DskipTests

# Frontend build
cd frontend && npm run build
```

### 15.3 End-to-End API Authentication Testing via cURL

```bash
# 1. Register a new Citizen user via JDBC:
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Eleanor Vance",
    "email": "eleanor.vance@smartcity.gov",
    "username": "eleanor_vance",
    "password": "SecurePassword@2026",
    "phone": "+1-555-0188",
    "role": "CITIZEN"
  }'

# 2. Login with Username or Email to obtain JWT Access Token:
AUTH_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "eleanor.vance@smartcity.gov",
    "password": "SecurePassword@2026"
  }' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 3. Retrieve authenticated profile (GET /api/auth/me):
curl -s -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $AUTH_TOKEN"

# 4. Update profile details via JDBC:
curl -s -X PUT http://localhost:8080/api/auth/profile \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Eleanor Vance-Updated",
    "phone": "+1-555-0199"
  }'

# 5. Safe Logout:
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

---

## SMS Gateway & Mobile OTP Configuration

CivicPulse supports two modes for mobile OTP verification: **Local Demo Mode** (ideal for project demonstrations, viva, and offline development) and **Real SMS Gateways** (Twilio, Fast2SMS, HTTP REST for production).

### 1. Local Demo Mode (Recommended for College Demonstration)
In local demo mode, the application generates a real dynamic 6-digit numeric OTP with all security policies active (5-minute expiry, single-use, 60s cooldown, max 3 attempts, salted SHA-256 in PostgreSQL), prints the OTP directly to the **backend terminal log**, and writes it to a temporary local file named `backend/demo-otp.log` (which is strictly excluded from version control via `.gitignore`).

Set in your `.env` file:
```bash
SMARTCITY_SMS_PROVIDER=mock
```

When you request an OTP on the login page (`http://localhost:5173/login`) or sign-up page (`http://localhost:5173/signup`), check the backend terminal or inspect `backend/demo-otp.log`:
```text
================================================================================
                     [CIVICPULSE LOCAL DEMO SMS GATEWAY]                       
--------------------------------------------------------------------------------
 Recipient Mobile : +919876543210
 Generated OTP    : >>> 810834 <<<
 Purpose          : Citizen Mobile Verification / Login
 Security Policy  : Valid for 5 minutes | Single-Use | Salted SHA-256 in DB     
 Mentor Notice    : Displayed strictly in local terminal for demonstration.     
================================================================================
```

### 2. Real Twilio SMS Integration
To use live Twilio SMS delivery, set in `.env`:
```bash
SMARTCITY_SMS_PROVIDER=twilio
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_FROM_NUMBER=+1xxxxxxxxxx
```

### Production Security Safeguards
- When running under the `prod` profile, `MockDevSmsGatewayProvider` is **strictly prohibited**. The backend validates credentials at boot and throws `IllegalStateException` if mock provider is active.
- Plaintext OTPs are **never** returned in API responses.
- Plaintext OTPs are **never** stored in the database (salted SHA-256 hash only).
- In production, OTPs are never printed to logs, and recipient phone numbers are masked (`+91******XX`).

### 2. Fast2SMS Integration (India Route)
```bash
export SMARTCITY_SMS_PROVIDER=fast2sms
export FAST2SMS_API_KEY=your_fast2sms_api_key
```

### 3. Generic REST SMS Gateway (NIC, CDAC, Msg91, etc.)
```bash
export SMARTCITY_SMS_PROVIDER=http
export SMS_GATEWAY_URL=https://api.yourprovider.com/sms/send
export SMS_GATEWAY_API_KEY=your_api_key
export SMS_GATEWAY_SENDER_ID=CIVICPULSE
```

### Security & Privacy Protections
- Plain-text OTPs are **never** stored in the database (SHA-256 salted hashes only).
- OTPs are **never** returned in API responses.
- OTPs are **never** logged to production application logs.
- Mobile numbers are masked in all audit and application logs (e.g. `+91******10`).
- If an SMS provider fails to deliver the message, the system returns an explicit failure without claiming delivery.

---

## 16. Known Limitations & External Service Dependencies

1. **Weather & Air Quality Telemetry (Open-Meteo)**:
   - CivicPulse integrates with the public Open-Meteo API for real-time weather and air quality observations.
   - Open-Meteo enforces a rate limit on free tier usage. When daily request limits are exceeded (HTTP 429), the backend automatically and safely falls back to cached observations stored in MongoDB without disrupting command centre operations.
2. **SMS Gateway Deliverability**:
   - In `prod` profile, real SMS delivery requires valid Twilio credentials and recipient numbers registered under your Twilio account if using a trial balance.
   - For local development, academic viva, and demonstration, `SMARTCITY_SMS_PROVIDER=mock` provides deterministic, reliable testing without external network dependencies.
3. **Government Portal Synchronization (CP-GRAMS)**:
   - Interoperability with CP-GRAMS runs against a simulated municipal sandbox environment. No actual classified government databases are mutated.
4. **IoT Sensor Ingestion**:
   - The sensor telemetry ingestion engine connects to Eclipse Mosquitto on port `1883`. If the broker is stopped, the application gracefully flags the health indicator as `DOWN` while retaining full capability across relational grievances and SLA tracking.

---

**CivicPulse Smart City Platform** — Production Release 1.0.0

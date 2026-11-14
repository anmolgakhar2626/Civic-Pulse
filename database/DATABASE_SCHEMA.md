# CivicPulse Smart City Command Centre — Database Architecture & Schema Reference

## 1. Overview
CivicPulse utilizes a hybrid polyglot persistence architecture designed for high-concurrency municipal operations:

- **PostgreSQL 16 + PostGIS 3.4**: Serves as the primary ACID relational and spatial GIS datastore. Handles citizen accounts, grievance lifecycles, departmental workflows, role-based access control (RBAC), SLA policies, escalation logs, and geospatial entity indexing.
- **MongoDB 7.0**: Serves as the time-series and document store for high-frequency IoT sensor telemetry, environmental air quality observations, weather readings, and raw MQTT sensor events.
- **Redis 7.2**: In-memory cache for fast operational state, session tokens, rate limiting, and real-time dashboard telemetry caches.

---

## 2. PostgreSQL Relational & GIS Tables (34 Tables)

### 2.1 Core Authentication & RBAC
- **`users`**: Municipal personnel and registered citizens.
  - Columns: `id (UUID PK)`, `username`, `email`, `password_hash (BCrypt)`, `full_name`, `phone`, `mobile_number`, `role`, `department_id (FK)`, `team_id (FK)`, `verified_citizen (BOOLEAN)`, `genuine_complaint_count (INT)`, `mobile_verified (BOOLEAN)`, `enabled (BOOLEAN)`, `active (BOOLEAN)`, `created_at`, `updated_at`.
- **`roles`**: System role definitions (`ADMIN`, `COMMAND_OPERATOR`, `DEPARTMENT_OFFICER`, `FIELD_WORKER`, `CITIZEN`).
- **`user_roles`**: Many-to-many junction table mapping `user_id` to `role_id`.
- **`mobile_otps`**: Secure single-use OTP authentication records with SHA-256 hash storage, 5-minute expiry, cooldown timers, and attempt counters.

### 2.2 Municipal Departments & Operations
- **`departments`**: Municipal administrative directorates (`TRAFFIC`, `PWD`, `WATER`, `ELECTRICAL`, `SANITATION`, `HEALTH`, `ENVIRONMENT`, `EMERGENCY`).
- **`teams`**: Specialized response and maintenance crews within departments (e.g., `TRAFFIC_OPS`, `PWD_ROAD_REPAIR`).

### 2.3 Citizen Complaints & Grievance Lifecycle
- **`complaints`**: Central grievance record tracking full redressal lifecycle.
  - Columns: `id (UUID PK)`, `complaint_number (e.g. CP-2026-000001)`, `citizen_id (FK)`, `citizen_name`, `citizen_phone`, `title`, `description`, `category`, `subcategory`, `department_id (FK)`, `assigned_to_user_id (FK)`, `location_id (FK)`, `address`, `latitude`, `longitude`, `priority (CRITICAL, HIGH, MEDIUM, LOW)`, `status (SUBMITTED, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)`, `complaint_verification_status (GENUINE, UNVERIFIED, REJECTED_SPAM, DUPLICATE)`, `sla_minutes`, `sla_deadline`, `sla_status (ON_TRACK, AT_RISK, BREACHED)`, `resolution_notes`, `resolution_evidence_url`, `resolved_at`, `closed_at`, `created_at`, `updated_at`.
- **`complaint_attachments`**: Photos and supporting documents uploaded by citizens or field crews (`CITIZEN_EVIDENCE`, `RESOLUTION_PROOF`).
- **`complaint_receipts`**: Official immutable digital acknowledgement receipts with unique receipt numbers and timestamped parameters.
- **`complaint_status_history`**: Audit trail of every lifecycle status transition, recording actor, previous status, next status, and timestamp.
- **`complaint_verification_history`**: Verification decision log by department officers marking reports as genuine or spam, recalculating citizen reputation scores.

### 2.4 Incidents, SLA Engine & Prioritization
- **`incidents`**: City-wide operational incidents triggered via IoT sensors, municipal hotlines, or operator input.
- **`incident_assignment_history`**: History of field crew dispatch and response assignments.
- **`incident_status_history`**: Incident lifecycle audit trail.
- **`priority_rules`**: Configurable rule engine matching severity, category, location, and environmental thresholds to priority levels.
- **`sla_policies`**: Target response and resolution time definitions per incident category and severity tier.
- **`sla_records`**: Real-time tracking of deadline compliance, remaining minutes, breach state, and escalation levels (`LEVEL_1`, `LEVEL_2`, `LEVEL_3`).
- **`sla_escalation_history`**: Notification and re-assignment audit trail when target resolution windows expire.
- **`response_actions`**: Standard operating procedure (SOP) action checklists generated for responders.
- **`assignment_rules`**: Automated routing rules mapping incident types to departments and teams.

### 2.5 GIS Spatial Infrastructure (PostGIS)
- **`locations`**: Spatial coordinates (`geometry(Point, 4326)`), ward numbers, zones, and geocoded street addresses.
- **`infrastructure`**: City assets (bridges, flyovers, water mains, substations) with point/polygon geometries.
- **`sensors_gis`**: Physical geospatial positions of IoT sensors across municipal grid.
- **`traffic_signals`**: Traffic light junction coordinates and live controller states.
- **`waste_bins`**: Smart waste receptacle locations with fill level sensors.
- **`emergency_resources`**: Real-time locations of fire engines, ambulances, and police patrol vehicles.

### 2.6 Notifications & Audit Logs
- **`notifications`**: Targeted multi-channel alerts (broadcast and direct-to-user) with read/unread tracking.
- **`audit_logs`**: Immutable security audit log tracking user logins, role modifications, and system configuration updates with sensitive parameter masking.

### 2.7 External Grievance Sandboxes
- **`external_grievances`**: Simulated interoperability with government portals (e.g. CP-GRAMS).
- **`government_portals`**: External grievance system registry.
- **`grievance_status_history`**: External portal synchronization logs.
- **`grievance_sync_logs`**: Integration sync receipts.
- **`acknowledgement_documents`**: Generated official letters and PDF receipts.

---

## 3. MongoDB Collections (Time-Series & Telemetry)

1. **`sensor_readings`**: High-frequency raw sensor events (water pressure, air quality, noise, vibration).
2. **`waste_bin_readings`**: Ultrasonic depth sensor logs monitoring trash fill percentages.
3. **`air_quality_readings`**: PM2.5, PM10, NO2, SO2, CO, and AQI readings.
4. **`sensors`**: IoT sensor registry metadata (manufacturer, battery, calibration, MQTT topic).
5. **`traffic_readings`**: Vehicle count, average transit speed, and road congestion density.
6. **`iot_events`**: Anomaly detection events triggered when sensor readings breach thresholds.
7. **`air_quality_observations`**: External hourly air quality telemetry cached from Open-Meteo.
8. **`weather_observations`**: Real-time weather parameters (temperature, humidity, precipitation, wind speed).

---

## 4. PostGIS Spatial Operations & Indexing

All spatial tables use the EPSG:4326 (WGS 84) coordinate reference system:
- Spatial indexes created using **GiST** (`CREATE INDEX idx_locations_geom ON locations USING GIST(geom);`).
- Proximity queries utilize `ST_DWithin` and `ST_Distance` on geography projections for meter-accurate distance calculations.
- GeoJSON export supported directly via `ST_AsGeoJSON()`.

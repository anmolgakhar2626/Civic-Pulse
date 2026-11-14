# Smart City Command Centre
## Product Requirements Document (PRD)

Version: 1.0
Status: Project Specification
Project Type: Smart City / Urban Operations Platform

---

## 1. Product Overview

The Smart City Command Centre is a unified urban operations platform built with a Java 21 + Spring Boot backend that collects data from IoT sensors, external APIs, GIS services, municipal systems, and citizen reports and presents it through a real-time command centre.

The platform is designed to help city authorities monitor urban conditions, detect incidents, prioritize them using a transparent rule-based engine, assign incidents to the appropriate departments, track SLA compliance, and coordinate automated or assisted responses.

The platform will cover:

- Urban governance
- Citizen services
- Smart mobility
- Air-quality monitoring
- Smart waste management
- Infrastructure maintenance
- Emergency response
- GIS-based city monitoring
- Real-time alerts
- Analytics and prediction

The central design principle is that incident priority must not be randomly assigned. Priority is derived from documented incident rules, severity indicators, location context, exposure, infrastructure importance, and sensor thresholds.

---

# 2. Problem Statement

Cities generate large volumes of information from traffic systems, environmental sensors, waste infrastructure, citizen complaints, weather services, emergency events, and municipal departments.

Without a unified platform:

- Information remains distributed across departments.
- Incidents can be difficult to prioritize consistently.
- Critical events may not reach the correct department quickly.
- Citizens cannot easily track complaints.
- City officials lack a single real-time operational view.
- Geographic relationships between incidents and infrastructure are difficult to understand.
- Routine tasks may be handled manually instead of being grouped and optimized.
- SLA violations may not be visible until after the deadline.

The Smart City Command Centre addresses these problems through a centralized, GIS-enabled, event-driven platform.

---

# 3. Product Goals

## Primary Goals

1. Create a unified real-time view of city operations.
2. Integrate IoT and external data sources.
3. Provide a GIS-based operational map.
4. Automatically classify incidents into four priority tiers.
5. Attach a target SLA to every actionable incident.
6. Automatically recommend or initiate appropriate response actions.
7. Assign incidents to relevant departments or response teams.
8. Track incident lifecycle and SLA performance.
9. Provide citizen complaint submission and tracking.
10. Provide analytics for city officials.
11. Support future AI/ML prediction without making AI mandatory for basic incident classification.

## Secondary Goals

- Optimize municipal routes.
- Detect anomalies in sensor data.
- Predict traffic congestion.
- Predict waste collection requirements.
- Identify recurring infrastructure problems.
- Provide historical city-performance analytics.

---

# 4. Non-Goals

The first version will use Java 21 + Spring Boot as the core backend. The first version will not attempt to:

- Replace existing police, ambulance, fire, or municipal dispatch systems.
- Control real-world traffic signals directly.
- Guarantee emergency response times.
- Use AI as the sole authority for incident severity.
- Provide unsupported real-world sensor readings.
- Present simulated data as real data.

For demonstrations, simulated IoT data must be clearly identified as simulated.

---

# 5. Target Users

## 5.1 Command Centre Operator

Needs to:

- View active incidents.
- Monitor the city map.
- Acknowledge alerts.
- Assign or approve response actions.
- Monitor SLA timers.
- Escalate overdue incidents.

## 5.2 Municipal Department Officer

Needs to:

- View department-specific incidents.
- Accept assigned tasks.
- Update status.
- Upload resolution evidence.
- Track SLA compliance.

## 5.3 Field Worker / Response Team

Needs to:

- View assigned tasks.
- Navigate to incident locations.
- Update arrival and completion status.
- Add notes and evidence.

## 5.4 Citizen

Needs to:

- Report civic problems.
- Add location and description.
- Upload optional evidence.
- Track complaint status.
- Receive updates.

## 5.5 City Administrator

Needs to:

- View city-wide KPIs.
- Analyze department performance.
- Review SLA compliance.
- Identify recurring problem zones.
- Manage users, departments, rules, and system configuration.

---

# 6. Core Modules

## 6.1 Command Centre Dashboard

The main interface should provide:

- Live city map
- Active incident count
- Critical incident count
- SLA-at-risk incidents
- Overdue incidents
- Traffic status
- Air-quality status
- Waste status
- Emergency events
- Citizen complaint statistics
- Department workload
- Recent alerts

The dashboard should be operational rather than decorative.

---

# 7. GIS / City Map

The GIS interface is the central visual component.

## Map Layers

Users should be able to toggle:

- Roads
- Traffic signals
- Traffic congestion
- Air-quality zones
- Smart waste bins
- Hospitals
- Police stations
- Fire stations
- Emergency shelters
- Public infrastructure
- Citizen complaints
- Active incidents
- Caution zones
- Critical zones

## Map Interactions

Users can:

- Click an incident.
- View incident details.
- View nearby emergency resources.
- Filter incidents by tier.
- Filter by department.
- Filter by status.
- Filter by time.
- View incident history.
- Open navigation/routing information.

---

# 8. Incident Management

Every actionable event becomes an Incident.

## Incident Data Model

Each incident should contain:

- Incident ID
- Incident type
- Description
- Source
- Latitude
- Longitude
- Location/address
- Reported time
- Detection time
- Severity indicators
- Population exposure if available
- Infrastructure importance
- Assigned tier
- Priority label
- SLA target
- SLA deadline
- Department
- Assigned team
- Current status
- Escalation state
- Resolution time
- Resolution notes
- Evidence
- Created by
- Last updated time

---

# 9. Incident Sources

Incidents may originate from:

## Citizen Reports

Examples:

- Pothole
- Garbage dumping
- Streetlight failure
- Water leakage
- Open manhole
- Traffic problem
- Road damage

## IoT Sensors

Examples:

- Traffic sensors
- Air-quality sensors
- Smart waste bins
- Water-level sensors
- Weather sensors
- Energy/electrical sensors

## External APIs

Examples:

- Weather
- Air quality
- Mapping/geospatial data

## Official / Operator Input

Command Centre operators can manually create verified incidents.

---

# 10. Priority Classification Engine

Priority must be rule-based rather than manually or randomly assigned.

The engine evaluates:

- Incident type
- Severity
- Location
- Population exposure
- Infrastructure importance
- Sensor thresholds
- Environmental conditions
- Time sensitivity
- Safety impact

Conceptual formula:

Priority = Incident Type + Severity + Location Context + Exposure + Infrastructure Importance + Thresholds + Time Sensitivity

The actual production implementation should use deterministic rules rather than an arbitrary numerical score.

---

# 11. Priority Tiers

## Tier 1 — CRITICAL

### Definition

Life safety or imminent hazard.

### Target SLA

<= 30 minutes

### Trigger Examples

- Major road accident or collision
- Live exposed electrical wires
- Transformer spark/fire risk
- Open manhole on a busy road
- Sudden deep flash flood
- Submerged underpass

### Automated / Assisted Actions

- Notify appropriate emergency services.
- Notify rapid-response team.
- Trigger high-priority Command Centre alarm.
- Display critical incident prominently.
- Send notification to responsible officer.
- Start SLA countdown.
- Identify nearby emergency resources.
- Create an escalation path.

---

## Tier 2 — HIGH

### Definition

Severe public disruption or significant public-health/infrastructure risk.

### Target SLA

2–4 hours

### Trigger Examples

- Non-functional traffic signal at a major junction
- Main pipeline burst
- Drinking-water contamination
- Road cave-in
- Severe tire-burst pothole
- Gas leakage
- High toxic-smog cluster
- PM2.5 > 300, when the configured data source reports this threshold

### Automated / Assisted Actions

- Dispatch or recommend the nearest maintenance team.
- Mark affected GIS zone as caution.
- Notify responsible department.
- Recommend traffic/public-transport rerouting.
- Start SLA timer.
- Escalate if the incident approaches its SLA deadline.

---

## Tier 3 — MEDIUM

### Definition

Sanitation and infrastructure maintenance requiring timely action.

### Target SLA

12–24 hours

### Trigger Examples

- Waste bin >90% capacity
- Illegal roadside dumping
- Dead streetlights
- Clogged roadside storm drain
- Routine infrastructure malfunction

### Automated / Assisted Actions

- Add location to collection/maintenance route.
- Assign task to relevant municipal department or contractor.
- Group nearby tasks for route optimization.
- Track SLA.

---

## Tier 4 — LOW

### Definition

Aesthetic or routine civic work.

### Target SLA

48–72 hours

### Trigger Examples

- Minor road cracks
- Sidewalk paving disrepair
- Faded lane markings
- Missing street signage
- Tree pruning
- Park maintenance
- Graffiti removal

### Automated / Assisted Actions

- Add to routine maintenance queue.
- Batch nearby tasks.
- Recommend an optimized worker route.
- Track completion against SLA.

---

# 12. Context-Aware Priority Rules

The same incident type may receive different treatment depending on context.

Example:

A garbage bin at 95% capacity in a residential lane:

→ Tier 3

A garbage accumulation blocking an emergency access route:

→ Higher priority based on configured safety rule.

Therefore the engine must not depend only on category.

Example rule:

IF incident_type = "waste_bin"
AND fill_level > 90%
AND emergency_access_blocked = FALSE
THEN Tier 3

IF incident_type = "waste_obstruction"
AND emergency_access_blocked = TRUE
THEN Tier 2

Another example:

IF incident_type = "traffic_signal_failure"
AND junction_type = "major"
THEN Tier 2

IF incident_type = "electrical_hazard"
AND exposed = TRUE
THEN Tier 1

---

# 13. SLA Management

Every incident receives:

- Target SLA
- SLA deadline
- Remaining time
- Current status
- Escalation state

## SLA States

- ON TRACK
- AT RISK
- BREACHED
- RESOLVED

Example:

Incident:

CRITICAL

Created:
10:05 AM

SLA:
30 minutes

Deadline:
10:35 AM

At 10:25 AM:

Status → AT RISK

At 10:36 AM:

Status → BREACHED

---

# 14. Incident Lifecycle

```text
DETECTED
   ↓
CLASSIFIED
   ↓
PRIORITIZED
   ↓
ASSIGNED
   ↓
ACKNOWLEDGED
   ↓
IN PROGRESS
   ↓
RESOLVED
   ↓
VERIFIED
   ↓
CLOSED
```

Possible alternate states:

- REJECTED
- DUPLICATE
- ESCALATED
- CANCELLED

---

# 15. Department Assignment

Priority rules should be separate from department assignment.

Example mapping:

| Incident | Department |
|---|---|
| Traffic signal failure | Traffic Department |
| Garbage overflow | Sanitation |
| Streetlight failure | Electrical/Municipal |
| Water pipeline burst | Water Department |
| Road damage | Public Works |
| Fire/electrical emergency | Emergency Services |
| Air-quality event | Environment Department |
| Flooded road | Disaster/Emergency + Public Works |

The system can recommend the department while authorized operators retain control.

---

# 16. Smart Traffic Module

Features:

- Live traffic readings
- Congestion detection
- Traffic incident display
- Traffic signal status
- Average vehicle speed
- Vehicle count
- Congestion zones
- Historical traffic analytics
- Predicted congestion
- Suggested rerouting

For the prototype, traffic sensor data can be simulated.

Example:

```json
{
  "sensorId": "TRF-001",
  "location": {
    "lat": 30.70,
    "lng": 76.81
  },
  "vehicleCount": 184,
  "averageSpeed": 21,
  "timestamp": "..."
}
```

---

# 17. Smart Waste Module

Each smart bin contains:

- Bin ID
- Location
- Fill percentage
- Last collection
- Battery status
- Sensor status

Rules:

```text
Fill < 70% → Normal
70–90% → Monitor
>90% → Tier 3 collection trigger
```

The system should:

- Generate collection tasks.
- Group nearby bins.
- Optimize collection routes.
- Track missed collections.
- Display high-fill bins on GIS.

---

# 18. Air Quality Module

Data:

- AQI
- PM2.5
- PM10
- NO2
- CO
- SO2
- O3

The system should:

- Display AQI by location.
- Create GIS heat/zone visualization.
- Detect configured threshold breaches.
- Create environmental incidents.
- Show historical trends.

External source:

Open-Meteo Air Quality API.

---

# 19. Weather Module

External weather data should be integrated using an API such as Open-Meteo.

Data:

- Temperature
- Humidity
- Rain
- Wind
- Weather conditions
- Forecast

Use cases:

- Flood-risk context
- Storm alerts
- Drainage preparedness
- Heat conditions
- Emergency planning

---

# 20. Citizen Service Module

Citizen workflow:

```text
Citizen
  ↓
Submit Complaint
  ↓
Location + Category + Description + Evidence
  ↓
Backend
  ↓
Duplicate/validation check
  ↓
Priority Rule Engine
  ↓
Department Assignment
  ↓
SLA Creation
  ↓
Department Action
  ↓
Resolution
  ↓
Citizen Notification
```

Citizen should be able to see:

- Complaint ID
- Status
- Assigned department
- Priority category
- Expected SLA
- Resolution update

---

# 21. AI / Analytics Layer

AI should enhance the system rather than replace deterministic safety rules.

Potential ML features:

## Traffic Prediction

Predict congestion from:

- Historical traffic
- Time of day
- Day of week
- Weather
- Events

## Waste Prediction

Predict when bins are likely to exceed capacity.

## Anomaly Detection

Identify unusual sensor behavior.

## Hotspot Analysis

Identify areas with recurring:

- Complaints
- Traffic problems
- Waste overflow
- Infrastructure failures

## Important Rule

AI predictions should be clearly labelled as predictions/recommendations.

Safety-critical priority classification remains governed by explicit rules and configured thresholds.

---

# 22. Technology Stack

## Frontend

- React
- Vite
- JavaScript/TypeScript
- CSS
- Axios
- React Router
- Recharts
- Leaflet or MapLibre

## Backend — Java

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Data MongoDB
- Spring Security
- JWT
- WebSocket
- Bean Validation

## 22A. Java Backend Architecture

The core backend will be implemented entirely in Java using Spring Boot.

### Java Responsibilities

The Java backend will handle:

- REST API endpoints
- Authentication and authorization
- Incident management
- Priority rule evaluation
- SLA calculation and monitoring
- Department assignment
- Citizen complaints
- IoT event ingestion
- MQTT integration
- External API integration
- GIS queries
- Real-time WebSocket updates
- Notifications
- Audit logging
- Analytics APIs
- Administrative configuration

### Recommended Java Package Structure

```text
src/main/java/com/smartcity/
│
├── auth/
├── user/
├── incident/
├── priority/
├── sla/
├── department/
├── complaint/
├── traffic/
├── waste/
├── airquality/
├── weather/
├── gis/
├── iot/
├── notification/
├── analytics/
├── websocket/
├── config/
└── common/
```

### Main Spring Boot Components

```text
Controller
    ↓
Service
    ↓
Rule / Business Logic
    ↓
Repository
    ↓
Database
```

Example:

```text
IncidentController
       ↓
IncidentService
       ↓
PriorityEngine + SLAManager
       ↓
IncidentRepository
       ↓
PostgreSQL
```

The Java backend is the system's source of truth for incident state, priority, SLA, assignment, and workflow.

## Databases

### PostgreSQL + PostGIS

For:

- Users
- Departments
- Infrastructure
- Complaints
- Incidents
- Geographic relationships
- Structured city data

### MongoDB

For:

- Sensor readings
- IoT events
- Logs
- Semi-structured environmental data
- High-volume event records

### Redis

For:

- Caching
- Temporary state
- Frequently accessed dashboard information

---

# 23. IoT Stack

For prototype/hardware integration:

- ESP32
- Arduino
- MQTT
- Mosquitto MQTT Broker

Architecture:

```text
ESP32 / Simulator
       ↓
     MQTT
       ↓
Mosquitto
       ↓
Spring Boot
       ↓
MongoDB
       ↓
WebSocket
       ↓
React Dashboard
```

---

# 24. GIS Stack

- OpenStreetMap
- Leaflet or MapLibre
- PostGIS
- Geocoding service
- Routing service

GIS responsibilities:

- Display incidents.
- Display infrastructure.
- Calculate proximity.
- Create caution zones.
- Identify nearby response resources.
- Support route planning.

---

# 25. External APIs

## Open-Meteo

Use for:

- Weather
- Forecast
- Air quality

## OpenStreetMap

Use for:

- Base map
- Road/network information

Additional routing/geocoding services can be added if required.

API keys and rate limits must be respected where applicable.

---

# 26. REST API Structure

```text
/api/auth
/api/users

/api/incidents
/api/incidents/{id}
/api/incidents/{id}/assign
/api/incidents/{id}/status
/api/incidents/{id}/escalate

/api/priority/rules
/api/priority/evaluate

/api/traffic
/api/traffic/readings
/api/traffic/incidents

/api/waste
/api/waste/bins
/api/waste/collections

/api/air-quality
/api/air-quality/readings

/api/weather
/api/weather/current
/api/weather/forecast

/api/complaints
/api/complaints/{id}

/api/departments
/api/teams

/api/alerts

/api/analytics
/api/analytics/traffic
/api/analytics/waste
/api/analytics/incidents

/api/gis
/api/gis/locations
```

---

# 27. Example Priority API

Request:

```http
POST /api/priority/evaluate
```

```json
{
  "incidentType": "traffic_signal_failure",
  "junctionType": "major",
  "location": {
    "lat": 30.7046,
    "lng": 76.7179
  },
  "publicExposure": "HIGH"
}
```

Response:

```json
{
  "tier": 2,
  "priority": "HIGH",
  "slaMinutes": 240,
  "reason": [
    "Major junction",
    "Traffic signal failure",
    "High public exposure"
  ],
  "recommendedDepartment": "Traffic Department",
  "recommendedActions": [
    "Dispatch maintenance team",
    "Mark GIS zone as caution",
    "Recommend traffic rerouting"
  ]
}
```

The response should always provide an explanation for the classification.

---

# 28. WebSocket / Real-Time Events

WebSocket channels can provide:

```text
/incidents
/alerts
/traffic
/air-quality
/waste
/dashboard
```

Example:

```json
{
  "event": "INCIDENT_CREATED",
  "incidentId": "INC-1042",
  "tier": 1,
  "priority": "CRITICAL",
  "location": {
    "lat": 30.70,
    "lng": 76.81
  }
}
```

The dashboard updates immediately.

---

# 29. Authentication and Authorization

Use Spring Security + JWT.

Roles:

```text
ADMIN
COMMAND_OPERATOR
DEPARTMENT_OFFICER
FIELD_WORKER
CITIZEN
```

Permissions must be role-based.

Example:

- Citizen → create/view own complaints
- Field worker → view assigned tasks
- Department officer → manage department incidents
- Command operator → manage city incidents
- Admin → manage configuration/users/rules

---

# 30. Database Concept

Core PostgreSQL tables:

```text
users
roles
departments
teams
incidents
incident_status_history
priority_rules
sla_records
complaints
infrastructure
locations
traffic_signals
waste_bins
emergency_resources
notifications
audit_logs
```

MongoDB collections:

```text
sensor_readings
traffic_readings
air_quality_readings
weather_observations
iot_events
system_events
```

---

# 31. Priority Rule Data Model

Rules should be configurable rather than hard-coded wherever practical.

Example:

```json
{
  "ruleId": "PR-001",
  "incidentType": "electrical_hazard",
  "conditions": {
    "exposed": true
  },
  "tier": 1,
  "slaMinutes": 30,
  "department": "Emergency Services",
  "actions": [
    "COMMAND_CENTER_ALARM",
    "EMERGENCY_NOTIFICATION",
    "CREATE_RESPONSE_TASK"
  ],
  "enabled": true
}
```

This allows authorized administrators to modify rules without rewriting the entire application.

---

# 32. Automated Response Engine

After classification:

```text
Incident
   ↓
Priority Engine
   ↓
Response Policy Engine
   ↓
Recommended / Authorized Actions
```

Actions can include:

- Notify department
- Notify emergency response
- Start SLA
- Create work order
- Mark GIS caution zone
- Recommend route change
- Add task to route optimizer
- Notify citizen
- Escalate
- Trigger dashboard alarm

Actions involving real-world emergency dispatch should require appropriate integration and authorization in a production system.

---

# 33. Route Optimization

The system should group nearby tasks.

Example:

```text
Task A → Sector 22
Task B → Sector 23
Task C → Sector 22
Task D → Sector 24
```

Instead of four separate trips:

```text
Route Optimizer
      ↓
A → C → B → D
```

Potential optimization factors:

- Distance
- Priority
- SLA deadline
- Vehicle capacity
- Task type
- Road availability

---

# 34. Command Centre Screens

## Screen 1 — Overview

- City map
- KPI cards
- Active incidents
- Critical alerts
- Traffic
- AQI
- Waste

## Screen 2 — Incident Centre

- Incident table
- Tier
- SLA
- Department
- Status
- Location
- Filters

## Screen 3 — GIS Operations

- Full-screen map
- Layers
- Incident details
- Nearby resources
- Zones

## Screen 4 — Traffic

- Congestion map
- Traffic sensors
- Signal status
- Trends
- Prediction

## Screen 5 — Waste

- Bin map
- Fill levels
- Collection tasks
- Route optimization

## Screen 6 — Environment

- AQI map
- Sensor readings
- Weather
- Trends
- Threshold alerts

## Screen 7 — Citizen Services

- Complaints
- Status
- Department
- SLA
- Resolution

## Screen 8 — Analytics

- SLA compliance
- Incident trends
- Department performance
- Recurring hotspots
- Traffic trends
- Waste trends

## Screen 9 — Administration

- Users
- Departments
- Priority rules
- SLA configuration
- Sensor management

---

# 35. Key Dashboard KPIs

The dashboard should display actual calculated values rather than fake counters.

Examples:

- Active incidents
- Critical incidents
- Incidents at risk of SLA breach
- SLA compliance %
- Average resolution time
- Open citizen complaints
- Traffic congestion index
- Average AQI
- Waste bins above threshold
- Active emergency events

If no real data exists, the UI must clearly identify demo/simulated data.

---

# 36. Example End-to-End Scenario

## Scenario: Major Road Accident

1. Traffic sensor detects abnormal vehicle behavior.
2. Operator or integrated source creates incident.
3. GIS identifies location.
4. Rule engine identifies:
   - Major accident
   - High public exposure
   - Major road
5. System assigns:

```text
Tier: 1
Priority: CRITICAL
SLA: <=30 minutes
```

6. Command Centre alarm activates.
7. Police/ambulance response notification is generated where integrated.
8. Nearby hospitals and emergency resources are displayed.
9. GIS marks the affected location.
10. Traffic module identifies congestion.
11. Rerouting recommendation is generated.
12. SLA timer starts.
13. Response team acknowledges.
14. Incident becomes IN PROGRESS.
15. Field team resolves the incident.
16. Operator verifies resolution.
17. Incident becomes CLOSED.
18. Resolution time is stored for analytics.

---

# 37. Example End-to-End Scenario: Waste Overflow

1. Smart bin reports 93% capacity.
2. Backend receives MQTT event.
3. Waste rule evaluates threshold.
4. Incident created.
5. Classification:

```text
Tier: 3
Priority: MEDIUM
SLA: 12–24 hours
```

6. Nearby waste tasks are identified.
7. Collection route is optimized.
8. Sanitation team receives task.
9. Bin is emptied.
10. Sensor reports normal capacity.
11. Task is marked resolved.
12. SLA performance is recorded.

---

# 38. Security Requirements

- HTTPS in production.
- JWT authentication.
- Role-based authorization.
- Password hashing.
- Input validation.
- API rate limiting where required.
- Audit logs.
- Secure API credentials.
- No secrets committed to GitHub.
- Environment variables for configuration.
- Database access restrictions.

---

# 39. Observability

Use:

- Application logs
- Audit logs
- Error tracking
- API health checks
- Sensor connectivity status

Useful endpoints:

```text
/actuator/health
/actuator/metrics
```

Spring Boot Actuator can be used for backend monitoring.

---

# 40. Deployment Architecture

Prototype:

```text
Browser
   ↓
React
   ↓
Spring Boot
   ↓
PostgreSQL / MongoDB
```

With IoT:

```text
IoT Simulator / ESP32
          ↓
       MQTT
          ↓
      Mosquitto
          ↓
      Spring Boot
          ↓
  PostgreSQL + MongoDB
          ↓
      React Dashboard
```

Docker can package:

- Frontend
- Backend
- PostgreSQL
- MongoDB
- Redis
- Mosquitto

---

# 41. Recommended Development Phases

## Phase 1 — Foundation

- React dashboard
- Spring Boot backend
- PostgreSQL
- Authentication
- Basic incident CRUD

## Phase 2 — GIS

- Map integration
- Incident markers
- Infrastructure markers
- Location filtering
- PostGIS

## Phase 3 — Priority Engine

- Tier 1–4 rules
- SLA calculation
- Rule explanation
- Department mapping
- Escalation

## Phase 4 — IoT

- MQTT
- Mosquitto
- Simulated traffic sensors
- Waste sensors
- Air-quality sensors

## Phase 5 — External APIs

- Weather
- Air quality
- Mapping/geospatial data

## Phase 6 — Real-Time

- WebSockets
- Live alerts
- Live dashboard updates

## Phase 7 — Citizen Portal

- Complaint submission
- Tracking
- Notifications
- Evidence

## Phase 8 — Analytics / ML

- Traffic prediction
- Waste prediction
- Anomaly detection
- Hotspot analytics

## Phase 9 — Optimization

- Route optimization
- SLA escalation
- Performance dashboards

---

# 42. MVP

The minimum viable version should contain:

1. React Command Centre
2. Spring Boot backend
3. PostgreSQL + PostGIS
4. MongoDB
5. GIS map
6. Incident management
7. Rule-based Tier 1–4 priority engine
8. SLA tracking
9. Department assignment
10. Citizen complaint system
11. Simulated IoT sensors
12. MQTT
13. Open-Meteo integration
14. WebSocket real-time updates
15. Basic analytics

---

# 43. Future Enhancements

- Computer vision for traffic incidents
- CCTV event detection
- Digital twin of city infrastructure
- Advanced ML forecasting
- Mobile application
- Voice-based citizen reporting
- Predictive maintenance
- Advanced fleet management
- Drone integration
- Smart energy monitoring
- Automated emergency coordination
- Multi-city deployment

---

# 44. Success Criteria

The prototype is successful when the Java/Spring Boot backend successfully orchestrates the complete incident lifecycle and the following conditions are met:

- A citizen can submit a complaint.
- The system validates and creates an incident.
- The priority engine automatically assigns a documented tier.
- The system provides a reason for that tier.
- An SLA is automatically generated.
- The correct department is identified.
- The incident appears on the GIS map.
- Operators can update its status.
- SLA timers update correctly.
- Critical incidents generate prominent alerts.
- IoT data can create/update city events.
- External weather/air-quality data is displayed.
- Dashboard updates can happen in real time.
- Resolved incidents are reflected in analytics.
- No priority is randomly assigned.

---

# 45. Final System Concept

```text
                 SMART CITY
              COMMAND CENTRE
                     │
      ┌──────────────┼──────────────┐
      │              │              │
     IoT           APIs          Citizens
      │              │              │
      └──────────────┼──────────────┘
                     ▼
              DATA INGESTION
                     │
                     ▼
              INCIDENT ENGINE
                     │
                     ▼
          PRIORITY RULE ENGINE
                     │
       ┌─────────────┼─────────────┐
       ▼             ▼             ▼
    Tier 1         Tier 2        Tier 3/4
   Critical         High        Medium/Low
       │             │             │
       └─────────────┼─────────────┘
                     ▼
              SLA MANAGEMENT
                     │
                     ▼
            DEPARTMENT ASSIGNMENT
                     │
                     ▼
             RESPONSE ENGINE
                     │
       ┌─────────────┼─────────────┐
       ▼             ▼             ▼
    Alerts         Routing       Tasks
       │             │             │
       └─────────────┼─────────────┘
                     ▼
              GIS COMMAND MAP
                     │
                     ▼
              LIVE DASHBOARD
                     │
                     ▼
                ANALYTICS
                     │
                     ▼
            BETTER CITY OPERATIONS
```

---

# 46. Project One-Line Definition

> A GIS-enabled, real-time Smart City Command Centre that integrates IoT, APIs, citizen reports, and city data to detect incidents, classify them through transparent rule-based priority tiers, enforce SLAs, coordinate responses, and provide actionable urban analytics.

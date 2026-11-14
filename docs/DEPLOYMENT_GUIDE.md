# CivicPulse Smart City Command Centre — Setup & Deployment Guide

## 1. System Requirements

### Hardware Prerequisites
- **CPU**: 4 Cores minimum (Apple Silicon M-series, Intel Core i5/i7, or AMD Ryzen).
- **RAM**: 8 GB minimum (16 GB recommended for full Docker multi-service stack).
- **Disk**: 10 GB free space for source code, dependencies, and container images.

### Software Prerequisites
- **Operating System**: macOS, Linux (Ubuntu 22.04+), or Windows 11 with WSL2.
- **Java**: Java Development Kit (JDK) 21 LTS (Eclipse Temurin 21 recommended).
- **Node.js**: Node.js 18 LTS or 20 LTS with `npm` 9+.
- **Build Tool**: Apache Maven 3.9+.
- **Database Servers** (if running natively without Docker):
  - PostgreSQL 16 with PostGIS 3.4 extension.
  - MongoDB 7.0 Community Edition.
  - Redis 7.2.
  - Eclipse Mosquitto 2.0 MQTT broker.
- **Or Docker**: Docker Desktop 4.25+ with Docker Compose v2.

---

## 2. Environment Configuration

1. Copy the example configuration template:
   ```bash
   cp .env.example .env
   ```
2. Inspect `.env` and verify local development settings:
   ```env
   # Core Services
   SERVER_PORT=8080
   FRONTEND_PORT=5173
   SPRING_PROFILES_ACTIVE=dev

   # PostgreSQL
   DATABASE_URL=jdbc:postgresql://localhost:5432/civicpulse_db
   DATABASE_USERNAME=civicpulse
   DATABASE_PASSWORD=civicpulse_secret

   # MongoDB
   MONGO_URI=mongodb://localhost:27017/civicpulse

   # Redis
   REDIS_HOST=localhost
   REDIS_PORT=6379

   # MQTT
   MQTT_BROKER_URL=tcp://localhost:1883
   MQTT_ENABLED=true

   # SMS Provider: use mock for local demonstration (prints OTP to backend/demo-otp.log)
   SMARTCITY_SMS_PROVIDER=mock
   ```

---

## 3. Database Initialization (Flyway Automatic Migration)

When Spring Boot starts, Flyway automatically discovers and applies all migration scripts located in `backend/src/main/resources/db/migration/` in sequential order:
- `V1` to `V8`: Core relational schemas, spatial extensions, priority rules, notifications, and GIS infrastructure.
- `V9` to `V13`: Auth enhancements, incident schemas, SLA policies, and map features.
- `V14` to `V16`: Multi-role demo associations, verified citizen reputation engine, and unified complaint lifecycle with photo attachments.

If setting up PostgreSQL manually for the first time:
```sql
CREATE USER civicpulse WITH PASSWORD 'civicpulse_secret';
CREATE DATABASE civicpulse_db OWNER civicpulse;
\c civicpulse_db;
CREATE EXTENSION IF NOT EXISTS postgis;
```

---

## 4. Local Development Execution

### 4.1 Running Infrastructure via Docker Compose (Recommended)
You can run the databases and broker in Docker while running backend and frontend locally:
```bash
docker compose up -d postgres mongodb redis mosquitto
```

### 4.2 Running the Spring Boot Backend
```bash
cd backend
mvn spring-boot:run
```
The backend initializes on `http://localhost:8080`.
Verify backend health:
```bash
curl http://localhost:8080/actuator/health
```
Expected response:
```json
{"status": "UP", "components": {"db": {"status": "UP"}, "mongo": {"status": "UP"}, "redis": {"status": "UP"}, "mqtt": {"status": "UP"}}}
```

### 4.3 Running the React Frontend
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
The frontend UI will start on `http://localhost:5173`.

---

## 5. Full Containerized Deployment (Docker Compose)

To build and run all 6 microservices (Postgres, MongoDB, Redis, Mosquitto, Spring Boot backend, and Nginx frontend) simultaneously:
```bash
docker compose up -d --build
```
Access points:
- **Web Frontend**: `http://localhost` (Port 80)
- **Backend API**: `http://localhost:8080/api`
- **Health Diagnostics**: `http://localhost:8080/actuator/health`

---

## 6. Default Demonstration Credentials

All standard demonstration accounts share the default password: **`Admin@CivicPulse2026`**

| Role | Username | Mobile Number | Intended Purpose |
|---|---|---|---|
| `ADMIN` | `admin` | `+919876543210` | Full command console, RBAC, departmental matrix, diagnostics |
| `COMMAND_OPERATOR` | `operator` | `+919876543211` | Real-time incident triage, live GIS map, IoT grid, dispatch |
| `DEPARTMENT_OFFICER` | `officer_traffic` | `+919876543212` | Traffic Department queue, worker assignment, verification |
| `FIELD_WORKER` | `field_worker` | `+919876543213` | Mobile work queue, progress updates, photo resolution proof |
| `CITIZEN` | `citizen` | `+919876543210` | Citizen grievance lodging, receipt downloads, Verified Citizen badge |

### Testing Citizen Mobile OTP Login
1. On the login screen, click **Citizen Mobile OTP Login**.
2. Enter mobile number: `9876543210`.
3. Click **Send Verification Code**.
4. With `SMARTCITY_SMS_PROVIDER=mock`, inspect `backend/demo-otp.log` to view the generated OTP.
5. Enter the OTP code to log in directly into the Citizen Portal.

---

## 7. Testing Instructions

### Backend Unit & Integration Tests
```bash
cd backend
mvn test
```

### Frontend Production Build Test
```bash
cd frontend
npm run build
```
Verify the build generates minified production assets in `frontend/dist/` without errors.

---

## 8. Troubleshooting & Common Questions

1. **Port 8080 is already in use**:
   Find the occupying PID and terminate it:
   ```bash
   lsof -i :8080
   kill -9 <PID>
   ```
2. **PostgreSQL Socket Connection Error (`Operation not permitted`)**:
   Verify PostgreSQL service is listening on `127.0.0.1:5432` and `pg_hba.conf` allows MD5/scram-sha-256 password authentication.
3. **Vite Dev Server Proxy Connection Refused**:
   Ensure Spring Boot is started and healthy on port 8080 before interacting with the frontend UI.

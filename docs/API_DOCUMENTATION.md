# CivicPulse Smart City Command Centre — REST API Documentation

Base URL: `http://localhost:8080/api`

All authenticated endpoints require an HTTP `Authorization` header:
```http
Authorization: Bearer <jwt-token>
```

---

## 1. Authentication & Identity (`/api/auth`)

| Method | Endpoint | Description | Access | Request Body | Response |
|---|---|---|---|---|---|
| `POST` | `/auth/login` | Username + password authentication | Public | `{"username": "...", "password": "..."}` | `AuthResponse` (token, user details, role) |
| `POST` | `/auth/register` | Citizen account registration | Public | `RegisterRequest` (username, email, password, mobile) | `AuthResponse` |
| `POST` | `/auth/otp/send` | Request 6-digit OTP for mobile login | Public | `{"mobileNumber": "9876543210"}` | `{"message": "OTP sent successfully"}` |
| `POST` | `/auth/otp/login` | Mobile number + OTP citizen login | Public | `{"mobileNumber": "9876543210", "otp": "123456"}` | `AuthResponse` |
| `GET` | `/auth/me` | Fetch authenticated session profile | Authenticated | None | `UserSummaryResponse` |

---

## 2. Citizen Grievance Management (`/api/complaints`)

| Method | Endpoint | Description | Access | Parameters / Body | Response |
|---|---|---|---|---|---|
| `POST` | `/complaints` | File grievance with photo evidence | `CITIZEN` only | `multipart/form-data`: `title`, `description`, `category`, `address`, `latitude`, `longitude`, `files` (array) | `ComplaintResponse` (201 Created) |
| `POST` | `/complaints` | File grievance via JSON payload | `CITIZEN` only | `application/json`: `CreateComplaintRequest` | `ComplaintResponse` (201 Created) |
| `GET` | `/complaints` | List complaints with multi-criteria filtering | Authenticated (scoped by RBAC) | Query params: `status`, `category`, `verificationStatus`, `priority`, `slaStatus`, `unassigned`, `departmentId`, `search`, `page`, `size` | `PagedResponse<ComplaintResponse>` |
| `GET` | `/complaints/{id}` | Get full complaint details by ID | Authenticated (scoped by RBAC) | Path: `id` (UUID) | `ComplaintResponse` |
| `PATCH` | `/complaints/{id}/assign` | Assign worker to complaint | `ADMIN`, `COMMAND_OPERATOR`, `DEPARTMENT_OFFICER` | `{"assignedToUserId": "UUID", "notes": "..."}` | `ComplaintResponse` |
| `GET` | `/complaints/assignees` | List eligible field workers for department | `ADMIN`, `COMMAND_OPERATOR`, `DEPARTMENT_OFFICER` | Query: `departmentId` (optional, falls back to city pool) | `List<UserSummaryResponse>` |
| `PATCH` | `/complaints/{id}/progress` | Update work progress with photos | `ADMIN`, `DEPARTMENT_OFFICER`, `FIELD_WORKER` | `multipart/form-data`: `status`, `progressNotes`, `files` | `ComplaintResponse` |
| `POST` | `/complaints/{id}/resolution` | Submit work resolution with photos | `ADMIN`, `DEPARTMENT_OFFICER`, `FIELD_WORKER` | `multipart/form-data`: `resolutionNotes`, `files` | `ComplaintResponse` |
| `PATCH` | `/complaints/{id}/verify-resolution` | Verify submitted resolution | `ADMIN`, `DEPARTMENT_OFFICER` | `{"decision": "ACCEPTED"\|"REJECTED", "notes": "..."}` | `ComplaintResponse` |
| `PATCH` | `/complaints/{id}/verify` | Verify citizen report validity | `ADMIN`, `DEPARTMENT_OFFICER` | `{"status": "GENUINE"\|"REJECTED_SPAM", "notes": "..."}` | `ComplaintResponse` |
| `PATCH` | `/complaints/{id}/priority` | Re-prioritize complaint & update SLA | `ADMIN`, `COMMAND_OPERATOR` | `{"priority": "CRITICAL"\|"HIGH"\|"MEDIUM"\|"LOW", "reason": "..."}` | `ComplaintResponse` |
| `PATCH` | `/complaints/{id}/close` | Formally close or reopen grievance | `ADMIN`, `COMMAND_OPERATOR`, `CITIZEN` (owner) | `{"action": "CLOSE"\|"REOPEN", "reason": "..."}` | `ComplaintResponse` |
| `GET` | `/complaints/{id}/receipt` | Get digital receipt metadata | Authenticated | Path: `id` | `ComplaintReceiptDTO` |
| `GET` | `/complaints/{id}/receipt/pdf` | Download official acknowledgement receipt text/PDF | Authenticated | Path: `id` | `byte[]` (`text/plain` file download) |
| `GET` | `/complaints/admin/stats` | City-wide grievance metrics | `ADMIN`, `COMMAND_OPERATOR` | None | `AdminComplaintStatsDTO` |
| `GET` | `/complaints/department/stats` | Department-specific queue metrics | `ADMIN`, `DEPARTMENT_OFFICER` | Query: `departmentId` (optional) | `DepartmentComplaintStatsDTO` |
| `GET` | `/complaints/department/stats/all` | Performance matrix for all 8 departments | `ADMIN`, `COMMAND_OPERATOR` | None | `List<DepartmentComplaintStatsDTO>` |
| `GET` | `/complaints/reputation/me` | Current citizen reputation score & badge | Authenticated | None | `CitizenReputationResponse` |
| `GET` | `/complaints/attachments/{attachmentId}` | Stream evidence image / document | Authenticated | Path: `attachmentId` | File stream (`image/png`, `image/jpeg`) |

---

## 3. SLA & Escalation Engine (`/api/sla`)

| Method | Endpoint | Description | Access | Response |
|---|---|---|---|---|
| `GET` | `/sla/overview` | Active, warning, breached, and completed counts | Authenticated | `SlaOverviewDTO` |
| `GET` | `/sla/active` | Live uncompleted SLA compliance records | Authenticated | `List<SLARecordDTO>` |
| `GET` | `/sla/breached` | Breached SLA compliance records | Authenticated | `List<SLARecordDTO>` |
| `GET` | `/sla/at-risk` | Approaching deadline SLA records ($\le 20\%$ time left) | Authenticated | `List<SLARecordDTO>` |
| `GET` | `/sla/policies` | Active municipal SLA target rules | Authenticated | `List<SLAPolicy>` |
| `POST` | `/sla/policies` | Create custom SLA policy | `ADMIN` only | `SLAPolicy` |
| `PUT` | `/sla/policies/{id}` | Update SLA policy parameters | `ADMIN` only | `SLAPolicy` |
| `GET` | `/sla/escalations` | Multi-tier escalation audit logs | Authenticated | `List<SLAEscalationLog>` |

---

## 4. Municipal Incidents (`/api/incidents`)

| Method | Endpoint | Description | Access | Response |
|---|---|---|---|---|
| `GET` | `/incidents` | Query municipal incidents with filtering | Authenticated | `PagedResponse<IncidentResponse>` |
| `GET` | `/incidents/{id}` | Get incident details, responders, and SOPs | Authenticated | `IncidentResponse` |
| `POST` | `/incidents` | Manually raise emergency incident | `ADMIN`, `COMMAND_OPERATOR` | `IncidentResponse` |
| `PATCH` | `/incidents/{id}/status` | Transition incident lifecycle status | `ADMIN`, `COMMAND_OPERATOR`, `DEPARTMENT_OFFICER` | `IncidentResponse` |
| `POST` | `/incidents/{id}/assign` | Dispatch emergency responder crew | `ADMIN`, `COMMAND_OPERATOR` | `IncidentResponse` |

---

## 5. IoT Telemetry & Environmental Sensors (`/api/sensors`)

| Method | Endpoint | Description | Access | Response |
|---|---|---|---|---|
| `GET` | `/sensors` | List active IoT telemetry sensors | Authenticated | `List<SensorResponse>` |
| `GET` | `/sensors/{id}/history` | Retrieve time-series readings from MongoDB | Authenticated | `List<SensorReading>` |
| `POST` | `/sensors/{id}/simulate` | Inject simulated reading for test | `ADMIN`, `COMMAND_OPERATOR` | `{"status": "published"}` |

---

## 6. City Analytics & System Health

| Method | Endpoint | Description | Access | Response |
|---|---|---|---|---|
| `GET` | `/analytics/overview` | Central analytics KPIs & department stats | Authenticated | `AnalyticsOverviewDTO` |
| `GET` | `/actuator/health` | Comprehensive infrastructure health check | Public | `{"status": "UP", "components": {"db": ..., "mongo": ..., "redis": ..., "mqtt": ...}}` |

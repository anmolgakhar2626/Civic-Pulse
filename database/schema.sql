--
-- PostgreSQL database dump
--

\restrict 3dbaDSiurBIGNcWpGoDczaXWsMspchj2E1TbW3bw1fze8g6UrcDuRwCE7G6TRhG

-- Dumped from database version 16.15 (Homebrew)
-- Dumped by pg_dump version 16.15 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


--
-- Name: cube; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS cube WITH SCHEMA public;


--
-- Name: EXTENSION cube; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION cube IS 'data type for multidimensional cubes';


--
-- Name: earthdistance; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS earthdistance WITH SCHEMA public;


--
-- Name: EXTENSION earthdistance; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION earthdistance IS 'calculate great-circle distances on the surface of the Earth';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: calculate_distance_meters(double precision, double precision, double precision, double precision); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.calculate_distance_meters(lat1 double precision, lon1 double precision, lat2 double precision, lon2 double precision) RETURNS double precision
    LANGUAGE plpgsql IMMUTABLE
    AS $$
BEGIN
    RETURN earth_distance(ll_to_earth(lat1, lon1), ll_to_earth(lat2, lon2));
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: acknowledgement_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acknowledgement_documents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    external_grievance_id uuid NOT NULL,
    document_reference character varying(255) NOT NULL,
    document_type character varying(50) DEFAULT 'ACKNOWLEDGEMENT_RECEIPT'::character varying NOT NULL,
    document_title character varying(200) NOT NULL,
    document_url character varying(500),
    issued_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    file_size_bytes bigint DEFAULT 0
);


--
-- Name: assignment_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assignment_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_type character varying(100) NOT NULL,
    department_code character varying(50) NOT NULL,
    department_name character varying(150) NOT NULL,
    default_team_code character varying(50),
    description text,
    priority_override character varying(50),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type character varying(100) NOT NULL,
    entity_id character varying(100),
    action character varying(100) NOT NULL,
    performed_by character varying(100) NOT NULL,
    details_json text,
    ip_address character varying(50),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: complaint_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.complaint_attachments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    complaint_id uuid NOT NULL,
    file_name character varying(255) NOT NULL,
    original_file_name character varying(255) NOT NULL,
    file_path character varying(500) NOT NULL,
    file_size bigint NOT NULL,
    content_type character varying(100) NOT NULL,
    attachment_type character varying(50) DEFAULT 'CITIZEN_EVIDENCE'::character varying NOT NULL,
    uploaded_by uuid,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: complaint_receipts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.complaint_receipts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    complaint_id uuid NOT NULL,
    receipt_number character varying(100) NOT NULL,
    complaint_record_number character varying(50) NOT NULL,
    citizen_name character varying(150) NOT NULL,
    citizen_phone character varying(50),
    title character varying(200),
    category character varying(100),
    department_name character varying(150),
    location_address character varying(255),
    priority character varying(20),
    status character varying(50),
    sla_deadline timestamp with time zone,
    expected_resolution_hours integer,
    acknowledgement_message text,
    generated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    metadata_json text
);


--
-- Name: complaint_record_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.complaint_record_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: complaint_status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.complaint_status_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    complaint_id uuid NOT NULL,
    previous_status character varying(50),
    new_status character varying(50) NOT NULL,
    changed_by uuid,
    changed_by_name character varying(150),
    changed_by_role character varying(50),
    change_reason character varying(255),
    remarks text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: complaint_verification_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.complaint_verification_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    complaint_id uuid NOT NULL,
    citizen_id uuid,
    previous_status character varying(50),
    new_status character varying(50) NOT NULL,
    decision_reason text,
    verified_by uuid NOT NULL,
    verified_by_name character varying(150),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: complaints; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.complaints (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    complaint_number character varying(50) NOT NULL,
    citizen_id uuid,
    citizen_name character varying(150) NOT NULL,
    citizen_phone character varying(50),
    category character varying(100) NOT NULL,
    description text NOT NULL,
    latitude double precision,
    longitude double precision,
    address character varying(255),
    status character varying(50) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    incident_id uuid,
    evidence_url character varying(500),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    title character varying(200),
    complaint_verification_status character varying(50) DEFAULT 'UNVERIFIED'::character varying,
    verified_by uuid,
    verified_at timestamp with time zone,
    verification_notes text,
    subcategory character varying(100),
    department_id uuid,
    assigned_to_user_id uuid,
    priority character varying(20) DEFAULT 'MEDIUM'::character varying,
    sla_minutes integer DEFAULT 1440,
    sla_deadline timestamp with time zone,
    sla_status character varying(50) DEFAULT 'ON_TRACK'::character varying,
    resolution_notes text,
    resolution_evidence_url character varying(500),
    resolved_at timestamp with time zone,
    closed_at timestamp with time zone
);


--
-- Name: departments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.departments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    contact_email character varying(150),
    contact_phone character varying(50),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: emergency_resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.emergency_resources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(150) NOT NULL,
    resource_type character varying(100) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    address character varying(255),
    contact_number character varying(50),
    available boolean DEFAULT true NOT NULL,
    capacity integer DEFAULT 0,
    operational_hours character varying(100) DEFAULT '24/7'::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: external_grievances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.external_grievances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    complaint_id uuid NOT NULL,
    portal_id uuid NOT NULL,
    portal_code character varying(50) NOT NULL,
    acknowledgement_number character varying(100) NOT NULL,
    idempotency_key character varying(100) NOT NULL,
    external_status character varying(50) DEFAULT 'ACKNOWLEDGED'::character varying NOT NULL,
    status_description text,
    officer_in_charge character varying(150),
    target_resolution_date timestamp with time zone,
    submitted_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_synced_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: government_portals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.government_portals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    portal_code character varying(50) NOT NULL,
    portal_name character varying(150) NOT NULL,
    endpoint_url character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_sandbox boolean DEFAULT true NOT NULL,
    auth_type character varying(50) DEFAULT 'API_KEY'::character varying NOT NULL,
    config_metadata text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: grievance_status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grievance_status_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    external_grievance_id uuid NOT NULL,
    status character varying(50) NOT NULL,
    remarks text,
    action_taken_by character varying(150),
    event_timestamp timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    raw_payload text
);


--
-- Name: grievance_sync_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.grievance_sync_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    external_grievance_id uuid,
    portal_code character varying(50) NOT NULL,
    sync_type character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    idempotency_key character varying(100),
    request_payload text,
    response_payload text,
    error_message text,
    retry_count integer DEFAULT 0 NOT NULL,
    sync_timestamp timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: incident_assignment_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incident_assignment_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_id uuid NOT NULL,
    assigned_by character varying(100) NOT NULL,
    previous_department_id uuid,
    new_department_id uuid,
    previous_team_id uuid,
    new_team_id uuid,
    assignment_reason text,
    assigned_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: incident_status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incident_status_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_id uuid NOT NULL,
    from_status character varying(50),
    to_status character varying(50) NOT NULL,
    changed_by_user_id uuid,
    changed_by_username character varying(100),
    notes text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    changed_by character varying(100),
    remarks text,
    changed_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: incidents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.incidents (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_number character varying(50) DEFAULT ((('INC-'::text || to_char(CURRENT_TIMESTAMP, 'YYYYMMDD'::text)) || '-'::text) || "substring"((gen_random_uuid())::text, 1, 8)) NOT NULL,
    incident_type character varying(100),
    description text NOT NULL,
    source character varying(50) DEFAULT 'CITIZEN'::character varying NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    address character varying(255),
    location_id uuid,
    reported_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    detection_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    severity_indicator character varying(50),
    population_exposure character varying(50),
    infrastructure_importance character varying(50),
    assigned_tier character varying(20),
    priority character varying(20),
    sla_minutes integer,
    sla_deadline timestamp with time zone,
    department_id uuid,
    assigned_team_id uuid,
    current_status character varying(50) DEFAULT 'OPEN'::character varying NOT NULL,
    escalation_state character varying(50) DEFAULT 'NONE'::character varying NOT NULL,
    resolution_time timestamp with time zone,
    resolution_notes text,
    evidence_url character varying(500),
    reason_notes text,
    created_by_user_id uuid,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    priority_tier character varying(20),
    priority_label character varying(50),
    priority_reason text,
    severity character varying(50),
    public_exposure character varying(50),
    safety_impact character varying(100),
    priority_evaluated_at timestamp with time zone,
    priority_rule_id character varying(100),
    assigned_at timestamp with time zone,
    assigned_by character varying(100),
    assignment_reason text,
    title character varying(200),
    category character varying(100),
    status character varying(50) DEFAULT 'OPEN'::character varying,
    location_name character varying(255),
    reported_by uuid,
    assigned_to uuid
);


--
-- Name: infrastructure; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.infrastructure (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    infrastructure_type character varying(100) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    address character varying(255),
    status character varying(50) DEFAULT 'OPERATIONAL'::character varying NOT NULL,
    importance_level character varying(50) DEFAULT 'PRIMARY'::character varying NOT NULL,
    department_id uuid,
    metadata_json text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_inspected_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    maintenance_priority character varying(50) DEFAULT 'MEDIUM'::character varying,
    notes text
);


--
-- Name: locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.locations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    location_type character varying(50) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    ward_number character varying(50),
    district character varying(100),
    metadata_json text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: mobile_otps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mobile_otps (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    mobile_number character varying(30) NOT NULL,
    user_id uuid,
    otp_hash character varying(255) NOT NULL,
    purpose character varying(50) DEFAULT 'REGISTRATION'::character varying NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    max_attempts integer DEFAULT 3 NOT NULL,
    resend_after timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    is_used boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    recipient_user_id uuid,
    title character varying(200) NOT NULL,
    message text NOT NULL,
    notification_type character varying(50) NOT NULL,
    channel character varying(50) DEFAULT 'IN_APP'::character varying NOT NULL,
    status character varying(50) DEFAULT 'UNREAD'::character varying NOT NULL,
    sent_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    recipient_role character varying(100),
    incident_id uuid,
    severity character varying(50) DEFAULT 'MEDIUM'::character varying NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    dedup_key character varying(255),
    complaint_id uuid,
    complaint_number character varying(50)
);


--
-- Name: priority_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.priority_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rule_id character varying(50) NOT NULL,
    incident_type character varying(100) NOT NULL,
    tier character varying(20) NOT NULL,
    priority character varying(20) NOT NULL,
    sla_minutes integer NOT NULL,
    default_department_id uuid,
    conditions_json text,
    recommended_actions_json text,
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    name character varying(150),
    reason_template text
);


--
-- Name: response_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.response_actions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_id uuid NOT NULL,
    action_type character varying(50) NOT NULL,
    description text NOT NULL,
    status character varying(50) DEFAULT 'RECOMMENDED'::character varying NOT NULL,
    created_by character varying(100),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at timestamp with time zone
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(50) NOT NULL,
    description character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sensors_gis; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sensors_gis (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sensor_id character varying(100) NOT NULL,
    sensor_type character varying(100) NOT NULL,
    location_name character varying(255),
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    status character varying(50) DEFAULT 'ONLINE'::character varying NOT NULL,
    last_value double precision,
    unit character varying(50),
    last_seen_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sla_escalation_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sla_escalation_history (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_id uuid NOT NULL,
    sla_record_id uuid,
    previous_escalation_level character varying(50),
    new_escalation_level character varying(50) NOT NULL,
    previous_status character varying(50),
    new_status character varying(50) NOT NULL,
    reason text,
    escalated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sla_policies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sla_policies (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    category character varying(100) NOT NULL,
    severity character varying(50) NOT NULL,
    response_time_minutes integer NOT NULL,
    resolution_time_minutes integer NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sla_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sla_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    incident_id uuid NOT NULL,
    target_sla_minutes integer NOT NULL,
    deadline_at timestamp with time zone NOT NULL,
    current_sla_status character varying(50) DEFAULT 'ON_TRACK'::character varying NOT NULL,
    remaining_seconds bigint,
    breached boolean DEFAULT false NOT NULL,
    breached_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    priority_tier character varying(20),
    target_minutes integer,
    started_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at timestamp with time zone,
    resolved_at timestamp with time zone,
    status character varying(50) DEFAULT 'ON_TRACK'::character varying,
    remaining_minutes bigint,
    escalation_level character varying(50) DEFAULT 'NONE'::character varying,
    policy_id uuid,
    response_deadline timestamp with time zone,
    resolution_deadline timestamp with time zone,
    first_responded_at timestamp with time zone,
    last_escalated_at timestamp with time zone
);


--
-- Name: teams; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teams (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    department_id uuid NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    contact_number character varying(50),
    status character varying(50) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    active boolean DEFAULT true NOT NULL
);


--
-- Name: traffic_signals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.traffic_signals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    signal_code character varying(50) NOT NULL,
    junction_name character varying(150) NOT NULL,
    junction_type character varying(50) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    operational_status character varying(50) DEFAULT 'ACTIVE'::character varying NOT NULL,
    current_cycle_seconds integer DEFAULT 90,
    last_inspected_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    assigned_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    username character varying(100) NOT NULL,
    email character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(150) NOT NULL,
    phone character varying(50),
    role_id uuid,
    department_id uuid,
    team_id uuid,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    role character varying(50) DEFAULT 'CITIZEN'::character varying,
    enabled boolean DEFAULT true,
    mobile_number character varying(30),
    is_mobile_verified boolean DEFAULT false,
    genuine_complaint_count integer DEFAULT 0,
    is_verified_citizen boolean DEFAULT false
);


--
-- Name: waste_bins; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.waste_bins (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    bin_code character varying(50) NOT NULL,
    location_name character varying(150) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    capacity_liters integer DEFAULT 1100 NOT NULL,
    current_fill_percentage double precision DEFAULT 0.0 NOT NULL,
    last_emptied_at timestamp with time zone,
    battery_percentage double precision DEFAULT 100.0,
    sensor_status character varying(50) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: acknowledgement_documents acknowledgement_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acknowledgement_documents
    ADD CONSTRAINT acknowledgement_documents_pkey PRIMARY KEY (id);


--
-- Name: assignment_rules assignment_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assignment_rules
    ADD CONSTRAINT assignment_rules_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: complaint_attachments complaint_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_attachments
    ADD CONSTRAINT complaint_attachments_pkey PRIMARY KEY (id);


--
-- Name: complaint_receipts complaint_receipts_complaint_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_receipts
    ADD CONSTRAINT complaint_receipts_complaint_id_key UNIQUE (complaint_id);


--
-- Name: complaint_receipts complaint_receipts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_receipts
    ADD CONSTRAINT complaint_receipts_pkey PRIMARY KEY (id);


--
-- Name: complaint_receipts complaint_receipts_receipt_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_receipts
    ADD CONSTRAINT complaint_receipts_receipt_number_key UNIQUE (receipt_number);


--
-- Name: complaint_status_history complaint_status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_status_history
    ADD CONSTRAINT complaint_status_history_pkey PRIMARY KEY (id);


--
-- Name: complaint_verification_history complaint_verification_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_verification_history
    ADD CONSTRAINT complaint_verification_history_pkey PRIMARY KEY (id);


--
-- Name: complaints complaints_complaint_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_complaint_number_key UNIQUE (complaint_number);


--
-- Name: complaints complaints_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_pkey PRIMARY KEY (id);


--
-- Name: departments departments_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_code_key UNIQUE (code);


--
-- Name: departments departments_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_name_key UNIQUE (name);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: emergency_resources emergency_resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.emergency_resources
    ADD CONSTRAINT emergency_resources_pkey PRIMARY KEY (id);


--
-- Name: external_grievances external_grievances_acknowledgement_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_grievances
    ADD CONSTRAINT external_grievances_acknowledgement_number_key UNIQUE (acknowledgement_number);


--
-- Name: external_grievances external_grievances_idempotency_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_grievances
    ADD CONSTRAINT external_grievances_idempotency_key_key UNIQUE (idempotency_key);


--
-- Name: external_grievances external_grievances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_grievances
    ADD CONSTRAINT external_grievances_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: government_portals government_portals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.government_portals
    ADD CONSTRAINT government_portals_pkey PRIMARY KEY (id);


--
-- Name: government_portals government_portals_portal_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.government_portals
    ADD CONSTRAINT government_portals_portal_code_key UNIQUE (portal_code);


--
-- Name: grievance_status_history grievance_status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grievance_status_history
    ADD CONSTRAINT grievance_status_history_pkey PRIMARY KEY (id);


--
-- Name: grievance_sync_logs grievance_sync_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grievance_sync_logs
    ADD CONSTRAINT grievance_sync_logs_pkey PRIMARY KEY (id);


--
-- Name: incident_assignment_history incident_assignment_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_assignment_history
    ADD CONSTRAINT incident_assignment_history_pkey PRIMARY KEY (id);


--
-- Name: incident_status_history incident_status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_status_history
    ADD CONSTRAINT incident_status_history_pkey PRIMARY KEY (id);


--
-- Name: incidents incidents_incident_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_incident_number_key UNIQUE (incident_number);


--
-- Name: incidents incidents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_pkey PRIMARY KEY (id);


--
-- Name: infrastructure infrastructure_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.infrastructure
    ADD CONSTRAINT infrastructure_code_key UNIQUE (code);


--
-- Name: infrastructure infrastructure_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.infrastructure
    ADD CONSTRAINT infrastructure_pkey PRIMARY KEY (id);


--
-- Name: locations locations_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_code_key UNIQUE (code);


--
-- Name: locations locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locations
    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);


--
-- Name: mobile_otps mobile_otps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mobile_otps
    ADD CONSTRAINT mobile_otps_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: priority_rules priority_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.priority_rules
    ADD CONSTRAINT priority_rules_pkey PRIMARY KEY (id);


--
-- Name: priority_rules priority_rules_rule_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.priority_rules
    ADD CONSTRAINT priority_rules_rule_id_key UNIQUE (rule_id);


--
-- Name: response_actions response_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.response_actions
    ADD CONSTRAINT response_actions_pkey PRIMARY KEY (id);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: sensors_gis sensors_gis_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sensors_gis
    ADD CONSTRAINT sensors_gis_pkey PRIMARY KEY (id);


--
-- Name: sensors_gis sensors_gis_sensor_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sensors_gis
    ADD CONSTRAINT sensors_gis_sensor_id_key UNIQUE (sensor_id);


--
-- Name: sla_escalation_history sla_escalation_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_escalation_history
    ADD CONSTRAINT sla_escalation_history_pkey PRIMARY KEY (id);


--
-- Name: sla_policies sla_policies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_policies
    ADD CONSTRAINT sla_policies_pkey PRIMARY KEY (id);


--
-- Name: sla_records sla_records_incident_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_records
    ADD CONSTRAINT sla_records_incident_id_key UNIQUE (incident_id);


--
-- Name: sla_records sla_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_records
    ADD CONSTRAINT sla_records_pkey PRIMARY KEY (id);


--
-- Name: teams teams_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_code_key UNIQUE (code);


--
-- Name: teams teams_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (id);


--
-- Name: traffic_signals traffic_signals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.traffic_signals
    ADD CONSTRAINT traffic_signals_pkey PRIMARY KEY (id);


--
-- Name: traffic_signals traffic_signals_signal_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.traffic_signals
    ADD CONSTRAINT traffic_signals_signal_code_key UNIQUE (signal_code);


--
-- Name: sla_policies uq_sla_policy_category_severity; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_policies
    ADD CONSTRAINT uq_sla_policy_category_severity UNIQUE (category, severity);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: waste_bins waste_bins_bin_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.waste_bins
    ADD CONSTRAINT waste_bins_bin_code_key UNIQUE (bin_code);


--
-- Name: waste_bins waste_bins_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.waste_bins
    ADD CONSTRAINT waste_bins_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_ack_docs_grv_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ack_docs_grv_id ON public.acknowledgement_documents USING btree (external_grievance_id);


--
-- Name: idx_assignment_history_assigned_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignment_history_assigned_at ON public.incident_assignment_history USING btree (assigned_at);


--
-- Name: idx_assignment_history_incident_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignment_history_incident_id ON public.incident_assignment_history USING btree (incident_id);


--
-- Name: idx_assignment_rules_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignment_rules_active ON public.assignment_rules USING btree (active);


--
-- Name: idx_assignment_rules_incident_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assignment_rules_incident_type ON public.assignment_rules USING btree (incident_type);


--
-- Name: idx_audit_logs_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);


--
-- Name: idx_audit_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_created_at ON public.audit_logs USING btree (created_at);


--
-- Name: idx_audit_logs_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity ON public.audit_logs USING btree (entity_type, entity_id);


--
-- Name: idx_comp_att_complaint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comp_att_complaint_id ON public.complaint_attachments USING btree (complaint_id);


--
-- Name: idx_comp_att_uploaded_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comp_att_uploaded_by ON public.complaint_attachments USING btree (uploaded_by);


--
-- Name: idx_comp_receipt_complaint; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comp_receipt_complaint ON public.complaint_receipts USING btree (complaint_id);


--
-- Name: idx_comp_receipt_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comp_receipt_number ON public.complaint_receipts USING btree (receipt_number);


--
-- Name: idx_comp_status_hist_complaint; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comp_status_hist_complaint ON public.complaint_status_history USING btree (complaint_id);


--
-- Name: idx_comp_status_hist_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comp_status_hist_created ON public.complaint_status_history USING btree (created_at);


--
-- Name: idx_complaints_assigned_to; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_assigned_to ON public.complaints USING btree (assigned_to_user_id);


--
-- Name: idx_complaints_citizen_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_citizen_id ON public.complaints USING btree (citizen_id);


--
-- Name: idx_complaints_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_department_id ON public.complaints USING btree (department_id);


--
-- Name: idx_complaints_incident_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_incident_id ON public.complaints USING btree (incident_id);


--
-- Name: idx_complaints_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_priority ON public.complaints USING btree (priority);


--
-- Name: idx_complaints_sla_deadline; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_sla_deadline ON public.complaints USING btree (sla_deadline);


--
-- Name: idx_complaints_sla_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_sla_status ON public.complaints USING btree (sla_status);


--
-- Name: idx_complaints_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_status ON public.complaints USING btree (status);


--
-- Name: idx_complaints_verification_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_complaints_verification_status ON public.complaints USING btree (complaint_verification_status);


--
-- Name: idx_emergency_resources_coords; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_emergency_resources_coords ON public.emergency_resources USING btree (latitude, longitude);


--
-- Name: idx_emergency_resources_earth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_emergency_resources_earth ON public.emergency_resources USING gist (public.ll_to_earth(latitude, longitude));


--
-- Name: idx_emergency_resources_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_emergency_resources_type ON public.emergency_resources USING btree (resource_type);


--
-- Name: idx_ext_grv_ack_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ext_grv_ack_number ON public.external_grievances USING btree (acknowledgement_number);


--
-- Name: idx_ext_grv_complaint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ext_grv_complaint_id ON public.external_grievances USING btree (complaint_id);


--
-- Name: idx_ext_grv_idempotency_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ext_grv_idempotency_key ON public.external_grievances USING btree (idempotency_key);


--
-- Name: idx_grv_hist_external_grv; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_grv_hist_external_grv ON public.grievance_status_history USING btree (external_grievance_id);


--
-- Name: idx_incident_history_changed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incident_history_changed_at ON public.incident_status_history USING btree (changed_at);


--
-- Name: idx_incident_history_incident_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incident_history_incident_id ON public.incident_status_history USING btree (incident_id);


--
-- Name: idx_incidents_assigned_team_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_assigned_team_id ON public.incidents USING btree (assigned_team_id);


--
-- Name: idx_incidents_assigned_to; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_assigned_to ON public.incidents USING btree (assigned_to);


--
-- Name: idx_incidents_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_category ON public.incidents USING btree (category);


--
-- Name: idx_incidents_coordinates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_coordinates ON public.incidents USING btree (latitude, longitude);


--
-- Name: idx_incidents_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_created_at ON public.incidents USING btree (created_at);


--
-- Name: idx_incidents_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_department_id ON public.incidents USING btree (department_id);


--
-- Name: idx_incidents_earth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_earth ON public.incidents USING gist (public.ll_to_earth(latitude, longitude));


--
-- Name: idx_incidents_location_name_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_location_name_lower ON public.incidents USING btree (lower((location_name)::text));


--
-- Name: idx_incidents_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_priority ON public.incidents USING btree (priority);


--
-- Name: idx_incidents_reported_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_reported_by ON public.incidents USING btree (reported_by);


--
-- Name: idx_incidents_reported_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_reported_time ON public.incidents USING btree (reported_time);


--
-- Name: idx_incidents_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_severity ON public.incidents USING btree (severity);


--
-- Name: idx_incidents_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_source ON public.incidents USING btree (source);


--
-- Name: idx_incidents_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_status ON public.incidents USING btree (current_status);


--
-- Name: idx_incidents_status_col; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_status_col ON public.incidents USING btree (status);


--
-- Name: idx_incidents_tier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_tier ON public.incidents USING btree (assigned_tier);


--
-- Name: idx_incidents_title_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_title_lower ON public.incidents USING btree (lower((title)::text));


--
-- Name: idx_incidents_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_incidents_type ON public.incidents USING btree (incident_type);


--
-- Name: idx_infrastructure_coords; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_infrastructure_coords ON public.infrastructure USING btree (latitude, longitude);


--
-- Name: idx_infrastructure_earth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_infrastructure_earth ON public.infrastructure USING gist (public.ll_to_earth(latitude, longitude));


--
-- Name: idx_infrastructure_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_infrastructure_status ON public.infrastructure USING btree (status);


--
-- Name: idx_infrastructure_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_infrastructure_type ON public.infrastructure USING btree (infrastructure_type);


--
-- Name: idx_locations_earth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_earth ON public.locations USING gist (public.ll_to_earth(latitude, longitude));


--
-- Name: idx_locations_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_locations_type ON public.locations USING btree (location_type);


--
-- Name: idx_mobile_otps_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mobile_otps_created_at ON public.mobile_otps USING btree (created_at);


--
-- Name: idx_mobile_otps_mobile_purpose; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mobile_otps_mobile_purpose ON public.mobile_otps USING btree (mobile_number, purpose);


--
-- Name: idx_notifications_complaint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_complaint_id ON public.notifications USING btree (complaint_id);


--
-- Name: idx_notifications_complaint_num; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_complaint_num ON public.notifications USING btree (complaint_number);


--
-- Name: idx_notifications_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_created_at ON public.notifications USING btree (created_at DESC);


--
-- Name: idx_notifications_dedup_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_dedup_key ON public.notifications USING btree (dedup_key);


--
-- Name: idx_notifications_incident_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_incident_id ON public.notifications USING btree (incident_id);


--
-- Name: idx_notifications_is_read; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_is_read ON public.notifications USING btree (recipient_user_id, is_read);


--
-- Name: idx_notifications_recipient_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_recipient_status ON public.notifications USING btree (recipient_user_id, status);


--
-- Name: idx_notifications_recipient_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_recipient_user_id ON public.notifications USING btree (recipient_user_id);


--
-- Name: idx_notifications_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_role ON public.notifications USING btree (recipient_role);


--
-- Name: idx_notifications_sent_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_sent_at ON public.notifications USING btree (sent_at DESC);


--
-- Name: idx_priority_rules_type_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_priority_rules_type_enabled ON public.priority_rules USING btree (incident_type, enabled);


--
-- Name: idx_response_actions_incident_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_response_actions_incident_id ON public.response_actions USING btree (incident_id);


--
-- Name: idx_response_actions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_response_actions_status ON public.response_actions USING btree (status);


--
-- Name: idx_sensors_gis_earth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sensors_gis_earth ON public.sensors_gis USING gist (public.ll_to_earth(latitude, longitude));


--
-- Name: idx_sla_escalation_incident; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_escalation_incident ON public.sla_escalation_history USING btree (incident_id);


--
-- Name: idx_sla_escalation_record; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_escalation_record ON public.sla_escalation_history USING btree (sla_record_id);


--
-- Name: idx_sla_escalation_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_escalation_time ON public.sla_escalation_history USING btree (escalated_at);


--
-- Name: idx_sla_policies_cat_sev; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_policies_cat_sev ON public.sla_policies USING btree (category, severity);


--
-- Name: idx_sla_policies_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_policies_enabled ON public.sla_policies USING btree (enabled);


--
-- Name: idx_sla_records_deadline; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_records_deadline ON public.sla_records USING btree (deadline_at);


--
-- Name: idx_sla_records_deadline_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_records_deadline_at ON public.sla_records USING btree (deadline_at);


--
-- Name: idx_sla_records_escalation_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_records_escalation_level ON public.sla_records USING btree (escalation_level);


--
-- Name: idx_sla_records_incident_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_records_incident_id ON public.sla_records USING btree (incident_id);


--
-- Name: idx_sla_records_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_records_status ON public.sla_records USING btree (current_sla_status);


--
-- Name: idx_sla_records_status_new; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sla_records_status_new ON public.sla_records USING btree (status);


--
-- Name: idx_sync_logs_grv_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_logs_grv_id ON public.grievance_sync_logs USING btree (external_grievance_id);


--
-- Name: idx_sync_logs_timestamp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_logs_timestamp ON public.grievance_sync_logs USING btree (sync_timestamp);


--
-- Name: idx_teams_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_teams_department_id ON public.teams USING btree (department_id);


--
-- Name: idx_traffic_signals_earth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_traffic_signals_earth ON public.traffic_signals USING gist (public.ll_to_earth(latitude, longitude));


--
-- Name: idx_traffic_signals_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_traffic_signals_status ON public.traffic_signals USING btree (operational_status);


--
-- Name: idx_user_roles_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_roles_role_id ON public.user_roles USING btree (role_id);


--
-- Name: idx_user_roles_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_roles_user_id ON public.user_roles USING btree (user_id);


--
-- Name: idx_users_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_department_id ON public.users USING btree (department_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_email_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email_lower ON public.users USING btree (lower((email)::text));


--
-- Name: idx_users_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_enabled ON public.users USING btree (enabled);


--
-- Name: idx_users_mobile_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_mobile_number ON public.users USING btree (mobile_number);


--
-- Name: idx_users_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role ON public.users USING btree (role);


--
-- Name: idx_users_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_role_id ON public.users USING btree (role_id);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: idx_users_username_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_username_lower ON public.users USING btree (lower((username)::text));


--
-- Name: idx_users_verified_citizen; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_verified_citizen ON public.users USING btree (is_verified_citizen);


--
-- Name: idx_verif_hist_citizen_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_verif_hist_citizen_id ON public.complaint_verification_history USING btree (citizen_id);


--
-- Name: idx_verif_hist_complaint_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_verif_hist_complaint_id ON public.complaint_verification_history USING btree (complaint_id);


--
-- Name: idx_waste_bins_earth; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_waste_bins_earth ON public.waste_bins USING gist (public.ll_to_earth(latitude, longitude));


--
-- Name: idx_waste_bins_fill; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_waste_bins_fill ON public.waste_bins USING btree (current_fill_percentage);


--
-- Name: acknowledgement_documents acknowledgement_documents_external_grievance_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acknowledgement_documents
    ADD CONSTRAINT acknowledgement_documents_external_grievance_id_fkey FOREIGN KEY (external_grievance_id) REFERENCES public.external_grievances(id) ON DELETE CASCADE;


--
-- Name: complaint_attachments complaint_attachments_complaint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_attachments
    ADD CONSTRAINT complaint_attachments_complaint_id_fkey FOREIGN KEY (complaint_id) REFERENCES public.complaints(id) ON DELETE CASCADE;


--
-- Name: complaint_attachments complaint_attachments_uploaded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_attachments
    ADD CONSTRAINT complaint_attachments_uploaded_by_fkey FOREIGN KEY (uploaded_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: complaint_receipts complaint_receipts_complaint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_receipts
    ADD CONSTRAINT complaint_receipts_complaint_id_fkey FOREIGN KEY (complaint_id) REFERENCES public.complaints(id) ON DELETE CASCADE;


--
-- Name: complaint_status_history complaint_status_history_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_status_history
    ADD CONSTRAINT complaint_status_history_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: complaint_status_history complaint_status_history_complaint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_status_history
    ADD CONSTRAINT complaint_status_history_complaint_id_fkey FOREIGN KEY (complaint_id) REFERENCES public.complaints(id) ON DELETE CASCADE;


--
-- Name: complaint_verification_history complaint_verification_history_citizen_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_verification_history
    ADD CONSTRAINT complaint_verification_history_citizen_id_fkey FOREIGN KEY (citizen_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: complaint_verification_history complaint_verification_history_complaint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_verification_history
    ADD CONSTRAINT complaint_verification_history_complaint_id_fkey FOREIGN KEY (complaint_id) REFERENCES public.complaints(id) ON DELETE CASCADE;


--
-- Name: complaint_verification_history complaint_verification_history_verified_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaint_verification_history
    ADD CONSTRAINT complaint_verification_history_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: complaints complaints_assigned_to_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_assigned_to_user_id_fkey FOREIGN KEY (assigned_to_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: complaints complaints_citizen_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_citizen_id_fkey FOREIGN KEY (citizen_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: complaints complaints_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: complaints complaints_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE SET NULL;


--
-- Name: complaints complaints_verified_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: external_grievances external_grievances_complaint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_grievances
    ADD CONSTRAINT external_grievances_complaint_id_fkey FOREIGN KEY (complaint_id) REFERENCES public.complaints(id) ON DELETE CASCADE;


--
-- Name: external_grievances external_grievances_portal_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.external_grievances
    ADD CONSTRAINT external_grievances_portal_id_fkey FOREIGN KEY (portal_id) REFERENCES public.government_portals(id) ON DELETE RESTRICT;


--
-- Name: grievance_status_history grievance_status_history_external_grievance_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grievance_status_history
    ADD CONSTRAINT grievance_status_history_external_grievance_id_fkey FOREIGN KEY (external_grievance_id) REFERENCES public.external_grievances(id) ON DELETE CASCADE;


--
-- Name: grievance_sync_logs grievance_sync_logs_external_grievance_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.grievance_sync_logs
    ADD CONSTRAINT grievance_sync_logs_external_grievance_id_fkey FOREIGN KEY (external_grievance_id) REFERENCES public.external_grievances(id) ON DELETE CASCADE;


--
-- Name: incident_assignment_history incident_assignment_history_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_assignment_history
    ADD CONSTRAINT incident_assignment_history_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE CASCADE;


--
-- Name: incident_assignment_history incident_assignment_history_new_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_assignment_history
    ADD CONSTRAINT incident_assignment_history_new_department_id_fkey FOREIGN KEY (new_department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: incident_assignment_history incident_assignment_history_new_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_assignment_history
    ADD CONSTRAINT incident_assignment_history_new_team_id_fkey FOREIGN KEY (new_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: incident_assignment_history incident_assignment_history_previous_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_assignment_history
    ADD CONSTRAINT incident_assignment_history_previous_department_id_fkey FOREIGN KEY (previous_department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: incident_assignment_history incident_assignment_history_previous_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_assignment_history
    ADD CONSTRAINT incident_assignment_history_previous_team_id_fkey FOREIGN KEY (previous_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: incident_status_history incident_status_history_changed_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_status_history
    ADD CONSTRAINT incident_status_history_changed_by_user_id_fkey FOREIGN KEY (changed_by_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: incident_status_history incident_status_history_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incident_status_history
    ADD CONSTRAINT incident_status_history_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE CASCADE;


--
-- Name: incidents incidents_assigned_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_assigned_team_id_fkey FOREIGN KEY (assigned_team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- Name: incidents incidents_assigned_to_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_assigned_to_fkey FOREIGN KEY (assigned_to) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: incidents incidents_created_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_created_by_user_id_fkey FOREIGN KEY (created_by_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: incidents incidents_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: incidents incidents_location_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_location_id_fkey FOREIGN KEY (location_id) REFERENCES public.locations(id) ON DELETE SET NULL;


--
-- Name: incidents incidents_reported_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.incidents
    ADD CONSTRAINT incidents_reported_by_fkey FOREIGN KEY (reported_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: infrastructure infrastructure_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.infrastructure
    ADD CONSTRAINT infrastructure_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: mobile_otps mobile_otps_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mobile_otps
    ADD CONSTRAINT mobile_otps_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: notifications notifications_complaint_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_complaint_id_fkey FOREIGN KEY (complaint_id) REFERENCES public.complaints(id) ON DELETE CASCADE;


--
-- Name: notifications notifications_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE SET NULL;


--
-- Name: notifications notifications_recipient_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_recipient_user_id_fkey FOREIGN KEY (recipient_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: priority_rules priority_rules_default_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.priority_rules
    ADD CONSTRAINT priority_rules_default_department_id_fkey FOREIGN KEY (default_department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: response_actions response_actions_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.response_actions
    ADD CONSTRAINT response_actions_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE CASCADE;


--
-- Name: sla_escalation_history sla_escalation_history_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_escalation_history
    ADD CONSTRAINT sla_escalation_history_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE CASCADE;


--
-- Name: sla_escalation_history sla_escalation_history_sla_record_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_escalation_history
    ADD CONSTRAINT sla_escalation_history_sla_record_id_fkey FOREIGN KEY (sla_record_id) REFERENCES public.sla_records(id) ON DELETE CASCADE;


--
-- Name: sla_records sla_records_incident_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_records
    ADD CONSTRAINT sla_records_incident_id_fkey FOREIGN KEY (incident_id) REFERENCES public.incidents(id) ON DELETE CASCADE;


--
-- Name: sla_records sla_records_policy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sla_records
    ADD CONSTRAINT sla_records_policy_id_fkey FOREIGN KEY (policy_id) REFERENCES public.sla_policies(id) ON DELETE SET NULL;


--
-- Name: teams teams_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- Name: user_roles user_roles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: users users_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL;


--
-- Name: users users_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: users users_team_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_team_id_fkey FOREIGN KEY (team_id) REFERENCES public.teams(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict 3dbaDSiurBIGNcWpGoDczaXWsMspchj2E1TbW3bw1fze8g6UrcDuRwCE7G6TRhG


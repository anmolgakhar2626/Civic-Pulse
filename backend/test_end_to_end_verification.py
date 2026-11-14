import urllib.request
import urllib.parse
import json
import sys

BASE_URL = "http://localhost:8080/api"

def api_request(path, method="GET", data=None, token=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            return response.status, json.loads(res_body) if res_body else {}
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        print(f"HTTPError {e.code} on {method} {path}: {err_body}")
        try:
            return e.code, json.loads(err_body)
        except Exception:
            return e.code, {"error": err_body}

def run_verification():
    print("==================================================")
    print("SMART CITY COMMAND CENTRE - END-TO-END VERIFICATION")
    print("==================================================")

    # 1. Login
    print("\n[Step 1] Authenticating as admin...")
    status, login_res = api_request("/auth/login", method="POST", data={
        "username": "admin",
        "password": "Admin@CivicPulse2026"
    })
    assert status == 200, f"Login failed: {login_res}"
    token = login_res["token"]
    print("✓ Login successful! Token acquired.")

    # 2. Check Dashboard / Weather / GIS
    print("\n[Step 2] Testing GIS & External Weather Endpoints...")
    status, weather_res = api_request("/weather/current?latitude=30.7333&longitude=76.7794", token=token)
    assert status == 200, f"Weather failed: {weather_res}"
    print(f"✓ Weather Live: {weather_res['data']['temperature']}°C, {weather_res['data']['humidity']}% RH, Wind: {weather_res['data']['windSpeed']} km/h")

    status, aq_res = api_request("/air-quality/current?latitude=30.7333&longitude=76.7794", token=token)
    assert status == 200, f"Air Quality failed: {aq_res}"
    print(f"✓ Air Quality Live: AQI {aq_res['data']['aqi']}, PM2.5: {aq_res['data']['pm25']}, Category: {aq_res['data']['category']}")

    status, gis_res = api_request("/gis/incidents", token=token)
    assert status == 200, f"GIS Incidents failed: {gis_res}"
    print(f"✓ GIS Incidents GeoJSON: {len(gis_res['features'])} features")

    # 3. Create Incident
    print("\n[Step 3] Creating new incident: ELECTRICAL_HAZARD (Live Wire)...")
    inc_payload = {
        "incidentType": "ELECTRICAL_HAZARD",
        "description": "Live exposed electrical wire sparking on public walkway",
        "source": "OPERATOR",
        "severityIndicator": "EXTREME",
        "latitude": 30.7425,
        "longitude": 76.7820,
        "address": "Sector 17 Commercial Promenade",
        "reportedAt": "2026-10-05T08:00:00Z"
    }
    status, inc_data = api_request("/incidents", method="POST", data=inc_payload, token=token)
    assert status in [200, 201], f"Create incident failed: {inc_data}"
    incident_id = inc_data.get("id") or inc_data.get("data", {}).get("id")
    print(f"✓ Incident created: ID={incident_id}, Number={inc_data.get('incidentNumber') or inc_data.get('data', {}).get('incidentNumber')}")

    # 4. Evaluate Priority
    print("\n[Step 4] Running Priority Rule Engine...")
    status, prio_data = api_request(f"/incidents/{incident_id}/evaluate-priority", method="POST", token=token)
    assert status == 200, f"Evaluate priority failed: {prio_data}"
    tier = prio_data.get("tier") or prio_data.get("priorityTier") or prio_data.get("data", {}).get("tier")
    sla_mins = prio_data.get("slaMinutes") or prio_data.get("data", {}).get("slaMinutes")
    print(f"✓ Priority Evaluated: Tier={tier}, Target SLA={sla_mins} minutes")
    assert tier == "CRITICAL", f"Expected CRITICAL for ELECTRICAL_HAZARD with live wire, got {tier}"

    # 5. Auto-Assign Department
    print("\n[Step 5] Running Department Auto-Assignment...")
    status, assign_data = api_request(f"/incidents/{incident_id}/auto-assign", method="POST", token=token)
    assert status == 200, f"Auto assign failed: {assign_data}"
    dept = assign_data.get("department") or assign_data.get("assignedDepartmentName")
    team = assign_data.get("team")
    print(f"✓ Incident Assigned: Department='{dept}', Team='{team}'")
    assert dept == "Emergency Services", f"Expected Emergency Services, got {dept}"

    # 6. Generate Response Plan
    print("\n[Step 6] Generating Standard Response Protocol Plan...")
    status, plan_data = api_request(f"/incidents/{incident_id}/generate-response-plan", method="POST", token=token)
    assert status == 200, f"Generate plan failed: {plan_data}"
    status, actions_data = api_request(f"/incidents/{incident_id}/actions", token=token)
    actions = actions_data.get("data") if isinstance(actions_data, dict) and "data" in actions_data else actions_data
    if isinstance(actions, list):
        print(f"✓ Response Plan Generated: {len(actions)} protocol actions dispatched")

    # 7. PostGIS Nearby Emergency Resources
    print("\n[Step 7] Testing PostGIS Spatial Proximity query for emergency resources near incident...")
    status, nearby_res = api_request("/gis/nearby-resources?latitude=30.7470&longitude=76.7900&radius=5000", token=token)
    assert status == 200, f"Nearby query failed: {nearby_res}"
    features = nearby_res["features"]
    print(f"✓ PostGIS Spatial Query returned {len(features)} emergency resources within 5km radius")
    for f in features[:3]:
        print(f"   - {f['properties']['name']} ({f['properties']['resourceType']})")

    # 8. IoT Sensor Spike Simulation -> Auto Incident Creation
    print("\n[Step 8] Testing IoT Telemetry Spike: Injecting PM2.5 = 380 µg/m³...")
    status, spike_data = api_request("/iot/simulator/trigger/pm25-spike?sensorId=PM25-IND-01&value=380.0", method="POST", token=token)
    assert status == 200, f"Sensor spike failed: {spike_data}"
    print(f"✓ Spike processed: {spike_data.get('message')}")

    print("\n==================================================")
    print("ALL VERIFICATION CHECKS COMPLETED SUCCESSFULLY! ✓")
    print("==================================================")

if __name__ == "__main__":
    run_verification()

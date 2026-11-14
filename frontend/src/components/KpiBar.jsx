import React from 'react';
import { AlertTriangle, Clock, CheckCircle2, Wind, Car, Trash2 } from 'lucide-react';

export default function KpiBar({ kpis }) {
  if (!kpis) return null;

  return (
    <div className="kpi-grid">
      <div className={`kpi-card ${kpis.criticalIncidents > 0 ? 'kpi-critical' : 'kpi-info'}`}>
        <div className="kpi-title">
          <span>Critical Incidents</span>
          <AlertTriangle size={14} color={kpis.criticalIncidents > 0 ? '#dc2626' : '#64748b'} />
        </div>
        <div className="kpi-value" style={{ color: kpis.criticalIncidents > 0 ? '#dc2626' : undefined }}>
          {kpis.criticalIncidents}
        </div>
        <div className="kpi-sub">Tier 1 imminent hazard (SLA ≤30m)</div>
      </div>

      <div className="kpi-card kpi-warning">
        <div className="kpi-title">
          <span>Active Incidents</span>
          <Clock size={14} color="#d97706" />
        </div>
        <div className="kpi-value">{kpis.activeIncidents}</div>
        <div className="kpi-sub">Across all municipal sectors</div>
      </div>

      <div className="kpi-card kpi-success">
        <div className="kpi-title">
          <span>SLA Compliance</span>
          <CheckCircle2 size={14} color="#15803d" />
        </div>
        <div className="kpi-value">{kpis.slaCompliancePercentage}%</div>
        <div className="kpi-sub">{kpis.breachedSlaIncidents} breaches recorded</div>
      </div>

      <div className="kpi-card kpi-info">
        <div className="kpi-title">
          <span>Congestion Index</span>
          <Car size={14} color="#0284c7" />
        </div>
        <div className="kpi-value">{kpis.trafficCongestionIndex}/100</div>
        <div className="kpi-sub">Live arterial traffic telemetry</div>
      </div>

      <div className="kpi-card kpi-info">
        <div className="kpi-title">
          <span>Average City AQI</span>
          <Wind size={14} color="#0f766e" />
        </div>
        <div className="kpi-value">{kpis.averageAqi}</div>
        <div className="kpi-sub">Open-Meteo & station telemetry</div>
      </div>

      <div className="kpi-card kpi-warning">
        <div className="kpi-title">
          <span>Overflowing Bins</span>
          <Trash2 size={14} color="#b45309" />
        </div>
        <div className="kpi-value">{kpis.criticalWasteBins}</div>
        <div className="kpi-sub">Smart bins &gt;90% capacity</div>
      </div>
    </div>
  );
}

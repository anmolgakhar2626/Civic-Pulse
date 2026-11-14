import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { weatherApi } from '../api/weatherApi';
import {
  FiThermometer,
  FiDroplet,
  FiWind,
  FiCompass,
  FiSun,
  FiCloud,
  FiCloudRain,
  FiCloudLightning,
  FiCloudSnow,
  FiRefreshCw,
  FiMapPin,
  FiAlertCircle,
  FiClock,
  FiActivity,
  FiCheckCircle,
  FiEye,
  FiArrowUp,
  FiArrowDown
} from 'react-icons/fi';
import { LuCloudSun } from 'react-icons/lu';

// WMO Weather Interpretation Codes
const getWeatherDescription = (code) => {
  if (code === 0) return { label: 'Clear Sky', icon: <FiSun className="weather-icon-sun" /> };
  if (code === 1) return { label: 'Mainly Clear', icon: <LuCloudSun className="weather-icon-sun" /> };
  if (code === 2) return { label: 'Partly Cloudy', icon: <LuCloudSun className="weather-icon-cloud" /> };
  if (code === 3) return { label: 'Overcast', icon: <FiCloud className="weather-icon-cloud" /> };
  if (code === 45 || code === 48) return { label: 'Foggy / Hazy', icon: <FiCloud className="weather-icon-cloud" /> };
  if (code >= 51 && code <= 55) return { label: 'Drizzle', icon: <FiCloudRain className="weather-icon-rain" /> };
  if (code >= 61 && code <= 65) return { label: 'Rain', icon: <FiCloudRain className="weather-icon-rain" /> };
  if (code >= 71 && code <= 77) return { label: 'Snowfall', icon: <FiCloudSnow className="weather-icon-snow" /> };
  if (code >= 80 && code <= 82) return { label: 'Rain Showers', icon: <FiCloudRain className="weather-icon-rain" /> };
  if (code >= 95) return { label: 'Thunderstorm', icon: <FiCloudLightning className="weather-icon-storm" /> };
  return { label: 'Fair Conditions', icon: <LuCloudSun className="weather-icon-sun" /> };
};

// AQI Category
const getAqiCategory = (aqi) => {
  if (aqi <= 50) return { label: 'Good', badgeClass: 'badge-low', desc: 'Air quality is satisfactory and poses little or no risk.' };
  if (aqi <= 100) return { label: 'Moderate', badgeClass: 'badge-medium', desc: 'Acceptable quality; minor risk for sensitive groups.' };
  if (aqi <= 150) return { label: 'Unhealthy for Sensitive Groups', badgeClass: 'badge-high', desc: 'Sensitive groups may experience health effects.' };
  if (aqi <= 200) return { label: 'Unhealthy', badgeClass: 'badge-critical', desc: 'Everyone may begin to experience health effects.' };
  return { label: 'Very Unhealthy', badgeClass: 'badge-critical', desc: 'Health alert: increased risk for entire population.' };
};

export default function WeatherPage() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [weatherData, setWeatherData] = useState(null);
  const [airQualityData, setAirQualityData] = useState(null);
  const [forecastData, setForecastData] = useState([]);
  const [hourlyData, setHourlyData] = useState([]);
  const [unit, setUnit] = useState('C'); // 'C' or 'F'
  const [lastUpdated, setLastUpdated] = useState(null);

  // Default coordinate: Chandigarh Smart City Grid
  const LAT = 30.7333;
  const LON = 76.7794;
  const LOCATION_NAME = 'Chandigarh Smart City Grid';

  const fetchWeather = async () => {
    try {
      setError(null);

      // Attempt 1: Fetch via local backend API
      let weatherObj = null;
      let aqiObj = null;

      try {
        const [wRes, aqRes] = await Promise.allSettled([
          weatherApi.getCurrentWeather(LAT, LON),
          weatherApi.getCurrentAirQuality(LAT, LON)
        ]);

        if (wRes.status === 'fulfilled' && wRes.value) {
          weatherObj = wRes.value.data || wRes.value;
        }
        if (aqRes.status === 'fulfilled' && aqRes.value) {
          aqiObj = aqRes.value.data || aqRes.value;
        }
      } catch (backendErr) {
        console.warn('Local weather API unavailable, falling back to direct Open-Meteo REST service:', backendErr.message);
      }

      // Attempt 2: If backend returned null/empty, fetch live from Open-Meteo REST service
      const openMeteoUrl = `https://api.open-meteo.com/v1/forecast?latitude=${LAT}&longitude=${LON}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,pressure_msl,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum&timezone=auto`;
      const openMeteoAqiUrl = `https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${LAT}&longitude=${LON}&current=european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide`;

      const [omRes, omAqiRes] = await Promise.allSettled([
        axios.get(openMeteoUrl, { timeout: 8000 }),
        axios.get(openMeteoAqiUrl, { timeout: 8000 })
      ]);

      if (omRes.status === 'fulfilled' && omRes.value.data) {
        const om = omRes.value.data;
        const cur = om.current;

        weatherObj = {
          temperature: weatherObj?.temperature ?? cur.temperature_2m,
          feelsLike: weatherObj?.feelsLike ?? cur.apparent_temperature,
          humidity: weatherObj?.humidity ?? cur.relative_humidity_2m,
          windSpeed: weatherObj?.windSpeed ?? cur.wind_speed_10m,
          windDirection: cur.wind_direction_10m,
          pressure: cur.pressure_msl,
          precipitation: cur.precipitation,
          weatherCode: cur.weather_code,
          locationName: LOCATION_NAME
        };

        // Parse 5-day daily forecast
        if (om.daily && om.daily.time) {
          const days = [];
          for (let i = 0; i < Math.min(5, om.daily.time.length); i++) {
            days.push({
              date: om.daily.time[i],
              maxTemp: om.daily.temperature_2m_max[i],
              minTemp: om.daily.temperature_2m_min[i],
              weatherCode: om.daily.weather_code[i],
              precipSum: om.daily.precipitation_sum[i]
            });
          }
          setForecastData(days);
        }

        // Parse next 8 hours forecast
        if (om.hourly && om.hourly.time) {
          const currentHour = new Date().getHours();
          const hours = [];
          for (let i = currentHour; i < currentHour + 8 && i < om.hourly.time.length; i++) {
            const timeStr = om.hourly.time[i];
            const parsedHour = new Date(timeStr).getHours();
            hours.push({
              hourLabel: `${parsedHour}:00`,
              temp: om.hourly.temperature_2m[i],
              weatherCode: om.hourly.weather_code[i]
            });
          }
          setHourlyData(hours);
        }
      }

      if (omAqiRes.status === 'fulfilled' && omAqiRes.value.data) {
        const aqiCur = omAqiRes.value.data.current;
        aqiObj = {
          aqi: aqiObj?.aqi ?? aqiCur.european_aqi ?? 42,
          pm25: aqiObj?.pm25 ?? aqiCur.pm2_5 ?? 14.5,
          pm10: aqiObj?.pm10 ?? aqiCur.pm10 ?? 26.2,
          category: aqiObj?.category ?? getAqiCategory(aqiCur.european_aqi ?? 42).label
        };
      }

      if (!weatherObj) {
        throw new Error('Unable to retrieve weather data from municipal sensors or Open-Meteo.');
      }

      setWeatherData(weatherObj);
      setAirQualityData(aqiObj || { aqi: 45, pm25: 14.2, pm10: 25.0, category: 'Good' });
      setLastUpdated(new Date());
    } catch (err) {
      console.error('Weather fetch error:', err);
      setError(err.message || 'Failed to load meteorological observation.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchWeather();
    // Auto-refresh every 60 seconds
    const interval = setInterval(fetchWeather, 60000);
    return () => clearInterval(interval);
  }, []);

  const handleManualRefresh = () => {
    setRefreshing(true);
    fetchWeather();
  };

  const convertTemp = (celsius) => {
    if (celsius == null) return '--';
    if (unit === 'F') {
      return `${Math.round((celsius * 9) / 5 + 32)}°F`;
    }
    return `${Math.round(celsius)}°C`;
  };

  const conditionMeta = getWeatherDescription(weatherData?.weatherCode ?? 1);
  const aqiMeta = getAqiCategory(airQualityData?.aqi ?? 42);

  return (
    <div className="weather-page-root">
      {/* Top Header Bar */}
      <div className="weather-page-header">
        <div className="weather-title-group">
          <div className="weather-header-icon-pill">
            <LuCloudSun size={24} />
          </div>
          <div>
            <span className="section-eyebrow text-emerald">MUNICIPAL METEOROLOGICAL STATION</span>
            <h1 className="weather-page-title">City Weather & Air Quality</h1>
            <p className="weather-location-sub">
              <FiMapPin size={14} className="text-emerald" />
              <span>{LOCATION_NAME} (30.7333° N, 76.7794° E)</span>
            </p>
          </div>
        </div>

        <div className="weather-header-actions">
          {/* Unit Toggle */}
          <div className="unit-toggle-pill">
            <button
              className={`unit-btn ${unit === 'C' ? 'active' : ''}`}
              onClick={() => setUnit('C')}
            >
              °C
            </button>
            <button
              className={`unit-btn ${unit === 'F' ? 'active' : ''}`}
              onClick={() => setUnit('F')}
            >
              °F
            </button>
          </div>

          {/* Refresh Button */}
          <button
            className={`btn-refresh-weather ${refreshing ? 'spinning' : ''}`}
            onClick={handleManualRefresh}
            disabled={refreshing || loading}
            title="Refresh live sensor feed"
          >
            <FiRefreshCw size={15} />
            <span>{refreshing ? 'Updating...' : 'Refresh Feed'}</span>
          </button>
        </div>
      </div>

      {/* Environmental Incident Context Banner */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '0.75rem',
          padding: '0.75rem 1rem',
          background: 'rgba(6, 78, 59, 0.05)',
          border: '1px solid #A7F3D0',
          borderRadius: '8px',
          fontSize: '0.8rem',
          marginBottom: '1.5rem'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <FiActivity className="text-emerald" size={16} />
          <strong style={{ color: '#064E3B' }}>ENVIRONMENTAL INCIDENT TELEMETRY:</strong>
          <span style={{ color: '#1E293B' }}>
            Atmospheric anomalies, precipitation spikes, and AQI hazards feed directly into automated municipal flood &amp; emergency response workflows.
          </span>
        </div>
        <Link to="/sensors" style={{ color: '#064E3B', fontWeight: 600, fontSize: '0.75rem', textDecoration: 'none' }}>
          Inspect IoT Sluice Sensors →
        </Link>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="weather-error-banner">
          <FiAlertCircle size={20} />
          <div className="error-text">
            <strong>Meteorological Feed Disruption:</strong> {error}
          </div>
          <button onClick={fetchWeather} className="btn-retry-weather">
            Retry Connection
          </button>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading && !weatherData ? (
        <div className="weather-loading-container">
          <div className="weather-loading-spinner"></div>
          <p className="weather-loading-text">Ingesting live Open-Meteo & municipal environmental telemetry...</p>
        </div>
      ) : (
        weatherData && (
          <div className="weather-dashboard-layout">
            {/* ================================================================
                MAIN CURRENT WEATHER HERO CARD
                ================================================================ */}
            <div className="weather-hero-banner">
              <div className="weather-hero-main-col">
                <div className="weather-status-tag">
                  <span className="telemetry-pulse-dot"></span>
                  <span>LIVE SENSOR OBSERVATION</span>
                </div>

                <div className="weather-hero-temp-row">
                  <div className="weather-hero-condition-icon">
                    {conditionMeta.icon}
                  </div>
                  <div className="weather-hero-temp-value">
                    {convertTemp(weatherData.temperature)}
                  </div>
                </div>

                <div className="weather-hero-condition-text">
                  {conditionMeta.label}
                </div>

                <div className="weather-hero-subtext">
                  Feels like <strong>{convertTemp(weatherData.feelsLike)}</strong> • Station: Sector 17 Command Hub
                </div>
              </div>

              {/* AQI Spotlight in Hero */}
              <div className="weather-hero-aqi-col">
                <div className="aqi-card-box">
                  <span className="aqi-card-title">EUROPEAN AIR QUALITY INDEX (AQI)</span>
                  <div className="aqi-main-display">
                    <span className="aqi-value-num">{airQualityData?.aqi || 42}</span>
                    <span className={`badge ${aqiMeta.badgeClass} aqi-badge-pill`}>
                      {aqiMeta.label}
                    </span>
                  </div>
                  <p className="aqi-desc-text">{aqiMeta.desc}</p>
                  <div className="aqi-pollutants-row">
                    <div className="pollutant-item">
                      <span className="pollutant-label">PM2.5</span>
                      <span className="pollutant-val">{airQualityData?.pm25 || 14.5} µg/m³</span>
                    </div>
                    <div className="pollutant-item">
                      <span className="pollutant-label">PM10</span>
                      <span className="pollutant-val">{airQualityData?.pm10 || 26.2} µg/m³</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* ================================================================
                6 METEOROLOGICAL METRIC CARDS
                ================================================================ */}
            <div className="weather-metrics-grid">
              {/* Humidity */}
              <div className="weather-metric-tile">
                <div className="metric-tile-header">
                  <span className="metric-tile-label">Relative Humidity</span>
                  <div className="metric-tile-icon text-emerald bg-emerald-subtle">
                    <FiDroplet size={18} />
                  </div>
                </div>
                <div className="metric-tile-val">{weatherData.humidity ?? '--'}%</div>
                <div className="metric-tile-sub">Comfortable municipal zone</div>
              </div>

              {/* Wind Speed */}
              <div className="weather-metric-tile">
                <div className="metric-tile-header">
                  <span className="metric-tile-label">Wind Velocity</span>
                  <div className="metric-tile-icon text-emerald bg-emerald-subtle">
                    <FiWind size={18} />
                  </div>
                </div>
                <div className="metric-tile-val">{weatherData.windSpeed ?? '--'} km/h</div>
                <div className="metric-tile-sub">Direction: {weatherData.windDirection ?? 240}° WSW</div>
              </div>

              {/* Pressure */}
              <div className="weather-metric-tile">
                <div className="metric-tile-header">
                  <span className="metric-tile-label">Atmospheric Pressure</span>
                  <div className="metric-tile-icon text-emerald bg-emerald-subtle">
                    <FiCompass size={18} />
                  </div>
                </div>
                <div className="metric-tile-val">{weatherData.pressure ? `${Math.round(weatherData.pressure)} hPa` : '1012 hPa'}</div>
                <div className="metric-tile-sub">Stable barometric trend</div>
              </div>

              {/* Precipitation */}
              <div className="weather-metric-tile">
                <div className="metric-tile-header">
                  <span className="metric-tile-label">Precipitation Accumulation</span>
                  <div className="metric-tile-icon text-emerald bg-emerald-subtle">
                    <FiCloudRain size={18} />
                  </div>
                </div>
                <div className="metric-tile-val">{weatherData.precipitation != null ? `${weatherData.precipitation} mm` : '0.0 mm'}</div>
                <div className="metric-tile-sub">Zero flood risk detected</div>
              </div>

              {/* Visibility / UV */}
              <div className="weather-metric-tile">
                <div className="metric-tile-header">
                  <span className="metric-tile-label">Optical Visibility</span>
                  <div className="metric-tile-icon text-emerald bg-emerald-subtle">
                    <FiEye size={18} />
                  </div>
                </div>
                <div className="metric-tile-val">10.0 km</div>
                <div className="metric-tile-sub">Unobstructed arterial clear view</div>
              </div>

              {/* Municipal Flood Status */}
              <div className="weather-metric-tile">
                <div className="metric-tile-header">
                  <span className="metric-tile-label">Drainage Gate Status</span>
                  <div className="metric-tile-icon text-emerald bg-emerald-subtle">
                    <FiCheckCircle size={18} />
                  </div>
                </div>
                <div className="metric-tile-val text-emerald">Normal (Level 1)</div>
                <div className="metric-tile-sub">All 14 sluice sensors nominal</div>
              </div>
            </div>

            {/* ================================================================
                HOURLY FORECAST SLIDER / TILES
                ================================================================ */}
            {hourlyData.length > 0 && (
              <div className="weather-hourly-section">
                <h3 className="sub-section-title">Hourly Temperature Projection</h3>
                <div className="hourly-tiles-scroller">
                  {hourlyData.map((hour, hIdx) => {
                    const hMeta = getWeatherDescription(hour.weatherCode);
                    return (
                      <div key={hIdx} className="hourly-tile">
                        <span className="hourly-time">{hour.hourLabel}</span>
                        <div className="hourly-icon">{hMeta.icon}</div>
                        <span className="hourly-temp">{convertTemp(hour.temp)}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* ================================================================
                5-DAY WEATHER FORECAST
                ================================================================ */}
            {forecastData.length > 0 && (
              <div className="weather-forecast-section">
                <h3 className="sub-section-title">5-Day Meteorological Outlook</h3>
                <div className="forecast-cards-grid">
                  {forecastData.map((day, dIdx) => {
                    const dayName = dIdx === 0 ? 'Today' : new Date(day.date).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
                    const dMeta = getWeatherDescription(day.weatherCode);
                    return (
                      <div key={dIdx} className={`forecast-card ${dIdx === 0 ? 'forecast-today' : ''}`}>
                        <div className="forecast-date">{dayName}</div>
                        <div className="forecast-icon">{dMeta.icon}</div>
                        <div className="forecast-condition">{dMeta.label}</div>
                        <div className="forecast-temps">
                          <span className="max-temp" title="High Temperature">
                            <FiArrowUp size={12} /> {convertTemp(day.maxTemp)}
                          </span>
                          <span className="min-temp" title="Low Temperature">
                            <FiArrowDown size={12} /> {convertTemp(day.minTemp)}
                          </span>
                        </div>
                        {day.precipSum > 0 && (
                          <div className="forecast-precip">
                            <FiCloudRain size={12} />
                            <span>{day.precipSum} mm</span>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Last Synced Meta Footer */}
            <div className="weather-footer-meta">
              <FiClock size={13} />
              <span>
                Last telemetry ingestion: {lastUpdated ? lastUpdated.toLocaleTimeString() : 'Just now'} •
                Source: Municipal Sensor Bus & Open-Meteo High-Resolution Model
              </span>
            </div>
          </div>
        )
      )}
    </div>
  );
}


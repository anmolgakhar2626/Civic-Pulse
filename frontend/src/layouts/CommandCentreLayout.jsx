import React, { useState, useEffect, useRef } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../context/WebSocketContext';
import VerifiedCitizenBadge from '../components/VerifiedCitizenBadge';
import { weatherApi } from '../api/weatherApi';
import { notificationsApi } from '../api/notificationsApi';
import AlertToast from '../components/AlertToast';
import NotificationCenter from '../components/NotificationCenter';
import {
  FiGrid,
  FiMapPin,
  FiAlertTriangle,
  FiActivity,
  FiClock,
  FiFileText,
  FiBarChart2,
  FiSettings,
  FiLogOut,
  FiUser,
  FiCloud,
  FiWind,
  FiBell,
  FiCheck,
  FiShield,
  FiInfo,
  FiHome,
} from 'react-icons/fi';
import { LuCloudSun } from 'react-icons/lu';

export const CommandCentreLayout = () => {
  const { user, logout, hasRole, getPrimaryRole } = useAuth();
  const { connected, alerts } = useWebSocket();
  const navigate = useNavigate();

  const primaryRole = getPrimaryRole ? getPrimaryRole() : (user?.role || 'CITIZEN');

  const handleLogout = async () => {
    try {
      await logout();
    } catch (err) {
      console.warn('Logout warning:', err);
    } finally {
      navigate('/login');
    }
  };

  const [weather, setWeather] = useState(null);
  const [airQuality, setAirQuality] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showNotifications, setShowNotifications] = useState(false);
  const notifRef = useRef(null);

  const fetchUnreadCount = async () => {
    try {
      const res = await notificationsApi.getUnreadCount();
      const data = res?.data || res;
      if (data && data.unreadCount != null) {
        setUnreadCount(Number(data.unreadCount));
      }
    } catch (err) {
      console.warn('Failed to load unread notification count', err);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 20000);

    const handleNotifEvent = () => {
      fetchUnreadCount();
    };
    window.addEventListener('civicpulse_notification_received', handleNotifEvent);

    return () => {
      clearInterval(interval);
      window.removeEventListener('civicpulse_notification_received', handleNotifEvent);
    };
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setShowNotifications(false);
      }
    };
    if (showNotifications) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showNotifications]);

  useEffect(() => {
    if (alerts && alerts.length > 0) {
      fetchUnreadCount();
    }
  }, [alerts]);

  const getDashboardLabel = () => {
    switch (primaryRole) {
      case 'ADMIN': return 'Admin Command Center';
      case 'COMMAND_OPERATOR': return 'Command Dashboard';
      case 'DEPARTMENT_OFFICER': return `${user?.departmentCode || 'Dept'} Operations`;
      case 'FIELD_WORKER': return 'Field Task Board';
      case 'CITIZEN': return 'Citizen Resident Portal';
      default: return 'Command Dashboard';
    }
  };

  const getRoleBadgeStyle = (r) => {
    switch (r) {
      case 'ADMIN': return { backgroundColor: '#7c3aed', color: '#fff' };
      case 'COMMAND_OPERATOR': return { backgroundColor: '#0284c7', color: '#fff' };
      case 'DEPARTMENT_OFFICER': return { backgroundColor: '#d97706', color: '#fff' };
      case 'FIELD_WORKER': return { backgroundColor: '#0d9488', color: '#fff' };
      case 'CITIZEN': return { backgroundColor: '#64748b', color: '#fff' };
      default: return { backgroundColor: '#334155', color: '#fff' };
    }
  };

  const navItems = [
    {
      to: '/dashboard',
      label: getDashboardLabel(),
      icon: <FiGrid size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN'],
    },
    {
      to: '/map',
      label: primaryRole === 'CITIZEN' ? 'Public Safety Map' : 'Live GIS Map',
      icon: <FiMapPin size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN'],
    },
    {
      to: '/incidents',
      label: primaryRole === 'FIELD_WORKER' ? 'Field Work Orders' : 'Incident Management',
      icon: <FiAlertTriangle size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER'],
    },
    {
      to: '/sensors',
      label: 'IoT Sensor Grid',
      icon: <FiActivity size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER'],
    },
    {
      to: '/sla',
      label: 'SLA Escalation Board',
      icon: <FiClock size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER'],
    },
    {
      to: '/complaints',
      label: primaryRole === 'CITIZEN' ? 'Civic Grievances' : 'Citizen Complaints',
      icon: <FiFileText size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'CITIZEN'],
    },
    {
      to: '/analytics',
      label: 'City Analytics',
      icon: <FiBarChart2 size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER'],
    },
    {
      to: '/weather',
      label: 'Weather Station',
      icon: <LuCloudSun size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN'],
    },
    {
      to: '/about',
      label: 'About CivicPulse',
      icon: <FiInfo size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN'],
    },
    {
      to: '/notifications',
      label: 'Notification Center',
      icon: <FiBell size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN'],
    },
    {
      to: '/settings',
      label: 'System & Settings',
      icon: <FiSettings size={16} />,
      roles: ['ADMIN', 'COMMAND_OPERATOR'],
    },
  ];

  const allowedNav = navItems.filter((item) => hasRole(item.roles));

  return (
    <div className="command-centre-layout">
      {/* Sidebar */}
      <aside className="cc-sidebar">
        <div className="cc-sidebar-header">
          <div className="cc-brand">
            <div className="cc-brand-icon">
              <FiMapPin />
            </div>
            <div>
              <div className="cc-brand-title">CivicPulse</div>
              <div className="cc-brand-subtitle">Municipal Command Centre</div>
            </div>
          </div>
        </div>

        <nav className="cc-nav">
          <div className="cc-nav-section-title">Operations Control</div>
          {allowedNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `cc-nav-link ${isActive ? 'active' : ''}`
              }
            >
              {item.icon}
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="cc-sidebar-footer">
          <div className="cc-user-card">
            <div className="cc-user-avatar">
              <FiUser />
            </div>
            <div className="cc-user-info">
              <div className="cc-user-name" title={user?.email}>{user?.fullName || user?.username || 'User'}</div>
              <div className="cc-user-role" style={{ marginTop: '2px' }}>
                <span className="badge" style={{ padding: '2px 6px', fontSize: '0.65rem', fontWeight: 600, ...getRoleBadgeStyle(primaryRole) }}>
                  {primaryRole}
                </span>
                {(user?.isVerifiedCitizen || user?.verifiedCitizen) && (
                  <div style={{ marginTop: '4px' }}>
                    <VerifiedCitizenBadge isVerified={true} genuineCount={user.genuineComplaintCount || 5} size="sm" />
                  </div>
                )}
                {user?.departmentName && (
                  <div style={{ fontSize: '0.675rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    {user.departmentName}
                  </div>
                )}
              </div>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="btn btn-outline btn-sm"
            style={{ width: '100%' }}
          >
            <FiLogOut size={13} />
            Sign Out
          </button>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '0.85rem', marginTop: '0.5rem' }}>
            <NavLink to="/" className="cc-footer-link">
              <FiHome size={11} />
              <span>Public Portal</span>
            </NavLink>
            <NavLink to="/privacy" className="cc-footer-link">
              <FiShield size={11} />
              <span>Privacy</span>
            </NavLink>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="cc-main-area">
        {/* Top Command Bar */}
        <header className="cc-topbar">
          <div className="cc-topbar-left">
            <div className={`cc-live-indicator ${connected ? '' : 'offline'}`}>
              <div className={`cc-live-dot ${connected ? '' : 'offline'}`} />
              {connected ? 'LIVE TELEMETRY ACTIVE' : 'RECONNECTING FEED...'}
            </div>

            <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>
              Jurisdiction: <strong>Chandigarh Smart City Grid</strong>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {/* Environmental Conditions from real Open-Meteo */}
            <div className="cc-topbar-telemetry">
              {weather && (
                <div className="cc-telemetry-badge" title="Open-Meteo Weather Observation">
                  <FiCloud size={13} color="var(--accent-primary)" />
                  <span>
                    <strong>{weather.temperature != null ? `${weather.temperature}°C` : '--'}</strong>, {weather.humidity}% RH
                  </span>
                </div>
              )}

              {airQuality && (
                <div className="cc-telemetry-badge" title="Open-Meteo Air Quality Observation">
                  <FiWind size={13} color="var(--accent-teal)" />
                  <span>
                    AQI: <strong>{airQuality.aqi || '--'}</strong> ({airQuality.category || 'NORMAL'})
                  </span>
                </div>
              )}
            </div>

            {/* Notification Bell Dropdown */}
            <div className="cc-notification-wrapper" ref={notifRef}>
              <button
                className="cc-notification-btn"
                title="Operational Notifications"
                onClick={() => setShowNotifications((prev) => !prev)}
              >
                <FiBell size={16} />
                {unreadCount > 0 && (
                  <span className="cc-notification-badge">{unreadCount}</span>
                )}
              </button>

              {showNotifications && (
                <NotificationCenter
                  onClose={() => setShowNotifications(false)}
                  onCountChange={setUnreadCount}
                />
              )}
            </div>
          </div>
        </header>

        {/* Global STOMP Critical Alerts Toast */}
        <AlertToast />

        {/* Child Page Outlet */}
        <main className="cc-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default CommandCentreLayout;

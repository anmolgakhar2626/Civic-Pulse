import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { WebSocketProvider } from './context/WebSocketContext';
import CommandCentreLayout from './layouts/CommandCentreLayout';
import PublicLayout from './layouts/PublicLayout';

import HomePage from './pages/HomePage';
import AboutPage from './pages/AboutPage';
import WeatherPage from './pages/WeatherPage';
import LoginPage from './pages/LoginPage';
import SignUpPage from './pages/SignUpPage';
import DashboardPage from './pages/DashboardPage';
import IncidentsPage from './pages/IncidentsPage';
import IncidentDetailPage from './pages/IncidentDetailPage';
import LiveMapPage from './pages/LiveMapPage';
import SensorsPage from './pages/SensorsPage';
import SlaBoardPage from './pages/SlaBoardPage';
import ComplaintsPage from './pages/ComplaintsPage';
import AnalyticsPage from './pages/AnalyticsPage';
import SettingsPage from './pages/SettingsPage';
import NotificationsPage from './pages/NotificationsPage';
import PrivacyPolicyPage from './pages/PrivacyPolicyPage';
import LoadingSpinner from './components/LoadingSpinner';

// Route guard for authenticated users with role restrictions
const ProtectedRoute = ({ children, allowedRoles = [] }) => {
  const { isAuthenticated, loading, hasRole, getPrimaryRole } = useAuth();

  if (loading) {
    return <LoadingSpinner text="Authenticating municipal session..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles.length > 0 && !hasRole(allowedRoles)) {
    const currentRole = getPrimaryRole ? getPrimaryRole() : 'USER';
    return (
      <div style={{ padding: '3.5rem 1.5rem', textAlign: 'center', maxWidth: '480px', margin: '4rem auto' }}>
        <div style={{ width: '48px', height: '48px', borderRadius: '50%', backgroundColor: '#fef2f2', color: '#ef4444', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.5rem', marginBottom: '1rem' }}>
          ⚠️
        </div>
        <h2 style={{ fontSize: '1.35rem', fontWeight: 700, marginBottom: '0.5rem', color: '#0f172a' }}>
          Access Denied
        </h2>
        <p style={{ color: '#64748b', fontSize: '0.875rem', marginBottom: '0.75rem' }}>
          Your role (<span style={{ fontWeight: 600, color: '#ef4444' }}>{currentRole}</span>) does not have authorization to view this resource.
        </p>
        <p style={{ color: '#94a3b8', fontSize: '0.775rem', marginBottom: '1.5rem' }}>
          Authorized roles: {allowedRoles.join(', ')}
        </p>
        <Link
          to="/dashboard"
          className="btn btn-primary"
          style={{ textDecoration: 'none', display: 'inline-block', padding: '0.5rem 1.25rem', borderRadius: '6px', fontWeight: 600, fontSize: '0.85rem' }}
        >
          Return to My Dashboard
        </Link>
      </div>
    );
  }

  return children;
};

export default function App() {
  return (
    <AuthProvider>
      <WebSocketProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Layout and Marketing/Overview Pages */}
            <Route element={<PublicLayout />}>
              <Route path="/" element={<HomePage />} />
              <Route path="/about" element={<AboutPage />} />
              <Route path="/weather" element={<WeatherPage />} />
              <Route path="/privacy" element={<PrivacyPolicyPage />} />
            </Route>

            {/* Standalone Authentication Pages */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignUpPage />} />
            <Route path="/register" element={<SignUpPage />} />

            {/* Protected Command Centre Operating Portal */}
            <Route
              element={
                <ProtectedRoute>
                  <CommandCentreLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/dashboard" element={<DashboardPage />} />

              <Route
                path="/incidents"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER']}>
                    <IncidentsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/incidents/:id"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN']}>
                    <IncidentDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/map"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN']}>
                    <LiveMapPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/sensors"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER']}>
                    <SensorsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/sla"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER']}>
                    <SlaBoardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/complaints"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'CITIZEN']}>
                    <ComplaintsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/analytics"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER']}>
                    <AnalyticsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/notifications"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR', 'DEPARTMENT_OFFICER', 'FIELD_WORKER', 'CITIZEN']}>
                    <NotificationsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/settings"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'COMMAND_OPERATOR']}>
                    <SettingsPage />
                  </ProtectedRoute>
                }
              />
            </Route>

            {/* Catch-all fallback redirects to home */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </WebSocketProvider>
    </AuthProvider>
  );
}

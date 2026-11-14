import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/authApi';
import {
  FiLock,
  FiUser,
  FiShield,
  FiAlertCircle,
  FiEye,
  FiEyeOff,
  FiLayers,
  FiSmartphone,
  FiCheckCircle,
  FiPhone,
  FiKey
} from 'react-icons/fi';

export const LoginPage = () => {
  // Authentication Mode: 'PASSWORD' or 'OTP'
  const [authMode, setAuthMode] = useState('PASSWORD');

  // Option A: Password Login State (Preserved 100%)
  const [identifier, setIdentifier] = useState('admin');
  const [password, setPassword] = useState('Admin@CivicPulse2026');
  const [selectedRole, setSelectedRole] = useState('ADMIN');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [validationErrors, setValidationErrors] = useState({});
  const [loading, setLoading] = useState(false);

  // Option B: Citizen Mobile OTP Login State
  const [countryCode, setCountryCode] = useState('+91');
  const [mobileNumber, setMobileNumber] = useState('');
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [otpSending, setOtpSending] = useState(false);
  const [otpVerifying, setOtpVerifying] = useState(false);
  const [otpCooldown, setOtpCooldown] = useState(0);
  const [otpMessage, setOtpMessage] = useState('');
  const [otpError, setOtpError] = useState('');

  const { login, loginWithOtp, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // Cooldown countdown timer for OTP resend
  useEffect(() => {
    let timer;
    if (otpCooldown > 0) {
      timer = setInterval(() => {
        setOtpCooldown((prev) => (prev > 1 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [otpCooldown]);

  // Switch tabs cleanly
  const handleTabChange = (mode) => {
    setAuthMode(mode);
    setError('');
    setOtpError('');
    setOtpMessage('');
    setValidationErrors({});
  };

  // Password Login Validation & Submission
  const validatePasswordForm = () => {
    const errors = {};
    if (!identifier.trim()) {
      errors.identifier = 'Username or email is required';
    }
    if (!password) {
      errors.password = 'Password is required';
    } else if (password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
    }
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handlePasswordSubmit = async (e) => {
    e?.preventDefault();
    setError('');

    if (!validatePasswordForm()) {
      return;
    }

    setLoading(true);

    try {
      await login(identifier.trim(), password);
      navigate('/dashboard');
    } catch (err) {
      const status = err.status || err.response?.status;
      if (status === 401) {
        setError('Invalid credentials. Please verify your municipal username and password.');
      } else if (status === 400) {
        setError(err.response?.data?.message || 'Validation error: please enter valid credentials.');
      } else if (status === 500) {
        setError('Municipal backend server error. Please ensure database and backend services are operational.');
      } else {
        const serverMsg = err.response?.data?.message || err.message;
        setError(serverMsg || 'Authentication failed. Please verify credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Compact Role Options for Municipal Incident Operations
  const roleOptions = [
    { role: 'ADMIN', username: 'admin', label: 'Admin (System Management)' },
    { role: 'COMMAND_OPERATOR', username: 'operator', label: 'Operator (City Operations)' },
    { role: 'DEPARTMENT_OFFICER', username: 'officer_traffic', label: 'Traffic Officer (Traffic Management)' },
    { role: 'FIELD_WORKER', username: 'field_worker', label: 'Field Worker (On-site Tasks)' },
    { role: 'CITIZEN', username: 'citizen', label: 'Citizen / Resident (Grievances & Incident Reporting)' },
  ];

  const handleRoleChange = (e) => {
    const newRole = e.target.value;
    setSelectedRole(newRole);
    const matched = roleOptions.find((r) => r.role === newRole);
    if (matched) {
      setIdentifier(matched.username);
      setPassword('Admin@CivicPulse2026');
      setError('');
      setValidationErrors({});
    }
  };

  // Option B: Citizen Mobile OTP Handlers
  const handleSendLoginOtp = async () => {
    setOtpError('');
    setOtpMessage('');

    const cleanNumber = mobileNumber.replace(/\D/g, '');
    if (cleanNumber.length < 10) {
      setOtpError('Please enter a valid 10-digit mobile number.');
      return;
    }

    const fullMobile = `${countryCode}${cleanNumber}`;
    setOtpSending(true);

    try {
      const res = await authApi.sendLoginOtp(fullMobile);
      const data = res.data || res;
      if (data.success !== false) {
        setOtpSent(true);
        setOtpCooldown(data.cooldownSeconds || 60);
        setOtpMessage(data.message || 'Demo OTP generated. Check the local backend terminal.');
      } else {
        setOtpError(data.message || 'Unable to send OTP. Please check mobile number.');
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message;
      setOtpError(msg || 'Unable to authenticate. Please check your details.');
    } finally {
      setOtpSending(false);
    }
  };

  const handleVerifyLoginOtp = async (e) => {
    e?.preventDefault();
    setOtpError('');

    if (!otp || otp.trim().length !== 6) {
      setOtpError('Please enter the complete 6-digit OTP code.');
      return;
    }

    const cleanNumber = mobileNumber.replace(/\D/g, '');
    const fullMobile = `${countryCode}${cleanNumber}`;
    setOtpVerifying(true);

    try {
      await loginWithOtp(fullMobile, otp.trim());
      navigate('/dashboard');
    } catch (err) {
      const msg = err.response?.data?.message || err.message;
      if (msg.includes('expired')) {
        setOtpError('OTP expired. Request a new OTP.');
      } else if (msg.includes('Invalid') || msg.includes('attempts')) {
        setOtpError(msg);
      } else {
        setOtpError(msg || 'Unable to authenticate. Please check your details.');
      }
    } finally {
      setOtpVerifying(false);
    }
  };

  return (
    <div className="login-page-wrapper">
      <div className="login-card-container">
        {/* Navigation Link */}
        <div className="login-back-nav">
          <Link to="/" className="login-back-link">
            ← Return to Incident Portal
          </Link>
        </div>

        {/* Minimal Clean Branding */}
        <div className="login-brand-header">
          <div className="login-brand-icon">
            <FiShield size={24} />
          </div>
          <h1 className="login-brand-title">CivicPulse</h1>
          <p className="login-brand-subtitle">Municipal Incident &amp; Complaint Command Centre</p>
        </div>

        {/* Dual Authentication Tabs */}
        <div className="login-auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={authMode === 'PASSWORD'}
            className={`login-auth-tab ${authMode === 'PASSWORD' ? 'active' : ''}`}
            onClick={() => handleTabChange('PASSWORD')}
          >
            <FiLock size={14} />
            <span>Password Sign In</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={authMode === 'OTP'}
            className={`login-auth-tab ${authMode === 'OTP' ? 'active' : ''}`}
            onClick={() => handleTabChange('OTP')}
          >
            <FiSmartphone size={14} />
            <span>Citizen Mobile OTP</span>
          </button>
        </div>

        {/* Global Error Alert */}
        {error && (
          <div className="login-error-alert" role="alert">
            <FiAlertCircle size={16} className="login-error-icon" />
            <span>{error}</span>
          </div>
        )}

        {/* =====================================================================
            TAB 1: PASSWORD LOGIN (100% PRESERVED EXISTING FUNCTIONALITY)
            ===================================================================== */}
        {authMode === 'PASSWORD' && (
          <form onSubmit={handlePasswordSubmit} noValidate className="login-form">
            {/* Username / Email */}
            <div className="form-group-field">
              <label className="form-field-label">Username or Email</label>
              <div className="form-input-relative">
                <input
                  type="text"
                  className={`login-input ${validationErrors.identifier ? 'error' : ''}`}
                  value={identifier}
                  onChange={(e) => {
                    setIdentifier(e.target.value);
                    if (validationErrors.identifier) {
                      setValidationErrors((prev) => ({ ...prev, identifier: '' }));
                    }
                  }}
                  placeholder="Enter municipal username or email"
                  autoComplete="username"
                  required
                />
                <FiUser size={15} className="input-leading-icon" />
              </div>
              {validationErrors.identifier && (
                <span className="field-error-text">{validationErrors.identifier}</span>
              )}
            </div>

            {/* Password */}
            <div className="form-group-field">
              <label className="form-field-label">Password</label>
              <div className="form-input-relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  className={`login-input ${validationErrors.password ? 'error' : ''}`}
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (validationErrors.password) {
                      setValidationErrors((prev) => ({ ...prev, password: '' }));
                    }
                  }}
                  placeholder="Enter password"
                  autoComplete="current-password"
                  required
                />
                <FiLock size={15} className="input-leading-icon" />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="input-password-toggle"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <FiEyeOff size={15} /> : <FiEye size={15} />}
                </button>
              </div>
              {validationErrors.password && (
                <span className="field-error-text">{validationErrors.password}</span>
              )}
            </div>

            {/* COMPACT ROLE SELECTION */}
            <div className="form-group-field">
              <label className="form-field-label">
                <span>Operational Role Selection</span>
              </label>
              <div className="form-input-relative">
                <select
                  className="login-input login-role-select"
                  value={selectedRole}
                  onChange={handleRoleChange}
                  aria-label="Select operational role for authentication"
                >
                  {roleOptions.map((opt) => (
                    <option key={opt.role} value={opt.role}>
                      {opt.label}
                    </option>
                  ))}
                </select>
                <FiLayers size={15} className="input-leading-icon" />
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="login-submit-button"
              disabled={loading}
            >
              {loading ? 'Authenticating Session...' : `Sign In as ${selectedRole.replace('_', ' ')}`}
            </button>
          </form>
        )}

        {/* =====================================================================
            TAB 2: CITIZEN MOBILE OTP LOGIN (INCREMENTAL ADDITION)
            ===================================================================== */}
        {authMode === 'OTP' && (
          <div className="login-form">
            {/* OTP Status Messages */}
            {otpMessage && (
              <div className="login-otp-demo-notice" role="status">
                <FiCheckCircle size={16} style={{ color: '#10B981', flexShrink: 0, marginTop: '2px' }} />
                <span>{otpMessage}</span>
              </div>
            )}

            {otpError && (
              <div className="login-error-alert" role="alert">
                <FiAlertCircle size={16} className="login-error-icon" />
                <span>{otpError}</span>
              </div>
            )}

            {/* Mobile Number Input */}
            <div className="form-group-field">
              <label className="form-field-label">Registered Citizen Mobile Number</label>
              <div className="login-mobile-input-group">
                <select
                  className="login-country-code-select"
                  value={countryCode}
                  onChange={(e) => setCountryCode(e.target.value)}
                  disabled={otpSent}
                  aria-label="Country Code"
                >
                  <option value="+91">+91 (IN)</option>
                  <option value="+1">+1 (US)</option>
                  <option value="+44">+44 (UK)</option>
                </select>
                <div className="form-input-relative" style={{ flex: 1 }}>
                  <input
                    type="tel"
                    className="login-input"
                    value={mobileNumber}
                    onChange={(e) => {
                      const digits = e.target.value.replace(/\D/g, '').slice(0, 10);
                      setMobileNumber(digits);
                    }}
                    placeholder="10-digit mobile number"
                    disabled={otpSent}
                    autoComplete="tel"
                    required
                  />
                  <FiPhone size={15} className="input-leading-icon" />
                </div>
              </div>
            </div>

            {/* Step 1: Send OTP Button */}
            {!otpSent && (
              <button
                type="button"
                className="login-submit-button"
                onClick={handleSendLoginOtp}
                disabled={otpSending || mobileNumber.length < 10 || otpCooldown > 0}
              >
                {otpSending
                  ? 'Dispatching Verification Code...'
                  : otpCooldown > 0
                  ? `Resend in ${otpCooldown}s`
                  : 'Send Login OTP'}
              </button>
            )}

            {/* Step 2: OTP Verification Controls */}
            {otpSent && (
              <form onSubmit={handleVerifyLoginOtp}>
                <div className="form-group-field">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                    <label className="form-field-label" style={{ margin: 0 }}>Enter 6-Digit OTP</label>
                    <button
                      type="button"
                      className="login-otp-resend-btn"
                      onClick={handleSendLoginOtp}
                      disabled={otpSending || otpCooldown > 0}
                    >
                      {otpSending ? 'Sending...' : otpCooldown > 0 ? `Resend in ${otpCooldown}s` : 'Resend Code'}
                    </button>
                  </div>
                  <div className="form-input-relative">
                    <input
                      type="text"
                      className="login-input"
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      placeholder="6-digit verification code"
                      maxLength={6}
                      autoComplete="one-time-code"
                      required
                    />
                    <FiKey size={15} className="input-leading-icon" />
                  </div>
                </div>

                <button
                  type="submit"
                  className="login-submit-button"
                  disabled={otpVerifying || otp.length !== 6}
                >
                  {otpVerifying ? 'Verifying & Signing In...' : 'Verify & Sign In'}
                </button>

                <div style={{ textAlign: 'center', marginTop: '0.85rem' }}>
                  <button
                    type="button"
                    onClick={() => {
                      setOtpSent(false);
                      setOtp('');
                      setOtpMessage('');
                      setOtpError('');
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#64748B',
                      fontSize: '0.785rem',
                      cursor: 'pointer',
                      textDecoration: 'underline'
                    }}
                  >
                    Change mobile number
                  </button>
                </div>
              </form>
            )}
          </div>
        )}

        {/* Footer Link */}
        <div className="login-footer-row">
          <span>Need to submit a citizen grievance?</span>{' '}
          <Link to="/signup" className="register-link">
            Register Citizen Account
          </Link>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;

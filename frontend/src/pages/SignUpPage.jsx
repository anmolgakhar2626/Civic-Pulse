import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/authApi';
import {
  FiShield,
  FiUser,
  FiMail,
  FiLock,
  FiPhone,
  FiAlertCircle,
  FiCheckCircle,
  FiEye,
  FiEyeOff,
  FiArrowLeft,
  FiKey,
  FiClock
} from 'react-icons/fi';

export const SignUpPage = () => {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    username: '',
    phone: '',
    role: 'CITIZEN',
    password: '',
    confirmPassword: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // Mobile OTP state
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [otpSending, setOtpSending] = useState(false);
  const [otpCooldown, setOtpCooldown] = useState(0);
  const [otpVerified, setOtpVerified] = useState(false);
  const [otpMessage, setOtpMessage] = useState('');
  const [otpError, setOtpError] = useState('');

  const { register } = useAuth();
  const navigate = useNavigate();

  // Cooldown countdown timer
  useEffect(() => {
    let timer;
    if (otpCooldown > 0) {
      timer = setInterval(() => {
        setOtpCooldown((prev) => (prev > 1 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [otpCooldown]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

    if (name === 'phone') {
      setOtpVerified(false);
      setOtpSent(false);
      setOtp('');
      setOtpMessage('');
      setOtpError('');
    }

    if (name === 'email' && (!formData.username || formData.username === formData.email.split('@')[0])) {
      const derived = value.split('@')[0] || '';
      setFormData((prev) => ({ ...prev, username: derived }));
    }

    if (validationErrors[name]) {
      setValidationErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleSendOtp = async () => {
    setOtpError('');
    setOtpMessage('');
    const rawPhone = formData.phone.trim();
    if (!rawPhone || rawPhone.length < 10) {
      setOtpError('Please enter a valid 10-digit mobile number before requesting OTP.');
      return;
    }

    setOtpSending(true);
    try {
      const res = await authApi.sendOtp(rawPhone, 'REGISTRATION');
      const data = res.data || res;
      if (data.success) {
        setOtpSent(true);
        setOtp('');
        setOtpCooldown(data.cooldownSeconds || 60);
        setOtpMessage(data.message || 'Demo OTP generated successfully. Check the local backend terminal.');
      } else {
        setOtpError(data.message || 'Failed to send OTP. Please check mobile number.');
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message;
      setOtpError(msg || 'Failed to send verification code.');
    } finally {
      setOtpSending(false);
    }
  };

  const handleVerifyOtp = async () => {
    setOtpError('');
    setOtpMessage('');
    const rawPhone = formData.phone.trim();
    if (!otp || otp.trim().length !== 6) {
      setOtpError('Please enter the complete 6-digit OTP code.');
      return;
    }

    try {
      const res = await authApi.verifyOtp(rawPhone, otp.trim(), 'REGISTRATION');
      const data = res.data || res;
      if (data.success) {
        setOtpVerified(true);
        setOtpMessage('Mobile number verified successfully!');
      } else {
        setOtpError(data.message || 'Invalid or expired OTP. Please check backend terminal.');
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message;
      setOtpError(msg || 'OTP verification failed. Check backend console.');
    }
  };

  const validate = () => {
    const errors = {};
    if (!formData.fullName.trim()) {
      errors.fullName = 'Full name is required';
    } else if (formData.fullName.trim().length < 2) {
      errors.fullName = 'Full name must be at least 2 characters';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email.trim()) {
      errors.email = 'Email address is required';
    } else if (!emailRegex.test(formData.email.trim())) {
      errors.email = 'Please enter a valid email address';
    }

    if (!formData.username.trim()) {
      errors.username = 'Username is required';
    } else if (formData.username.trim().length < 3) {
      errors.username = 'Username must be at least 3 characters';
    }

    if (!formData.phone.trim()) {
      errors.phone = 'Mobile number is required for OTP verification';
    } else if (formData.phone.trim().length < 10) {
      errors.phone = 'Please enter a valid phone number';
    } else if (!otpVerified) {
      errors.phone = 'Please verify your mobile number with OTP first';
    }

    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
    }

    if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!validate()) {
      return;
    }

    setLoading(true);

    try {
      const payload = {
        fullName: formData.fullName.trim(),
        email: formData.email.trim().toLowerCase(),
        username: formData.username.trim(),
        phone: formData.phone.trim() || undefined,
        role: formData.role,
        password: formData.password,
      };

      await register(payload);
      setSuccess('Citizen registration successful! Redirecting to command portal login...');
      setTimeout(() => {
        navigate('/login');
      }, 1500);
    } catch (err) {
      const status = err.status || err.response?.status;
      if (status === 409) {
        setError('An account with this email or username already exists. Please sign in instead.');
      } else if (status === 400) {
        setError(err.response?.data?.message || 'Invalid registration details. Please verify all fields.');
      } else if (status === 500) {
        setError('Server error during registration. Please ensure database connectivity.');
      } else {
        const serverMsg = err.response?.data?.message || err.message;
        setError(serverMsg || 'Registration failed. Please check the provided information.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="public-page-bg">
      <div className="public-card" style={{ maxWidth: '520px' }}>
        <div style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Link to="/" style={{ color: '#10B981', fontSize: '0.8rem', fontWeight: 600, textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <FiArrowLeft size={14} /> Back to Home
          </Link>
          <Link to="/login" style={{ color: '#64748b', fontSize: '0.78rem', textDecoration: 'none' }}>
            Already registered? Sign In
          </Link>
        </div>

        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <div className="public-brand-icon">
            <FiShield size={26} />
          </div>
          <h1 className="public-title">CivicPulse</h1>
          <p className="public-subtitle">
            Citizen &amp; Operator Portal Registration
          </p>
        </div>

        {error && (
          <div
            style={{
              backgroundColor: '#fef2f2',
              color: '#b91c1c',
              border: '1px solid #fecaca',
              padding: '0.65rem 0.85rem',
              borderRadius: '8px',
              fontSize: '0.8rem',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <FiAlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div
            style={{
              backgroundColor: '#f0fdf4',
              color: '#15803d',
              border: '1px solid #bbf7d0',
              padding: '0.65rem 0.85rem',
              borderRadius: '8px',
              fontSize: '0.8rem',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <FiCheckCircle size={16} />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {/* Full Name */}
          <div style={{ marginBottom: '0.85rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem', color: '#1E293B' }}>
              Full Name *
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type="text"
                name="fullName"
                className="cc-input"
                style={{
                  width: '100%',
                  padding: '0.55rem 0.75rem 0.55rem 2.25rem',
                  borderRadius: '8px',
                  border: validationErrors.fullName ? '1px solid #ef4444' : '1px solid #cbd5e1',
                  fontSize: '0.875rem',
                }}
                value={formData.fullName}
                onChange={handleChange}
                placeholder="e.g. Eleanor Vance"
                required
              />
              <FiUser
                size={14}
                style={{ position: 'absolute', left: '10px', top: '11px', color: '#64748b' }}
              />
            </div>
            {validationErrors.fullName && (
              <span style={{ fontSize: '0.75rem', color: '#ef4444', marginTop: '0.2rem', display: 'block' }}>
                {validationErrors.fullName}
              </span>
            )}
          </div>

          {/* Email and Username */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '0.85rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem', color: '#1E293B' }}>
                Email Address *
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="email"
                  name="email"
                  className="cc-input"
                  style={{
                    width: '100%',
                    padding: '0.55rem 0.75rem 0.55rem 2.25rem',
                    borderRadius: '8px',
                    border: validationErrors.email ? '1px solid #ef4444' : '1px solid #cbd5e1',
                    fontSize: '0.875rem',
                  }}
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="name@city.gov"
                  required
                />
                <FiMail
                  size={14}
                  style={{ position: 'absolute', left: '10px', top: '11px', color: '#64748b' }}
                />
              </div>
              {validationErrors.email && (
                <span style={{ fontSize: '0.75rem', color: '#ef4444', marginTop: '0.2rem', display: 'block' }}>
                  {validationErrors.email}
                </span>
              )}
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem', color: '#1E293B' }}>
                Username *
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="text"
                  name="username"
                  className="cc-input"
                  style={{
                    width: '100%',
                    padding: '0.55rem 0.75rem 0.55rem 2.25rem',
                    borderRadius: '8px',
                    border: validationErrors.username ? '1px solid #ef4444' : '1px solid #cbd5e1',
                    fontSize: '0.875rem',
                  }}
                  value={formData.username}
                  onChange={handleChange}
                  placeholder="username"
                  required
                />
                <FiUser
                  size={14}
                  style={{ position: 'absolute', left: '10px', top: '11px', color: '#64748b' }}
                />
              </div>
              {validationErrors.username && (
                <span style={{ fontSize: '0.75rem', color: '#ef4444', marginTop: '0.2rem', display: 'block' }}>
                  {validationErrors.username}
                </span>
              )}
            </div>
          </div>

          {/* Phone and Role */}
          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: '0.75rem', marginBottom: '0.85rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem', color: '#1E293B' }}>
                Mobile Number (SMS OTP) *
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type="tel"
                  name="phone"
                  className="cc-input"
                  style={{
                    width: '100%',
                    padding: '0.55rem 0.75rem 0.55rem 2.25rem',
                    borderRadius: '8px',
                    border: validationErrors.phone ? '1px solid #ef4444' : otpVerified ? '1px solid #10B981' : '1px solid #cbd5e1',
                    fontSize: '0.875rem',
                    backgroundColor: otpVerified ? '#f0fdf4' : '#ffffff',
                  }}
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="+919876543210"
                  required
                />
                <FiPhone
                  size={14}
                  style={{ position: 'absolute', left: '10px', top: '11px', color: otpVerified ? '#10B981' : '#64748b' }}
                />
                {otpVerified && (
                  <FiCheckCircle
                    size={16}
                    style={{ position: 'absolute', right: '10px', top: '10px', color: '#10B981' }}
                  />
                )}
              </div>
              {validationErrors.phone && (
                <span style={{ fontSize: '0.75rem', color: '#ef4444', marginTop: '0.2rem', display: 'block' }}>
                  {validationErrors.phone}
                </span>
              )}
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem', color: '#1E293B' }}>
                Municipal Role
              </label>
              <select
                name="role"
                className="cc-input"
                style={{
                  width: '100%',
                  padding: '0.55rem 0.75rem',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.875rem',
                  backgroundColor: '#ffffff',
                }}
                value={formData.role}
                onChange={handleChange}
              >
                <option value="CITIZEN">Citizen (Resident)</option>
                <option value="COMMAND_OPERATOR">Command Operator</option>
                <option value="DEPARTMENT_OFFICER">Department Officer</option>
                <option value="FIELD_WORKER">Field Worker</option>
              </select>
            </div>
          </div>

          {/* OTP Verification Section */}
          {!otpVerified && (
            <div style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '0.75rem', marginBottom: '0.85rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#334155' }}>
                  Mobile Security Verification
                </span>
                <button
                  type="button"
                  onClick={handleSendOtp}
                  disabled={otpSending || otpCooldown > 0}
                  className="btn btn-outline btn-sm"
                  style={{ fontSize: '0.725rem', padding: '3px 8px', borderRadius: '4px' }}
                >
                  {otpSending ? 'Sending...' : otpCooldown > 0 ? `Resend in ${otpCooldown}s` : otpSent ? 'Resend OTP' : 'Send OTP'}
                </button>
              </div>

              {otpSent && (
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <div style={{ position: 'relative', flex: 1 }}>
                    <input
                      type="text"
                      maxLength={6}
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                      placeholder="6-digit OTP"
                      className="cc-input"
                      style={{ width: '100%', padding: '0.45rem 0.65rem 0.45rem 1.85rem', fontSize: '0.85rem', letterSpacing: '0.15em', fontWeight: 700 }}
                    />
                    <FiKey size={13} style={{ position: 'absolute', left: '8px', top: '10px', color: '#64748b' }} />
                  </div>
                  <button
                    type="button"
                    onClick={handleVerifyOtp}
                    className="btn btn-primary btn-sm"
                    style={{ fontSize: '0.75rem', padding: '0.45rem 0.85rem' }}
                  >
                    Verify Now
                  </button>
                </div>
              )}

              {otpMessage && (
                <div style={{ fontSize: '0.725rem', color: '#059669', marginTop: '0.35rem', fontWeight: 500 }}>
                  ✓ {otpMessage}
                </div>
              )}
              {otpError && (
                <div style={{ fontSize: '0.725rem', color: '#dc2626', marginTop: '0.35rem', fontWeight: 500 }}>
                  ⚠ {otpError}
                </div>
              )}
            </div>
          )}

          {/* Password & Confirm Password */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem', color: '#1E293B' }}>
                Password *
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  className="cc-input"
                  style={{
                    width: '100%',
                    padding: '0.55rem 2.2rem 0.55rem 2.25rem',
                    borderRadius: '8px',
                    border: validationErrors.password ? '1px solid #ef4444' : '1px solid #cbd5e1',
                    fontSize: '0.875rem',
                  }}
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Min 6 characters"
                  required
                />
                <FiLock
                  size={14}
                  style={{ position: 'absolute', left: '10px', top: '11px', color: '#64748b' }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '8px',
                    top: '8px',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: '#64748b',
                    padding: '2px',
                    display: 'flex',
                    alignItems: 'center',
                  }}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <FiEyeOff size={15} /> : <FiEye size={15} />}
                </button>
              </div>
              {validationErrors.password && (
                <span style={{ fontSize: '0.75rem', color: '#ef4444', marginTop: '0.2rem', display: 'block' }}>
                  {validationErrors.password}
                </span>
              )}
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.3rem', color: '#1E293B' }}>
                Confirm Password *
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  className="cc-input"
                  style={{
                    width: '100%',
                    padding: '0.55rem 2.2rem 0.55rem 2.25rem',
                    borderRadius: '8px',
                    border: validationErrors.confirmPassword ? '1px solid #ef4444' : '1px solid #cbd5e1',
                    fontSize: '0.875rem',
                  }}
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  placeholder="Repeat password"
                  required
                />
                <FiLock
                  size={14}
                  style={{ position: 'absolute', left: '10px', top: '11px', color: '#64748b' }}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{
                    position: 'absolute',
                    right: '8px',
                    top: '8px',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: '#64748b',
                    padding: '2px',
                    display: 'flex',
                    alignItems: 'center',
                  }}
                  aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                >
                  {showConfirmPassword ? <FiEyeOff size={15} /> : <FiEye size={15} />}
                </button>
              </div>
              {validationErrors.confirmPassword && (
                <span style={{ fontSize: '0.75rem', color: '#ef4444', marginTop: '0.2rem', display: 'block' }}>
                  {validationErrors.confirmPassword}
                </span>
              )}
            </div>
          </div>

          <button
            type="submit"
            className="public-btn-primary"
            disabled={loading}
          >
            {loading ? 'Creating Citizen Account...' : 'Register Citizen Account'}
          </button>
        </form>

        <div style={{ textAlign: 'center', marginTop: '1.25rem', fontSize: '0.825rem', color: '#475569' }}>
          Already have an account?{' '}
          <Link
            to="/login"
            style={{ color: '#10B981', fontWeight: 600, textDecoration: 'none' }}
          >
            Sign in to Command Centre
          </Link>
        </div>
      </div>
    </div>
  );
};

export default SignUpPage;

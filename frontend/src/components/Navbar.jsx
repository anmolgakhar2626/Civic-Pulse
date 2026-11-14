import React, { useState, useEffect } from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  FiShield,
  FiMenu,
  FiX,
  FiLogIn,
  FiUserPlus,
  FiActivity,
  FiGrid,
  FiLogOut
} from 'react-icons/fi';
import { LuCloudSun } from 'react-icons/lu';

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleSignOut = async () => {
    try {
      await logout();
      navigate('/');
    } catch (err) {
      console.warn('Sign out error', err);
    }
  };

  return (
    <header className={`public-navbar ${isScrolled ? 'scrolled' : ''}`}>
      <div className="navbar-container">
        {/* Brand / Logo */}
        <Link to="/" className="navbar-brand">
          <div className="brand-logo-icon">
            <FiShield size={20} />
          </div>
          <div className="brand-text-group">
            <span className="brand-name">Civic<span className="text-emerald">Pulse</span></span>
            <span className="brand-badge-tag">Command Centre</span>
          </div>
        </Link>

        {/* Live Telemetry Status Pill */}
        <div className="navbar-telemetry-pill" title="Real-time Municipal Grid Telemetry Active">
          <span className="telemetry-pulse-dot"></span>
          <span className="telemetry-label">TELEMETRY LIVE</span>
        </div>

        {/* Desktop Navigation Links */}
        <nav className="navbar-nav desktop-nav">
          <NavLink
            to="/"
            end
            className={({ isActive }) => `nav-link-item ${isActive ? 'active' : ''}`}
          >
            Home
          </NavLink>
          <NavLink
            to="/about"
            className={({ isActive }) => `nav-link-item ${isActive ? 'active' : ''}`}
          >
            About Us
          </NavLink>
          <NavLink
            to="/weather"
            className={({ isActive }) => `nav-link-item weather-nav-link ${isActive ? 'active' : ''}`}
          >
            <LuCloudSun size={18} className="weather-nav-icon" />
            <span>Weather</span>
          </NavLink>
        </nav>

        {/* Auth CTA Actions */}
        <div className="navbar-auth-actions desktop-nav">
          {isAuthenticated ? (
            <div className="auth-logged-in-group">
              <Link to="/dashboard" className="btn-navbar-primary">
                <FiGrid size={15} />
                <span>Command Dashboard</span>
              </Link>
              <button
                onClick={handleSignOut}
                className="btn-navbar-outline"
                title={`Signed in as ${user?.username || 'User'}`}
              >
                <FiLogOut size={14} />
                <span>Sign Out</span>
              </button>
            </div>
          ) : (
            <div className="auth-guest-group">
              <Link to="/login" className="btn-navbar-outline">
                <FiLogIn size={14} />
                <span>Sign In</span>
              </Link>
              <Link to="/signup" className="btn-navbar-primary">
                <FiUserPlus size={14} />
                <span>Citizen Portal</span>
              </Link>
            </div>
          )}
        </div>

        {/* Mobile Menu Button */}
        <button
          className="mobile-menu-toggle"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
        >
          {mobileMenuOpen ? <FiX size={24} /> : <FiMenu size={24} />}
        </button>
      </div>

      {/* Mobile Navigation Drawer */}
      {mobileMenuOpen && (
        <div className="mobile-nav-drawer">
          <div className="mobile-telemetry-bar">
            <span className="telemetry-pulse-dot"></span>
            <span>Chandigarh Smart City Grid: Telemetry Live</span>
          </div>

          <NavLink
            to="/"
            end
            className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={() => setMobileMenuOpen(false)}
          >
            Home
          </NavLink>
          <NavLink
            to="/about"
            className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={() => setMobileMenuOpen(false)}
          >
            About Us
          </NavLink>
          <NavLink
            to="/weather"
            className={({ isActive }) => `mobile-nav-link ${isActive ? 'active' : ''}`}
            onClick={() => setMobileMenuOpen(false)}
          >
            <LuCloudSun size={18} />
            <span>Weather</span>
          </NavLink>

          <div className="mobile-auth-divider"></div>

          {isAuthenticated ? (
            <div className="mobile-auth-buttons">
              <Link
                to="/dashboard"
                className="btn-navbar-primary full-width"
                onClick={() => setMobileMenuOpen(false)}
              >
                <FiGrid size={16} />
                <span>Open Command Dashboard</span>
              </Link>
              <button
                onClick={() => {
                  handleSignOut();
                  setMobileMenuOpen(false);
                }}
                className="btn-navbar-outline full-width"
              >
                <FiLogOut size={16} />
                <span>Sign Out ({user?.username || 'User'})</span>
              </button>
            </div>
          ) : (
            <div className="mobile-auth-buttons">
              <Link
                to="/login"
                className="btn-navbar-outline full-width"
                onClick={() => setMobileMenuOpen(false)}
              >
                <FiLogIn size={16} />
                <span>Sign In to Command Centre</span>
              </Link>
              <Link
                to="/signup"
                className="btn-navbar-primary full-width"
                onClick={() => setMobileMenuOpen(false)}
              >
                <FiUserPlus size={16} />
                <span>Register Citizen Account</span>
              </Link>
            </div>
          )}
        </div>
      )}
    </header>
  );
}


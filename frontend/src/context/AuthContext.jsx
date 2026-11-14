import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export const normalizeRole = (role) => {
  if (!role) return 'CITIZEN';
  if (typeof role === 'object' && role !== null) {
    return String(role.name || role.code || 'CITIZEN').replace(/^ROLE_/, '').toUpperCase();
  }
  return String(role).replace(/^ROLE_/, '').toUpperCase();
};

export const normalizeUser = (u) => {
  if (!u) return null;
  const primary = u.role
    ? (typeof u.role === 'object' ? u.role.name : u.role)
    : (Array.isArray(u.roles) && u.roles[0]) || 'CITIZEN';
  const normRole = normalizeRole(primary);
  const normRoles = (Array.isArray(u.roles) && u.roles.length > 0 ? u.roles : [normRole])
    .map(normalizeRole);

  return {
    ...u,
    role: normRole,
    roles: normRoles,
  };
};

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('civicpulse_token'));
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('civicpulse_user');
    try {
      return saved ? normalizeUser(JSON.parse(saved)) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  // Sync token and verify session profile on mount or token changes
  useEffect(() => {
    let isMounted = true;

    const fetchUser = async () => {
      if (token) {
        try {
          const res = await authApi.getMe();
          const userData = normalizeUser(res.data || res);
          if (isMounted) {
            setUser(userData);
            localStorage.setItem('civicpulse_user', JSON.stringify(userData));
          }
        } catch (err) {
          console.warn('Session expired or invalid token:', err.message);
          if (isMounted) {
            logout();
          }
        }
      } else {
        if (isMounted) {
          setUser(null);
        }
      }
      if (isMounted) {
        setLoading(false);
      }
    };

    fetchUser();

    const handleAuthLogout = () => {
      logout();
    };
    window.addEventListener('civicpulse_auth_logout', handleAuthLogout);

    return () => {
      isMounted = false;
      window.removeEventListener('civicpulse_auth_logout', handleAuthLogout);
    };
  }, [token]);

  const login = async (usernameOrEmail, password) => {
    const res = await authApi.login(usernameOrEmail, password);
    const data = res.data || res;
    const accessToken = data.token || data.accessToken;
    if (!accessToken) {
      throw new Error('No access token returned from server');
    }

    // 1. Store JWT token securely for active session persistence
    localStorage.setItem('civicpulse_token', accessToken);
    setToken(accessToken);

    // 2. Fetch current verified user via GET /api/auth/me with the new JWT
    let currentUser = null;
    try {
      const meRes = await authApi.getMe();
      currentUser = normalizeUser(meRes.data || meRes);
    } catch {
      currentUser = normalizeUser(data.user || {
        username: data.username || usernameOrEmail,
        roles: data.roles || ['CITIZEN'],
        role: data.role || 'CITIZEN'
      });
    }

    setUser(currentUser);
    localStorage.setItem('civicpulse_user', JSON.stringify(currentUser));
    return currentUser;
  };

  const loginWithOtp = async (mobileNumber, otp) => {
    const res = await authApi.verifyLoginOtp(mobileNumber, otp);
    const data = res.data || res;
    const accessToken = data.token || data.accessToken;
    if (!accessToken) {
      throw new Error('No access token returned from server');
    }

    // 1. Store JWT token securely for active session persistence
    localStorage.setItem('civicpulse_token', accessToken);
    setToken(accessToken);

    // 2. Fetch current verified user via GET /api/auth/me with the new JWT
    let currentUser = null;
    try {
      const meRes = await authApi.getMe();
      currentUser = normalizeUser(meRes.data || meRes);
    } catch {
      currentUser = normalizeUser(data.user || {
        username: data.username || mobileNumber,
        roles: data.roles || ['CITIZEN'],
        role: data.role || 'CITIZEN'
      });
    }

    setUser(currentUser);
    localStorage.setItem('civicpulse_user', JSON.stringify(currentUser));
    return currentUser;
  };

  const register = async (userData) => {
    const res = await authApi.register(userData);
    return res.data || res;
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors on logout
    } finally {
      localStorage.removeItem('civicpulse_token');
      localStorage.removeItem('civicpulse_user');
      setToken(null);
      setUser(null);
    }
  };

  const hasRole = (requiredRoles = []) => {
    if (!user) return false;
    if (!requiredRoles || requiredRoles.length === 0) return true;

    const targetList = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles];
    const userRoles = [
      ...(Array.isArray(user.roles) ? user.roles : []),
      user.role
    ]
      .filter(Boolean)
      .map(normalizeRole);

    return targetList.some((req) => {
      const cleanReq = normalizeRole(req);
      return userRoles.includes(cleanReq);
    });
  };

  const getPrimaryRole = () => {
    if (!user) return 'CITIZEN';
    return normalizeRole(user.role || (Array.isArray(user.roles) && user.roles[0]) || 'CITIZEN');
  };

  const refreshUser = async () => {
    if (token) {
      try {
        const res = await authApi.getMe();
        const userData = normalizeUser(res.data || res);
        setUser(userData);
        localStorage.setItem('civicpulse_user', JSON.stringify(userData));
        return userData;
      } catch (err) {
        console.warn('Failed to refresh user profile:', err.message);
      }
    }
    return null;
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        isAuthenticated: !!token,
        loading,
        login,
        loginWithOtp,
        register,
        logout,
        hasRole,
        getPrimaryRole,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

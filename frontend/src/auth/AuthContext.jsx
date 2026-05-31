import { createContext, useContext, useState, useCallback } from 'react';
import api from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const token = localStorage.getItem('ucl_token');
    const username = localStorage.getItem('ucl_username');
    const role = localStorage.getItem('ucl_role');
    return token ? { token, username, role } : null;
  });

  const persist = (data) => {
    localStorage.setItem('ucl_token', data.token);
    localStorage.setItem('ucl_username', data.username);
    localStorage.setItem('ucl_role', data.role);
    setAuth({ token: data.token, username: data.username, role: data.role });
  };

  const login = useCallback(async (username, password) => {
    const { data } = await api.post('/auth/login', { username, password });
    persist(data);
    return data;
  }, []);

  const register = useCallback(async (username, password) => {
    const { data } = await api.post('/auth/register', { username, password });
    persist(data);
    return data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('ucl_token');
    localStorage.removeItem('ucl_username');
    localStorage.removeItem('ucl_role');
    setAuth(null);
  }, []);

  const value = {
    auth,
    isAuthenticated: !!auth,
    isAdmin: auth?.role === 'ADMIN',
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

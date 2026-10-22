import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import { api, onAuthFailure, tokenStore } from './api';
import type { User } from './types';

interface AuthState {
  user: User | null;
  ready: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (payload: { email: string; password: string; fullName: string }) => Promise<User>;
  logout: (reason?: string) => void;
}

const AuthContext = createContext<AuthState | null>(null);

// сообщение на странице входа после принудительного выхода
export const LOGOUT_REASON_KEY = 'finance.logoutReason';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);

  const logout = useCallback((reason?: string) => {
    tokenStore.clear();
    if (reason) sessionStorage.setItem(LOGOUT_REASON_KEY, reason);
    setUser(null);
  }, []);

  useEffect(() => {
    onAuthFailure((code, message) =>
      logout(code === 'USER_BLOCKED' ? message : 'Сессия истекла. Войдите заново'),
    );
    if (!tokenStore.get()) {
      setReady(true);
      return;
    }
    api
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setReady(true));
  }, [logout]);

  const login = async (email: string, password: string) => {
    const res = await api.login(email, password);
    tokenStore.set(res.token);
    setUser(res.user);
    return res.user;
  };

  const register = async (payload: { email: string; password: string; fullName: string }) => {
    const res = await api.register(payload);
    tokenStore.set(res.token);
    setUser(res.user);
    return res.user;
  };

  return <AuthContext.Provider value={{ user, ready, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth вне AuthProvider');
  return ctx;
}

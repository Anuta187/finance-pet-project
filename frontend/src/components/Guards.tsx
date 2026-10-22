import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth';
import type { Role } from '../types';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, ready } = useAuth();
  const location = useLocation();
  if (!ready) {
    return (
      <div className="splash" data-testid="app-loading">
        <span className="spinner" /> Загрузка…
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { user } = useAuth();
  if (user?.role !== role) {
    return (
      <div className="page">
        <div className="panel empty" data-testid="forbidden">
          <div className="empty__icon">🔒</div>
          <h2>403 — Недостаточно прав</h2>
          <p className="muted">Этот раздел доступен только администраторам банка.</p>
        </div>
      </div>
    );
  }
  return <>{children}</>;
}

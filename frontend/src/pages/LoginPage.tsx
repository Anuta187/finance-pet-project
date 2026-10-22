import { FormEvent, useEffect, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ApiError } from '../api';
import { LOGOUT_REASON_KEY, useAuth } from '../auth';
import { AuthLayout } from './AuthLayout';

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<{ code: string; message: string } | null>(null);
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const reason = sessionStorage.getItem(LOGOUT_REASON_KEY);
    if (reason) {
      setNotice(reason);
      sessionStorage.removeItem(LOGOUT_REASON_KEY);
    }
  }, []);

  if (user) return <Navigate to="/" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email.trim() || !password) {
      setError({ code: 'VALIDATION_ERROR', message: 'Введите email и пароль' });
      return;
    }
    setLoading(true);
    try {
      const u = await login(email, password);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(u.role === 'ADMIN' ? '/admin' : from && from !== '/admin' ? from : '/', { replace: true });
    } catch (err) {
      const apiErr = err as ApiError;
      setError({ code: apiErr.code, message: apiErr.message });
    } finally {
      setLoading(false);
    }
  }

  const fill = (e: string, p: string) => {
    setEmail(e);
    setPassword(p);
    setError(null);
  };

  return (
    <AuthLayout>
      <h1>Вход в интернет-банк</h1>
      <p className="muted">Счета, переводы, платежи и карты — в одном месте</p>
      {notice && (
        <p className="alert alert-warning" data-testid="login-notice">
          {notice}
        </p>
      )}
      <form className="form" onSubmit={submit} noValidate data-testid="login-form">
        <label>
          Email
          <input
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            data-testid="login-email"
          />
        </label>
        <label>
          Пароль
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            data-testid="login-password"
          />
        </label>
        {error && (
          <p className="alert alert-error" data-testid="login-error" data-error-code={error.code}>
            {error.message}
          </p>
        )}
        <button type="submit" className="btn btn--primary btn--wide" disabled={loading} data-testid="login-submit">
          {loading ? 'Входим…' : 'Войти'}
        </button>
      </form>
      <p className="auth__switch">
        Нет аккаунта?{' '}
        <Link to="/register" data-testid="to-register">
          Зарегистрироваться
        </Link>
      </p>
      <div className="demo-box">
        <div className="demo-box__title">Демо-доступ</div>
        <button type="button" className="demo-box__item" onClick={() => fill('demo@finance.local', 'demo12345')} data-testid="demo-client">
          <b>Клиент</b> demo@finance.local / demo12345
        </button>
        <button type="button" className="demo-box__item" onClick={() => fill('admin@finance.local', 'admin12345')} data-testid="demo-admin">
          <b>Администратор</b> admin@finance.local / admin12345
        </button>
      </div>
    </AuthLayout>
  );
}

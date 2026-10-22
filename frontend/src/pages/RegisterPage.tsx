import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { ApiError } from '../api';
import { useAuth } from '../auth';
import { useToast } from '../toast';
import { AuthLayout } from './AuthLayout';

export function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [error, setError] = useState<{ code: string; message: string } | null>(null);
  const [loading, setLoading] = useState(false);

  if (user) return <Navigate to="/" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password !== password2) {
      setError({ code: 'VALIDATION_ERROR', message: 'Пароли не совпадают' });
      return;
    }
    setLoading(true);
    try {
      const u = await register({ email, password, fullName });
      toast('success', `Добро пожаловать, ${u.fullName}!`);
      navigate('/', { replace: true });
    } catch (err) {
      const apiErr = err as ApiError;
      setError({ code: apiErr.code, message: apiErr.message });
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <h1>Регистрация</h1>
      <p className="muted">Создайте аккаунт клиента — это бесплатно</p>
      <form className="form" onSubmit={submit} noValidate data-testid="register-form">
        <label>
          Имя и фамилия
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Анна Иванова" data-testid="register-name" />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" data-testid="register-email" />
        </label>
        <div className="form__row">
          <label>
            Пароль
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} data-testid="register-password" />
          </label>
          <label>
            Повторите пароль
            <input type="password" value={password2} onChange={(e) => setPassword2(e.target.value)} data-testid="register-password2" />
          </label>
        </div>
        <p className="hint">Не меньше 8 символов, хотя бы одна буква и одна цифра</p>
        {error && (
          <p className="alert alert-error" data-testid="register-error" data-error-code={error.code}>
            {error.message}
          </p>
        )}
        <button type="submit" className="btn btn--primary btn--wide" disabled={loading} data-testid="register-submit">
          Создать аккаунт
        </button>
      </form>
      <p className="auth__switch">
        Уже есть аккаунт? <Link to="/login">Войти</Link>
      </p>
    </AuthLayout>
  );
}

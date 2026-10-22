import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';

const CLIENT_NAV = [
  { to: '/', label: 'Главная', icon: '◧', id: 'dashboard', end: true },
  { to: '/transfers', label: 'Переводы', icon: '⇄', id: 'transfers' },
  { to: '/payments', label: 'Платежи', icon: '₽', id: 'payments' },
  { to: '/cards', label: 'Карты', icon: '▭', id: 'cards' },
  { to: '/autopayments', label: 'Автоплатежи', icon: '↻', id: 'autopayments' },
  { to: '/history', label: 'История', icon: '☰', id: 'history' },
];

const ADMIN_NAV = [{ to: '/admin', label: 'Администрирование', icon: '⚙', id: 'admin', end: false }];

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const nav = user?.role === 'ADMIN' ? ADMIN_NAV : CLIENT_NAV;
  const initials = (user?.fullName ?? '?')
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand__logo" aria-hidden="true">
            ₽
          </span>
          <div>
            <div className="brand__name">Личные финансы</div>
            <div className="brand__tag">интернет-банк</div>
          </div>
        </div>
        <nav className="nav" data-testid="nav">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav__link${isActive ? ' nav__link--active' : ''}`}
              data-testid={`nav-${item.id}`}
            >
              <span className="nav__icon" aria-hidden="true">
                {item.icon}
              </span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__foot">
          <a className="nav__link nav__link--muted" href="/api/docs" target="_blank" rel="noreferrer">
            <span className="nav__icon">{'{}'}</span>API (Swagger)
          </a>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="topbar__title" />
          <div className="topbar__user">
            {user?.role === 'ADMIN' && <span className="badge badge--admin">Администратор</span>}
            <span className="avatar" aria-hidden="true">
              {initials}
            </span>
            <div className="topbar__who">
              <div data-testid="user-name">{user?.fullName}</div>
              <div className="muted small" data-testid="user-email">
                {user?.email}
              </div>
            </div>
            <button
              className="btn btn--ghost"
              data-testid="logout"
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              Выйти
            </button>
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  );
}

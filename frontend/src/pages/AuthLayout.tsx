import { ReactNode } from 'react';

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth">
      <div className="auth__promo">
        <div className="brand brand--light">
          <span className="brand__logo">₽</span>
          <div>
            <div className="brand__name">Личные финансы</div>
            <div className="brand__tag">интернет-банк</div>
          </div>
        </div>
        <div className="auth__hero">
          <h2>Все деньги — под контролем</h2>
          <ul>
            <li>Счета и карты в одном окне</li>
            <li>Переводы с дневным лимитом и защитой от ошибок</li>
            <li>Оплата услуг и автоплатежи по расписанию</li>
            <li>Аналитика расходов по категориям</li>
          </ul>
        </div>
        <div className="auth__foot">Учебный проект · Spring Boot 3 · React · JWT</div>
      </div>
      <div className="auth__panel">
        <div className="auth__box">{children}</div>
      </div>
    </div>
  );
}

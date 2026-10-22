import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { AccountCards, totalBalance } from '../components/AccountCards';
import { CreateAccountForm } from '../components/CreateAccountForm';
import { SpendingChart } from '../components/SpendingChart';
import { firstName, formatMoney } from '../format';
import type { Account, Operation, Spending } from '../types';
import { useLoad } from '../useLoad';

export function DashboardPage() {
  const { user } = useAuth();
  const [showCreate, setShowCreate] = useState(false);
  const accounts = useLoad<Account[]>(api.getAccounts, []);
  const recent = useLoad<Operation[]>(() => api.getOperations(undefined, 6), []);
  const spending = useLoad<Spending | null>(api.getSpending, null);

  const reloadAll = () => {
    accounts.reload();
    recent.reload();
  };

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1 data-testid="greeting">Здравствуйте, {firstName(user?.fullName ?? '')}!</h1>
          <p className="muted">Ваши финансы на сегодня</p>
        </div>
        <div className="quick">
          <Link className="btn btn--primary" to="/transfers" data-testid="quick-transfer">
            ⇄ Перевести
          </Link>
          <Link className="btn btn--secondary" to="/payments" data-testid="quick-pay">
            ₽ Оплатить
          </Link>
        </div>
      </div>

      {(accounts.error || recent.error) && (
        <p className="alert alert-error" data-testid="load-error">
          {accounts.error || recent.error}
        </p>
      )}

      <section className="hero-card">
        <div>
          <div className="hero-card__label">Всего на счетах</div>
          <div className="hero-card__total money" data-testid="accounts-total">
            {formatMoney(totalBalance(accounts.data))}
          </div>
          <div className="hero-card__sub">
            {accounts.data.length} {plural(accounts.data.length, 'счёт', 'счёта', 'счетов')}
          </div>
        </div>
        <button className="btn btn--light" onClick={() => setShowCreate((v) => !v)} data-testid="toggle-create-account">
          {showCreate ? 'Скрыть' : '+ Открыть счёт'}
        </button>
      </section>

      {(showCreate || (accounts.loaded && accounts.data.length === 0)) && (
        <section className="panel">
          <h2>Новый счёт</h2>
          <CreateAccountForm
            onCreated={() => {
              setShowCreate(false);
              reloadAll();
            }}
          />
        </section>
      )}

      <section className="panel">
        <div className="panel__head">
          <h2>Мои счета</h2>
        </div>
        <AccountCards accounts={accounts.data} loaded={accounts.loaded} />
      </section>

      <div className="grid-2">
        <section className="panel">
          <div className="panel__head">
            <h2>Расходы по категориям</h2>
            <Link to="/payments" className="link">
              Оплатить услугу →
            </Link>
          </div>
          <SpendingChart spending={spending.data} />
        </section>
        <section className="panel">
          <div className="panel__head">
            <h2>Последние операции</h2>
            <Link to="/history" className="link" data-testid="to-history">
              Вся история →
            </Link>
          </div>
          <RecentList operations={recent.data} />
        </section>
      </div>
    </div>
  );
}

function RecentList({ operations }: { operations: Operation[] }) {
  if (!operations.length) return <p className="muted">Операций пока нет</p>;
  return (
    <ul className="recent" data-testid="recent-operations">
      {operations.map((op) => {
        const out = op.type === 'PAYMENT';
        const title =
          op.type === 'PAYMENT' ? op.categoryName : op.type === 'OPENING' ? 'Пополнение счёта' : 'Перевод между счетами';
        const sub =
          op.type === 'TRANSFER' ? `${op.fromAccountName} → ${op.toAccountName}` : op.comment || op.fromAccountName || op.toAccountName;
        return (
          <li key={op.id} className="recent__item" data-testid="recent-item">
            <span className={`recent__icon recent__icon--${op.type.toLowerCase()}`}>
              {op.type === 'PAYMENT' ? '₽' : op.type === 'OPENING' ? '+' : '⇄'}
            </span>
            <div className="recent__body">
              <div>{title}</div>
              <div className="muted small">{sub}</div>
            </div>
            <div className={`money ${out ? 'money--out' : op.type === 'OPENING' ? 'money--in' : ''}`}>
              {out ? '−' : op.type === 'OPENING' ? '+' : ''}
              {formatMoney(op.amount)}
            </div>
          </li>
        );
      })}
    </ul>
  );
}

function plural(n: number, one: string, few: string, many: string) {
  const m10 = n % 10;
  const m100 = n % 100;
  if (m10 === 1 && m100 !== 11) return one;
  if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few;
  return many;
}

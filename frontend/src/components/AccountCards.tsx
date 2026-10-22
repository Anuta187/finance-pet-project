import type { Account } from '../types';
import { formatMoney } from '../format';
import { EmptyState } from './EmptyState';

const GRADIENTS = ['acc--blue', 'acc--teal', 'acc--violet', 'acc--orange', 'acc--pink'];

export function AccountCards({ accounts, loaded }: { accounts: Account[]; loaded: boolean }) {
  if (!accounts.length) {
    return loaded ? (
      <EmptyState icon="💼" title="Счетов пока нет" text="Откройте первый счёт — это займёт пару секунд" testId="accounts-empty" />
    ) : (
      <div className="skeleton-row" data-testid="accounts-loading">
        <div className="skeleton" />
        <div className="skeleton" />
        <div className="skeleton" />
      </div>
    );
  }
  return (
    <div className="accounts" data-testid="accounts-list">
      {accounts.map((a, i) => (
        <div
          key={a.id}
          className={`acc ${GRADIENTS[i % GRADIENTS.length]}`}
          data-testid="account-row"
          data-account-id={a.id}
        >
          <div className="acc__top">
            <span className="acc__name" data-testid="account-name">
              {a.name}
            </span>
            <span className="acc__num">№ {String(a.id).padStart(4, '0')}</span>
          </div>
          <div className="acc__balance money" data-testid="account-balance">
            {formatMoney(a.balance)}
          </div>
          <div className="acc__cur">Рублёвый счёт · {a.currency}</div>
        </div>
      ))}
    </div>
  );
}

export function totalBalance(accounts: Account[]): number {
  return accounts.reduce((sum, a) => sum + Math.round(a.balance * 100), 0) / 100;
}

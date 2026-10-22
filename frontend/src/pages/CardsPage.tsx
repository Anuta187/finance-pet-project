import { useState } from 'react';
import { api, ApiError } from '../api';
import { BankCard } from '../components/BankCard';
import { EmptyState } from '../components/EmptyState';
import { useToast } from '../toast';
import type { Account, Card } from '../types';
import { useLoad } from '../useLoad';

export function CardsPage() {
  const cards = useLoad<Card[]>(api.getCards, []);
  const accounts = useLoad<Account[]>(api.getAccounts, []);
  const [accountId, setAccountId] = useState('');
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  async function issue() {
    if (!accountId) {
      toast('error', 'Выберите счёт для новой карты');
      return;
    }
    setBusy(true);
    try {
      const card = await api.issueCard(Number(accountId));
      toast('success', `Карта •••• ${card.last4} выпущена`);
      cards.reload();
    } catch (e) {
      toast('error', (e as ApiError).message);
    } finally {
      setBusy(false);
    }
  }

  async function toggle(card: Card) {
    try {
      const updated = card.status === 'ACTIVE' ? await api.blockCard(card.id) : await api.unblockCard(card.id);
      toast('success', `Карта •••• ${updated.last4} ${updated.status === 'BLOCKED' ? 'заблокирована' : 'разблокирована'}`);
      cards.reload();
    } catch (e) {
      toast('error', (e as ApiError).message);
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1>Карты</h1>
          <p className="muted">Виртуальные карты «Мир», привязанные к вашим счетам</p>
        </div>
      </div>
      <section className="panel">
        <h2>Выпустить карту</h2>
        <div className="form form--inline">
          <label>
            Счёт
            <select value={accountId} onChange={(e) => setAccountId(e.target.value)} data-testid="issue-card-account">
              <option value="" disabled>
                Выберите счёт
              </option>
              {accounts.data.map((a) => (
                <option key={a.id} value={String(a.id)}>
                  {a.name}
                </option>
              ))}
            </select>
          </label>
          <button className="btn btn--primary" onClick={issue} disabled={busy} data-testid="issue-card-submit">
            Выпустить карту
          </button>
        </div>
      </section>
      <section className="panel">
        <h2>Мои карты</h2>
        {cards.loaded && cards.data.length === 0 ? (
          <EmptyState icon="💳" title="Карт пока нет" text="Выпустите виртуальную карту к любому счёту" testId="cards-empty" />
        ) : (
          <div className="cards-grid" data-testid="cards-list">
            {cards.data.map((card) => (
              <div key={card.id} className="card-item" data-testid="card-item" data-card-id={card.id}>
                <BankCard card={card} />
                <div className="card-item__meta">
                  <div>
                    <div className="muted small">Счёт</div>
                    <div>{card.accountName}</div>
                  </div>
                  <span className={`status status--${card.status.toLowerCase()}`} data-testid="card-status">
                    {card.status === 'ACTIVE' ? 'Активна' : 'Заблокирована'}
                  </span>
                </div>
                <button
                  className={`btn ${card.status === 'ACTIVE' ? 'btn--danger-ghost' : 'btn--secondary'} btn--wide`}
                  onClick={() => toggle(card)}
                  data-testid="card-toggle"
                >
                  {card.status === 'ACTIVE' ? 'Заблокировать' : 'Разблокировать'}
                </button>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

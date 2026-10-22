import { FormEvent, useState } from 'react';
import { api, ApiError } from '../api';
import { SourceSelect, parseSource } from '../components/SourceSelect';
import { formatMoney } from '../format';
import { useToast } from '../toast';
import type { Account, Card, Category, UiError } from '../types';
import { useLoad } from '../useLoad';

const ICONS: Record<string, string> = {
  MOBILE: '📱',
  INTERNET: '🌐',
  UTILITIES: '🏠',
  TRANSPORT: '🚌',
  EDUCATION: '🎓',
  ENTERTAINMENT: '🎬',
  HEALTH: '💊',
  OTHER: '🧾',
};

export function PaymentsPage() {
  const categories = useLoad<Category[]>(api.getCategories, []);
  const accounts = useLoad<Account[]>(api.getAccounts, []);
  const cards = useLoad<Card[]>(api.getCards, []);
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [source, setSource] = useState('');
  const [amount, setAmount] = useState('');
  const [comment, setComment] = useState('');
  const [error, setError] = useState<UiError | null>(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  const category = categories.data.find((c) => c.id === categoryId);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!categoryId) {
      setError({ code: 'VALIDATION_ERROR', message: 'Выберите категорию услуги' });
      return;
    }
    if (!source) {
      setError({ code: 'VALIDATION_ERROR', message: 'Выберите счёт или карту для оплаты' });
      return;
    }
    setLoading(true);
    try {
      const res = await api.pay({ categoryId, ...parseSource(source), amount, comment: comment || undefined });
      toast('success', `Оплачено ${formatMoney(res.operation.amount)}: ${res.operation.categoryName}`);
      setAmount('');
      setComment('');
      accounts.reload();
    } catch (err) {
      const apiErr = err as ApiError;
      setError({ code: apiErr.code, message: apiErr.message });
      cards.reload();
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1>Оплата услуг</h1>
          <p className="muted">Выберите категорию, источник и сумму</p>
        </div>
      </div>
      <section className="panel">
        <h2>Категория</h2>
        <div className="categories" data-testid="categories">
          {categories.data.map((c) => (
            <button
              key={c.id}
              type="button"
              className={`category${c.id === categoryId ? ' category--active' : ''}`}
              style={{ ['--cat' as string]: c.color }}
              onClick={() => setCategoryId(c.id)}
              data-testid={`category-${c.code}`}
            >
              <span className="category__icon">{ICONS[c.code] ?? '🧾'}</span>
              <span>{c.name}</span>
            </button>
          ))}
        </div>
      </section>
      <section className="panel">
        <h2>{category ? `Оплата: ${category.name}` : 'Детали платежа'}</h2>
        <form className="form" onSubmit={submit} noValidate data-testid="payment-form">
          <div className="form__row">
            <label>
              Списать с
              <SourceSelect accounts={accounts.data} cards={cards.data} value={source} onChange={setSource} testId="payment-source" />
            </label>
            <label>
              Сумма, ₽
              <input value={amount} onChange={(e) => setAmount(e.target.value)} inputMode="decimal" placeholder="0,00" data-testid="payment-amount" />
            </label>
          </div>
          <label>
            Назначение / лицевой счёт
            <input value={comment} onChange={(e) => setComment(e.target.value)} maxLength={200} placeholder="Например, номер телефона или договора" data-testid="payment-comment" />
          </label>
          {error && (
            <p className="alert alert-error" data-testid="payment-error" data-error-code={error.code}>
              {error.message}
            </p>
          )}
          <button type="submit" className="btn btn--primary btn--wide" disabled={loading} data-testid="payment-submit">
            Оплатить
          </button>
        </form>
      </section>
    </div>
  );
}

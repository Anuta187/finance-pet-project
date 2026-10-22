import { FormEvent, useState } from 'react';
import { api, ApiError } from '../api';
import { EmptyState } from '../components/EmptyState';
import { SourceSelect, parseSource } from '../components/SourceSelect';
import { formatDay, formatMoney, PERIOD_LABEL, todayIso } from '../format';
import { useToast } from '../toast';
import type { Account, Autopayment, Card, Category, Period, UiError } from '../types';
import { useLoad } from '../useLoad';

export function AutopaymentsPage() {
  const list = useLoad<Autopayment[]>(api.getAutopayments, []);
  const categories = useLoad<Category[]>(api.getCategories, []);
  const accounts = useLoad<Account[]>(api.getAccounts, []);
  const cards = useLoad<Card[]>(api.getCards, []);
  const [name, setName] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [source, setSource] = useState('');
  const [amount, setAmount] = useState('');
  const [period, setPeriod] = useState<Period>('MONTHLY');
  const [startDate, setStartDate] = useState(todayIso());
  const [error, setError] = useState<UiError | null>(null);
  const toast = useToast();

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!categoryId || !source) {
      setError({ code: 'VALIDATION_ERROR', message: 'Выберите категорию и источник списания' });
      return;
    }
    try {
      const ap = await api.createAutopayment({
        name,
        categoryId: Number(categoryId),
        ...parseSource(source),
        amount,
        period,
        startDate: startDate || undefined,
      });
      toast('success', `Автоплатёж «${ap.name}» создан, первое списание ${formatDay(ap.nextRunDate)}`);
      setName('');
      setAmount('');
      list.reload();
    } catch (err) {
      const apiErr = err as ApiError;
      setError({ code: apiErr.code, message: apiErr.message });
    }
  }

  async function disable(ap: Autopayment) {
    try {
      await api.disableAutopayment(ap.id);
      toast('success', `Автоплатёж «${ap.name}» отключён`);
      list.reload();
    } catch (e) {
      toast('error', (e as ApiError).message);
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1>Автоплатежи</h1>
          <p className="muted">Регулярные платежи списываются автоматически в назначенный день</p>
        </div>
      </div>
      <section className="panel">
        <h2>Новый автоплатёж</h2>
        <form className="form" onSubmit={submit} noValidate data-testid="autopayment-form">
          <div className="form__row form__row--3">
            <label>
              Название
              <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} placeholder="Например, Интернет дома" data-testid="autopayment-name" />
            </label>
            <label>
              Категория
              <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} data-testid="autopayment-category">
                <option value="" disabled>
                  Выберите категорию
                </option>
                {categories.data.map((c) => (
                  <option key={c.id} value={String(c.id)}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Списывать с
              <SourceSelect accounts={accounts.data} cards={cards.data} value={source} onChange={setSource} testId="autopayment-source" />
            </label>
          </div>
          <div className="form__row form__row--3">
            <label>
              Сумма, ₽
              <input value={amount} onChange={(e) => setAmount(e.target.value)} inputMode="decimal" placeholder="0,00" data-testid="autopayment-amount" />
            </label>
            <label>
              Периодичность
              <select value={period} onChange={(e) => setPeriod(e.target.value as Period)} data-testid="autopayment-period">
                <option value="MONTHLY">Ежемесячно</option>
                <option value="WEEKLY">Еженедельно</option>
                <option value="DAILY">Ежедневно</option>
              </select>
            </label>
            <label>
              Первое списание
              <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} data-testid="autopayment-start" />
            </label>
          </div>
          {error && (
            <p className="alert alert-error" data-testid="autopayment-error" data-error-code={error.code}>
              {error.message}
            </p>
          )}
          <button type="submit" className="btn btn--primary" data-testid="autopayment-submit">
            Создать автоплатёж
          </button>
        </form>
      </section>
      <section className="panel">
        <h2>Мои автоплатежи</h2>
        {list.loaded && list.data.length === 0 ? (
          <EmptyState icon="↻" title="Автоплатежей нет" text="Настройте регулярную оплату связи, интернета или ЖКХ" testId="autopayments-empty" />
        ) : (
          <div className="table-wrap">
            <table className="table" data-testid="autopayments-table">
              <thead>
                <tr>
                  <th>Название</th>
                  <th>Категория</th>
                  <th>Источник</th>
                  <th className="num">Сумма</th>
                  <th>Период</th>
                  <th>Следующее списание</th>
                  <th>Статус</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {list.data.map((ap) => (
                  <tr key={ap.id} data-testid="autopayment-row" data-autopayment-id={ap.id}>
                    <td data-testid="autopayment-row-name">{ap.name}</td>
                    <td>{ap.categoryName}</td>
                    <td>{ap.cardMasked ? `Карта •••• ${ap.cardMasked.slice(-4)}` : ap.accountName}</td>
                    <td className="num money">{formatMoney(ap.amount)}</td>
                    <td>{PERIOD_LABEL[ap.period]}</td>
                    <td>{ap.active ? formatDay(ap.nextRunDate) : '—'}</td>
                    <td>
                      <span className={`status status--${ap.active ? 'active' : 'off'}`} data-testid="autopayment-status">
                        {ap.active ? 'Активен' : 'Отключён'}
                      </span>
                      {ap.lastStatus && (
                        <div className={`small ${ap.lastStatus === 'FAILED' ? 'text-danger' : 'muted'}`} title={ap.lastError ?? ''}>
                          {ap.lastStatus === 'SUCCESS' ? 'последний: успешно' : 'последний: ошибка'}
                        </div>
                      )}
                    </td>
                    <td>
                      {ap.active && (
                        <button className="btn btn--danger-ghost btn--sm" onClick={() => disable(ap)} data-testid="autopayment-disable">
                          Отключить
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

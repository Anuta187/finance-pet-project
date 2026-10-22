import { FormEvent, useState } from 'react';
import { api, ApiError } from '../api';
import { formatMoney } from '../format';
import { useToast } from '../toast';
import type { Account, TransferResult, UiError } from '../types';

export function TransferForm({ accounts, onDone }: { accounts: Account[]; onDone: (result: TransferResult) => void }) {
  const [fromAccountId, setFromAccountId] = useState('');
  const [toAccountId, setToAccountId] = useState('');
  const [amount, setAmount] = useState('');
  const [comment, setComment] = useState('');
  const [error, setError] = useState<UiError | null>(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!fromAccountId || !toAccountId) {
      setError({ code: 'VALIDATION_ERROR', message: 'Выберите счёт списания и счёт зачисления' });
      return;
    }
    setLoading(true);
    try {
      const result = await api.transfer({
        fromAccountId: Number(fromAccountId),
        toAccountId: Number(toAccountId),
        amount,
        comment: comment || undefined,
      });
      toast(
        'success',
        `Переведено ${formatMoney(result.operation.amount)}: «${result.fromAccount.name}» → «${result.toAccount.name}»`,
      );
      setAmount('');
      setComment('');
      onDone(result);
    } catch (err) {
      const apiErr = err as ApiError;
      setError({ code: apiErr.code ?? 'NETWORK_ERROR', message: apiErr.message });
    } finally {
      setLoading(false);
    }
  }

  const options = accounts.map((a) => (
    <option key={a.id} value={String(a.id)}>
      {a.name} ({formatMoney(a.balance)})
    </option>
  ));

  return (
    <form className="form" data-testid="transfer-form" onSubmit={submit} noValidate>
      <div className="form__row">
        <label>
          Откуда
          <select value={fromAccountId} onChange={(e) => setFromAccountId(e.target.value)} data-testid="transfer-from">
            <option value="" disabled>
              Выберите счёт
            </option>
            {options}
          </select>
        </label>
        <label>
          Куда
          <select value={toAccountId} onChange={(e) => setToAccountId(e.target.value)} data-testid="transfer-to">
            <option value="" disabled>
              Выберите счёт
            </option>
            {options}
          </select>
        </label>
      </div>
      <div className="form__row">
        <label>
          Сумма, ₽
          <input
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            inputMode="decimal"
            placeholder="0,00"
            data-testid="transfer-amount"
          />
        </label>
        <label>
          Комментарий
          <input
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={200}
            placeholder="Необязательно"
            data-testid="transfer-comment"
          />
        </label>
      </div>
      <button type="submit" className="btn btn--primary btn--wide" disabled={loading} data-testid="transfer-submit">
        Перевести
      </button>
      <p className="hint">Дневной лимит переводов с одного счёта — 100 000 ₽</p>
      {error && (
        <p className="alert alert-error" data-testid="transfer-error" data-error-code={error.code}>
          {error.message}
        </p>
      )}
    </form>
  );
}

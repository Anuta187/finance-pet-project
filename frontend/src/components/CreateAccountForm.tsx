import { FormEvent, useState } from 'react';
import { api } from '../api';
import { useToast } from '../toast';
import type { Account } from '../types';

export function CreateAccountForm({ onCreated }: { onCreated: (account: Account) => void }) {
  const [name, setName] = useState('');
  const [initialBalance, setInitialBalance] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    if (!name.trim()) {
      setError('Введите название счёта');
      return;
    }
    setLoading(true);
    try {
      const account = await api.createAccount({
        name,
        initialBalance: initialBalance === '' ? undefined : initialBalance,
      });
      toast('success', `Счёт «${account.name}» открыт`);
      setName('');
      setInitialBalance('');
      onCreated(account);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="form form--inline" data-testid="create-account-form" onSubmit={submit} noValidate>
      <label>
        Название счёта
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={50}
          placeholder="Например, Отпуск"
          data-testid="account-name-input"
        />
      </label>
      <label>
        Начальный остаток, ₽
        <input
          value={initialBalance}
          onChange={(e) => setInitialBalance(e.target.value)}
          inputMode="decimal"
          placeholder="0"
          data-testid="account-initial-input"
        />
      </label>
      <button type="submit" className="btn btn--primary" disabled={loading} data-testid="create-account-submit">
        Открыть счёт
      </button>
      {error && (
        <p className="alert alert-error form__full" data-testid="create-account-error">
          {error}
        </p>
      )}
    </form>
  );
}

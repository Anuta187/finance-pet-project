import { useEffect, useState } from 'react';
import { api } from '../api';
import { OperationsTable } from '../components/OperationsTable';
import type { Account, Operation } from '../types';
import { useLoad } from '../useLoad';

export function HistoryPage() {
  const accounts = useLoad<Account[]>(api.getAccounts, []);
  const [filter, setFilter] = useState('');
  const [operations, setOperations] = useState<Operation[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoaded(false);
    api
      .getOperations(filter)
      .then((ops) => {
        setOperations(ops);
        setError('');
      })
      .catch((e) => setError((e as Error).message))
      .finally(() => setLoaded(true));
  }, [filter]);

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1>История операций</h1>
          <p className="muted">Пополнения, переводы и платежи — новые сверху</p>
        </div>
        <label className="filter">
          Счёт:
          <select value={filter} onChange={(e) => setFilter(e.target.value)} data-testid="history-filter">
            <option value="">Все счета</option>
            {accounts.data.map((a) => (
              <option key={a.id} value={String(a.id)}>
                {a.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      {error && <p className="alert alert-error">{error}</p>}
      <section className="panel" data-loaded={loaded}>
        <OperationsTable operations={operations} />
      </section>
    </div>
  );
}

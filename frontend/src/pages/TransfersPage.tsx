import { api } from '../api';
import { AccountCards } from '../components/AccountCards';
import { TransferForm } from '../components/TransferForm';
import { CreateAccountForm } from '../components/CreateAccountForm';
import type { Account } from '../types';
import { useLoad } from '../useLoad';

export function TransfersPage() {
  const accounts = useLoad<Account[]>(api.getAccounts, []);
  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1>Переводы</h1>
          <p className="muted">Между своими счетами — мгновенно и без комиссии</p>
        </div>
      </div>
      {accounts.error && <p className="alert alert-error">{accounts.error}</p>}
      <div className="grid-2 grid-2--wide-left">
        <section className="panel">
          <h2>Перевод между счетами</h2>
          <TransferForm accounts={accounts.data} onDone={accounts.reload} />
        </section>
        <section className="panel">
          <h2>Новый счёт</h2>
          <CreateAccountForm onCreated={accounts.reload} />
        </section>
      </div>
      <section className="panel">
        <h2>Мои счета</h2>
        <AccountCards accounts={accounts.data} loaded={accounts.loaded} />
      </section>
    </div>
  );
}

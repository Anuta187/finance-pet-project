import { useState } from 'react';
import { api, ApiError } from '../api';
import { OperationsTable } from '../components/OperationsTable';
import { formatDate, formatMoney } from '../format';
import { useToast } from '../toast';
import type { AdminUser, Operation } from '../types';
import { useLoad } from '../useLoad';

export function AdminPage() {
  const users = useLoad<AdminUser[]>(api.adminUsers, []);
  const operations = useLoad<Operation[]>(api.adminOperations, []);
  const [tab, setTab] = useState<'users' | 'operations'>('users');
  const toast = useToast();

  const clients = users.data.filter((u) => u.role === 'CLIENT');
  const blocked = users.data.filter((u) => u.blocked).length;
  const total = clients.reduce((s, u) => s + Math.round(u.totalBalance * 100), 0) / 100;

  async function toggle(u: AdminUser) {
    try {
      const res = u.blocked ? await api.adminUnblock(u.id) : await api.adminBlock(u.id);
      toast('success', `${res.email} ${res.blocked ? 'заблокирован' : 'разблокирован'}`);
      users.reload();
    } catch (e) {
      toast('error', (e as ApiError).message);
    }
  }

  async function runAutopayments() {
    try {
      const r = await api.adminRunAutopayments();
      toast(
        r.failed ? 'info' : 'success',
        r.processed ? `Автоплатежи: обработано ${r.processed}, успешно ${r.succeeded}, с ошибкой ${r.failed}` : 'Автоплатежей к исполнению нет',
      );
      operations.reload();
      users.reload();
    } catch (e) {
      toast('error', (e as ApiError).message);
    }
  }

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1>Администрирование</h1>
          <p className="muted">Клиенты банка, блокировки и журнал всех операций</p>
        </div>
        <button className="btn btn--secondary" onClick={runAutopayments} data-testid="admin-run-autopayments">
          ↻ Выполнить автоплатежи
        </button>
      </div>

      <div className="stats">
        <div className="stat">
          <div className="muted small">Клиентов</div>
          <div className="stat__value" data-testid="stat-clients">
            {clients.length}
          </div>
        </div>
        <div className="stat">
          <div className="muted small">Заблокировано</div>
          <div className="stat__value" data-testid="stat-blocked">
            {blocked}
          </div>
        </div>
        <div className="stat">
          <div className="muted small">Остатки клиентов</div>
          <div className="stat__value money">{formatMoney(total)}</div>
        </div>
        <div className="stat">
          <div className="muted small">Операций в журнале</div>
          <div className="stat__value">{operations.data.length}</div>
        </div>
      </div>

      <div className="tabs" role="tablist">
        <button className={`tab${tab === 'users' ? ' tab--active' : ''}`} onClick={() => setTab('users')} data-testid="tab-users">
          Пользователи
        </button>
        <button className={`tab${tab === 'operations' ? ' tab--active' : ''}`} onClick={() => setTab('operations')} data-testid="tab-operations">
          Все операции
        </button>
      </div>

      {tab === 'users' ? (
        <section className="panel">
          <div className="table-wrap">
            <table className="table" data-testid="admin-users-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Клиент</th>
                  <th>Роль</th>
                  <th className="num">Счетов</th>
                  <th className="num">Остаток</th>
                  <th>Регистрация</th>
                  <th>Статус</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {users.data.map((u) => (
                  <tr key={u.id} data-testid="admin-user-row" data-user-id={u.id} data-email={u.email}>
                    <td className="muted">{u.id}</td>
                    <td>
                      <div>{u.fullName}</div>
                      <div className="muted small">{u.email}</div>
                    </td>
                    <td>
                      <span className={`badge ${u.role === 'ADMIN' ? 'badge--admin' : 'badge--client'}`}>
                        {u.role === 'ADMIN' ? 'Администратор' : 'Клиент'}
                      </span>
                    </td>
                    <td className="num">{u.accountsCount}</td>
                    <td className="num money">{formatMoney(u.totalBalance)}</td>
                    <td className="muted nowrap">{formatDate(u.createdAt)}</td>
                    <td>
                      <span className={`status status--${u.blocked ? 'blocked' : 'active'}`} data-testid="admin-user-status">
                        {u.blocked ? 'Заблокирован' : 'Активен'}
                      </span>
                    </td>
                    <td>
                      {u.role === 'CLIENT' && (
                        <button
                          className={`btn btn--sm ${u.blocked ? 'btn--secondary' : 'btn--danger-ghost'}`}
                          onClick={() => toggle(u)}
                          data-testid="admin-block-toggle"
                        >
                          {u.blocked ? 'Разблокировать' : 'Заблокировать'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : (
        <section className="panel">
          <OperationsTable operations={operations.data} showOwner testId="admin-operations-table" />
        </section>
      )}
    </div>
  );
}

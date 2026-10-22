import type { Operation, OperationType } from '../types';
import { formatDate, formatMoney } from '../format';
import { EmptyState } from './EmptyState';

const typeLabel: Record<OperationType, string> = { OPENING: 'Пополнение', TRANSFER: 'Перевод', PAYMENT: 'Оплата' };

// showOwner добавляет колонку с клиентом для админки
export function OperationsTable({ operations, showOwner = false, testId = 'history-table' }: {
  operations: Operation[];
  showOwner?: boolean;
  testId?: string;
}) {
  if (!operations.length) {
    return <EmptyState icon="🧾" title="Операций пока нет" testId="history-empty" />;
  }
  return (
    <div className="table-wrap">
      <table className="table" data-testid={testId}>
        <thead>
          <tr>
            <th>Дата</th>
            {showOwner && <th>Клиент</th>}
            <th>Тип</th>
            <th>Откуда</th>
            <th>Куда</th>
            <th className="num">Сумма</th>
            <th>Комментарий</th>
          </tr>
        </thead>
        <tbody>
          {operations.map((op) => (
            <tr key={op.id} data-testid="history-row" data-operation-id={op.id}>
              <td className="muted nowrap">{formatDate(op.createdAt)}</td>
              {showOwner && <td data-testid="history-owner">{op.ownerEmail}</td>}
              <td data-testid="history-type">
                <span className={`badge badge--${op.type.toLowerCase()}`}>{typeLabel[op.type] ?? op.type}</span>
                {op.autopaymentId && <span className="badge badge--auto" title="Автоплатёж">авто</span>}
              </td>
              <td data-testid="history-from">
                {op.fromAccountName || '—'}
                {op.cardMasked && <div className="muted small">карта •••• {op.cardMasked.slice(-4)}</div>}
              </td>
              <td data-testid="history-to">{op.type === 'PAYMENT' ? op.categoryName : op.toAccountName}</td>
              <td
                className={`num money ${op.type === 'PAYMENT' ? 'money--out' : op.type === 'OPENING' ? 'money--in' : ''}`}
                data-testid="history-amount"
              >
                {formatMoney(op.amount)}
              </td>
              <td data-testid="history-comment">{op.comment || ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

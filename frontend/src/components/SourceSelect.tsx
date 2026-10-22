import type { Account, Card } from '../types';
import { formatMoney } from '../format';

// value: "account:1" или "card:2"
export function SourceSelect({ accounts, cards, value, onChange, testId }: {
  accounts: Account[];
  cards: Card[];
  value: string;
  onChange: (v: string) => void;
  testId: string;
}) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)} data-testid={testId}>
      <option value="" disabled>
        Счёт или карта
      </option>
      <optgroup label="Счета">
        {accounts.map((a) => (
          <option key={`a${a.id}`} value={`account:${a.id}`}>
            {a.name} ({formatMoney(a.balance)})
          </option>
        ))}
      </optgroup>
      {cards.length > 0 && (
        <optgroup label="Карты">
          {cards.map((c) => (
            <option key={`c${c.id}`} value={`card:${c.id}`} disabled={c.status === 'BLOCKED'}>
              Карта •••• {c.last4} — {c.accountName}
              {c.status === 'BLOCKED' ? ' (заблокирована)' : ''}
            </option>
          ))}
        </optgroup>
      )}
    </select>
  );
}

export function parseSource(value: string): { accountId?: number; cardId?: number } {
  const [kind, id] = value.split(':');
  return kind === 'card' ? { cardId: Number(id) } : { accountId: Number(id) };
}

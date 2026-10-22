import type { Spending } from '../types';
import { formatMoney, formatMonth } from '../format';
import { EmptyState } from './EmptyState';

// Диаграмма на SVG без библиотек
export function SpendingChart({ spending }: { spending: Spending | null }) {
  if (!spending) return <div className="skeleton skeleton--tall" />;
  if (!spending.categories.length) {
    return (
      <EmptyState icon="📊" title="Расходов в этом месяце нет" text="Оплатите услугу — и здесь появится разбивка по категориям" testId="spending-empty" />
    );
  }
  const r = 54;
  const c = 2 * Math.PI * r;
  let offset = 0;
  const total = spending.categories.reduce((s, x) => s + x.amount, 0);

  return (
    <div className="spending" data-testid="spending-chart">
      <div className="donut">
        <svg viewBox="0 0 140 140" width="168" height="168" role="img" aria-label="Расходы по категориям">
          <circle cx="70" cy="70" r={r} fill="none" stroke="#eef1f8" strokeWidth="18" />
          {spending.categories.map((cat) => {
            const len = (cat.amount / total) * c;
            const el = (
              <circle
                key={cat.categoryId}
                cx="70"
                cy="70"
                r={r}
                fill="none"
                stroke={cat.color}
                strokeWidth="18"
                strokeDasharray={`${len} ${c - len}`}
                strokeDashoffset={-offset}
                transform="rotate(-90 70 70)"
              />
            );
            offset += len;
            return el;
          })}
        </svg>
        <div className="donut__center">
          <div className="muted small">{formatMonth(spending.month)}</div>
          <div className="donut__total money" data-testid="spending-total">
            {formatMoney(spending.total)}
          </div>
        </div>
      </div>
      <ul className="legend">
        {spending.categories.map((cat) => (
          <li key={cat.categoryId} data-testid="spending-item" data-category={cat.code}>
            <span className="legend__dot" style={{ background: cat.color }} />
            <span className="legend__name">{cat.name}</span>
            <span className="legend__pct muted">{Math.round((cat.amount / total) * 100)}%</span>
            <span className="legend__sum money" data-testid="spending-amount">
              {formatMoney(cat.amount)}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

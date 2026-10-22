import type { Card } from '../types';

export function BankCard({ card }: { card: Card }) {
  const blocked = card.status === 'BLOCKED';
  return (
    <div className={`bank-card${blocked ? ' bank-card--blocked' : ''}`} data-testid="bank-card" data-card-id={card.id}>
      <div className="bank-card__top">
        <span className="bank-card__bank">Личные финансы</span>
        <span className="bank-card__ps">{card.paymentSystem}</span>
      </div>
      <div className="bank-card__chip" aria-hidden="true" />
      <div className="bank-card__number" data-testid="card-number">
        {card.maskedNumber.replace(/\*{4}/g, '••••')}
      </div>
      <div className="bank-card__bottom">
        <div>
          <div className="bank-card__label">Держатель</div>
          <div>{card.holderName}</div>
        </div>
        <div>
          <div className="bank-card__label">Действует до</div>
          <div data-testid="card-expiry">{card.expiry}</div>
        </div>
      </div>
      {blocked && (
        <div className="bank-card__lock" aria-hidden="true">
          🔒
        </div>
      )}
    </div>
  );
}

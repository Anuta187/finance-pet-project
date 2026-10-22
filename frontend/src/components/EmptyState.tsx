import { ReactNode } from 'react';

export function EmptyState({ icon, title, text, testId, children }: {
  icon: string;
  title: string;
  text?: string;
  testId?: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty" data-testid={testId}>
      <div className="empty__icon" aria-hidden="true">
        {icon}
      </div>
      <div className="empty__title">{title}</div>
      {text && <p className="muted">{text}</p>}
      {children}
    </div>
  );
}

import { Link } from 'react-router-dom';
import { EmptyState } from '../components/EmptyState';

export function NotFoundPage() {
  return (
    <div className="page">
      <div className="panel">
        <EmptyState icon="🧭" title="Страница не найдена" testId="not-found">
          <Link to="/" className="btn btn--primary">
            На главную
          </Link>
        </EmptyState>
      </div>
    </div>
  );
}

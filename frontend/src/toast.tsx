import { createContext, ReactNode, useCallback, useContext, useState } from 'react';

type ToastType = 'success' | 'error' | 'info';
interface Toast {
  id: number;
  type: ToastType;
  text: string;
}

const ToastContext = createContext<(type: ToastType, text: string) => void>(() => {});

let nextId = 1;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = (id: number) => setToasts((list) => list.filter((t) => t.id !== id));

  const show = useCallback((type: ToastType, text: string) => {
    const id = nextId++;
    setToasts((list) => [...list.slice(-3), { id, type, text }]);
    window.setTimeout(() => dismiss(id), 5000);
  }, []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast--${t.type}`} data-testid="toast" data-type={t.type} role="status">
            <span className="toast__icon" aria-hidden="true">
              {t.type === 'success' ? '✓' : t.type === 'error' ? '!' : 'i'}
            </span>
            <span className="toast__text">{t.text}</span>
            <button className="toast__close" onClick={() => dismiss(t.id)} aria-label="Закрыть">
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);

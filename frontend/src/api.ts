import type {
  Account,
  AdminUser,
  AuthResponse,
  Autopayment,
  AutopaymentRunResult,
  Card,
  Category,
  Operation,
  PaymentResult,
  Period,
  Spending,
  TransferResult,
  User,
} from './types';

export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
    public details?: Record<string, unknown>,
  ) {
    super(message);
  }
}

const TOKEN_KEY = 'finance.token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

// вызывается на 401 и USER_BLOCKED
let authFailureHandler: (code: string, message: string) => void = () => {};
export function onAuthFailure(handler: (code: string, message: string) => void) {
  authFailureHandler = handler;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = tokenStore.get();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`/api${path}`, { ...options, headers });
  } catch {
    throw new ApiError('NETWORK_ERROR', 'Сервер недоступен. Проверьте, что бэкенд запущен', 0);
  }
  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) {
    const err = data?.error ?? {};
    const code: string = err.code ?? (res.status >= 500 ? 'SERVER_ERROR' : 'NETWORK_ERROR');
    const message: string = err.message ?? `Ошибка запроса (${res.status})`;
    if (token && (res.status === 401 || code === 'USER_BLOCKED')) {
      authFailureHandler(code, message);
    }
    throw new ApiError(code, message, res.status, err.details);
  }
  return data as T;
}

const post = <T>(path: string, body?: unknown) =>
  request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) });

export const api = {
  // Auth
  login: (email: string, password: string) => post<AuthResponse>('/auth/login', { email, password }),
  register: (payload: { email: string; password: string; fullName: string }) =>
    post<AuthResponse>('/auth/register', payload),
  me: () => request<User>('/auth/me'),

  // Счета, переводы, история
  getAccounts: () => request<Account[]>('/accounts'),
  createAccount: (payload: { name: string; initialBalance?: string }) => post<Account>('/accounts', payload),
  transfer: (payload: { fromAccountId: number; toAccountId: number; amount: string; comment?: string }) =>
    post<TransferResult>('/transfers', payload),
  getOperations: (accountId?: string, limit?: number) => {
    const q = new URLSearchParams();
    if (accountId) q.set('accountId', accountId);
    if (limit) q.set('limit', String(limit));
    const qs = q.toString();
    return request<Operation[]>(`/operations${qs ? `?${qs}` : ''}`);
  },

  // Карты
  getCards: () => request<Card[]>('/cards'),
  issueCard: (accountId: number) => post<Card>('/cards', { accountId }),
  blockCard: (id: number) => post<Card>(`/cards/${id}/block`),
  unblockCard: (id: number) => post<Card>(`/cards/${id}/unblock`),

  // Платежи и аналитика
  getCategories: () => request<Category[]>('/categories'),
  pay: (payload: { categoryId: number; accountId?: number; cardId?: number; amount: string; comment?: string }) =>
    post<PaymentResult>('/payments', payload),
  getSpending: () => request<Spending>('/stats/spending'),

  // Автоплатежи
  getAutopayments: () => request<Autopayment[]>('/autopayments'),
  createAutopayment: (payload: {
    name: string;
    categoryId: number;
    accountId?: number;
    cardId?: number;
    amount: string;
    period: Period;
    startDate?: string;
  }) => post<Autopayment>('/autopayments', payload),
  disableAutopayment: (id: number) => post<Autopayment>(`/autopayments/${id}/disable`),

  // Администрирование
  adminUsers: () => request<AdminUser[]>('/admin/users'),
  adminBlock: (id: number) => post<AdminUser>(`/admin/users/${id}/block`),
  adminUnblock: (id: number) => post<AdminUser>(`/admin/users/${id}/unblock`),
  adminOperations: () => request<Operation[]>('/admin/operations?limit=200'),
  adminRunAutopayments: () => post<AutopaymentRunResult>('/admin/autopayments/run'),
};

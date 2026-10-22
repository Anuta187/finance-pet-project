export type Role = 'CLIENT' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  blocked: boolean;
  createdAt: string;
}

export interface AdminUser extends User {
  accountsCount: number;
  totalBalance: number;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface Account {
  id: number;
  name: string;
  currency: string;
  balance: number;
  createdAt: string;
}

export type OperationType = 'OPENING' | 'TRANSFER' | 'PAYMENT';

export interface Operation {
  id: number;
  type: OperationType;
  fromAccountId: number | null;
  fromAccountName: string | null;
  toAccountId: number | null;
  toAccountName: string | null;
  amount: number;
  comment: string | null;
  createdAt: string;
  categoryId: number | null;
  categoryName: string | null;
  cardId: number | null;
  cardMasked: string | null;
  autopaymentId: number | null;
  ownerId?: number;
  ownerEmail?: string;
}

export interface TransferResult {
  operation: Operation;
  fromAccount: Account;
  toAccount: Account;
}

export type CardStatus = 'ACTIVE' | 'BLOCKED';

export interface Card {
  id: number;
  accountId: number;
  accountName: string;
  maskedNumber: string;
  last4: string;
  paymentSystem: string;
  holderName: string;
  expiryMonth: number;
  expiryYear: number;
  expiry: string;
  status: CardStatus;
  createdAt: string;
}

export interface Category {
  id: number;
  code: string;
  name: string;
  color: string;
}

export interface PaymentResult {
  operation: Operation;
  account: Account;
}

export interface Spending {
  month: string;
  total: number;
  categories: { categoryId: number; code: string; name: string; color: string; amount: number; count: number }[];
}

export type Period = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface Autopayment {
  id: number;
  name: string;
  accountId: number;
  accountName: string;
  cardId: number | null;
  cardMasked: string | null;
  categoryId: number;
  categoryName: string;
  amount: number;
  period: Period;
  startDate: string;
  nextRunDate: string;
  active: boolean;
  lastRunAt: string | null;
  lastStatus: 'SUCCESS' | 'FAILED' | null;
  lastError: string | null;
  createdAt: string;
}

export interface AutopaymentRunResult {
  date: string;
  processed: number;
  succeeded: number;
  failed: number;
}

export interface UiError {
  code: string;
  message: string;
}

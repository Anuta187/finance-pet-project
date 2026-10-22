// Обычные пробелы вместо неразрывных, чтобы было проще проверять в тестах
export function formatMoney(value: number): string {
  const kop = Math.round(Number(value) * 100);
  const sign = kop < 0 ? '-' : '';
  const abs = Math.abs(kop);
  const rub = String(Math.floor(abs / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  const cents = String(abs % 100).padStart(2, '0');
  return `${sign}${rub},${cents} ₽`;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// без сдвига часового пояса
export function formatDay(isoDate: string): string {
  const [y, m, d] = isoDate.split('-');
  return `${d}.${m}.${y}`;
}

export function todayIso(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

const MONTHS = ['январь', 'февраль', 'март', 'апрель', 'май', 'июнь', 'июль', 'август', 'сентябрь', 'октябрь', 'ноябрь', 'декабрь'];

export function formatMonth(ym: string): string {
  const [y, m] = ym.split('-');
  return `${MONTHS[Number(m) - 1]} ${y}`;
}

export const PERIOD_LABEL: Record<string, string> = {
  DAILY: 'Ежедневно',
  WEEKLY: 'Еженедельно',
  MONTHLY: 'Ежемесячно',
};

export function firstName(fullName: string): string {
  return fullName.split(' ')[0] || fullName;
}

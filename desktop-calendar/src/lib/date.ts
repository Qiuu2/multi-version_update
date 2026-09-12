/**
 * 纯日期（'YYYY-MM-DD'）运算。
 * 全部走 UTC 午夜，避免本地时区的夏令时让「加一天」算出 23 或 25 小时。
 */

const DAY = 86400000;

export function iso(y: number, m: number, d: number): string {
  return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}

/** 本机当天 */
export function todayIso(): string {
  const now = new Date();
  return iso(now.getFullYear(), now.getMonth() + 1, now.getDate());
}

function toUtc(key: string): number {
  return Date.parse(`${key}T00:00:00Z`);
}

function fromUtc(ms: number): string {
  const d = new Date(ms);
  return iso(d.getUTCFullYear(), d.getUTCMonth() + 1, d.getUTCDate());
}

/** b - a，单位天 */
export function diffDays(a: string, b: string): number {
  return Math.round((toUtc(b) - toUtc(a)) / DAY);
}

export function shiftDate(key: string, n: number): string {
  return fromUtc(toUtc(key) + n * DAY);
}

/** '2026-09-14' → '09/14'；空串原样返回 */
export function fmtShort(key: string): string {
  return key ? key.slice(5).replace('-', '/') : '';
}

/** 0 = 周日 … 6 = 周六 */
export function weekdayOf(key: string): number {
  return new Date(toUtc(key)).getUTCDay();
}

export function weekdayLabel(key: string): string {
  return '周' + '日一二三四五六'[weekdayOf(key)];
}

/** 下一个周一；当天已是周一则跳到下周一 */
export function nextMonday(key: string): string {
  const wd = weekdayOf(key);
  return shiftDate(key, (8 - wd) % 7 || 7);
}

/** 周一起始的 6 × 7 月网格，返回 42 个日期键 */
export function monthGrid(year: number, month: number): string[] {
  const first = Date.UTC(year, month - 1, 1);
  const shift = (new Date(first).getUTCDay() + 6) % 7;
  const start = Date.UTC(year, month - 1, 1 - shift);
  const out: string[] = [];
  for (let i = 0; i < 42; i++) out.push(fromUtc(start + i * DAY));
  return out;
}

/** 闭区间内的每一天 */
export function daysBetween(a: string, b: string): string[] {
  const lo = a <= b ? a : b;
  const hi = a <= b ? b : a;
  const out: string[] = [];
  for (let ms = toUtc(lo), end = toUtc(hi); ms <= end; ms += DAY) out.push(fromUtc(ms));
  return out;
}

export function isDateKey(v: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(v);
}

export function yearOf(key: string): number {
  return Number(key.slice(0, 4));
}

export function monthOf(key: string): number {
  return Number(key.slice(5, 7));
}

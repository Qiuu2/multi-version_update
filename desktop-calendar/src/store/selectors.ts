import { diffDays } from '../lib/date';
import { timeOf } from '../lib/item';
import type { Item } from '../types';
import type { CalendarState } from './reducer';

/** 分组名 → 识别色 */
export function groupColors(s: CalendarState): Record<string, string> {
  const out: Record<string, string> = {};
  s.groups.forEach((g) => {
    out[g.name] = g.color;
  });
  return out;
}

export function allGroups(s: CalendarState): string[] {
  return s.groups.map((g) => g.name);
}

/** 未分组的色点用 --c-dot-empty，条目色块用 --c-faint（原型如此） */
export function colorOf(s: CalendarState, group: string): string {
  return groupColors(s)[group] || 'var(--c-faint)';
}

/** 分组显隐 + 「显示已完成」两个开关的过滤 */
export function isVisible(s: CalendarState, t: Item): boolean {
  if (s.hidden[t.group]) return false;
  if (!s.showDone && t.done) return false;
  return true;
}

export function itemsOn(s: CalendarState, key: string): Item[] {
  return s.items.filter((t) => t.date === key && isVisible(s, t));
}

/** 日详情与月视图的排序：未完成在前，再按时间 */
export function sortForDay(a: Item, b: Item): number {
  return (a.done ? 1 : 0) - (b.done ? 1 : 0) || timeOf(a).localeCompare(timeOf(b));
}

/** 实时匹配标题 / 分组 / 地点，按日期排序；无期限排最后 */
export function searchMatches(s: CalendarState): Item[] {
  const q = s.search.trim().toLowerCase();
  if (!q) return [];
  return s.items
    .filter(
      (t) =>
        (t.title || '').toLowerCase().includes(q) ||
        (t.group || '').toLowerCase().includes(q) ||
        (t.location || '').toLowerCase().includes(q),
    )
    .slice()
    .sort((a, b) => (a.date || '9999').localeCompare(b.date || '9999'));
}

/**
 * 收集箱里的条目。
 * 刻意不套分组显隐 —— 它是暂存区不是视图，被图例过滤掉会让「新建了却看不见」，
 * 分组筛选只管月视图和右侧栏。已完成仍然跟随「显示已完成」开关。
 */
export function undatedItems(s: CalendarState): Item[] {
  return s.items
    .filter((t) => !t.date && (s.showDone || !t.done))
    // 未完成在前；同组内新建的排最上面 —— 刚敲完就能看见它，
    // 不然列表一长，新条目掉到滚动区外面，看着就像「没建出来」
    .sort((a, b) => (a.done ? 1 : 0) - (b.done ? 1 : 0) || b.id - a.id);
}

/** 底栏统计：今日日程数 / 未来 7 天未完成待办数 */
export function footerCounts(s: CalendarState, today: string): { events: number; todos: number } {
  const events = s.items.filter(
    (t) => t.date === today && t.type === 'event' && isVisible(s, t),
  ).length;
  const todos = s.items.filter(
    (t) =>
      t.type === 'task' && t.date && !t.done && isVisible(s, t) && diffDays(today, t.date) <= 7,
  ).length;
  return { events, todos };
}

/** 右侧栏行的冲突标记：晚于父任务截止日 */
export function hasConflict(t: Item): boolean {
  return !!(t.parentDue && t.date && t.date > t.parentDue);
}

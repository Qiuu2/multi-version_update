import type { Item } from '../types';

/**
 * 条目的显示时间。
 * 原型的种子数据把时间写在标题里（'14:30 量化组周会'），
 * 没有 time 字段时从标题头部解析，保持同样的回退。
 */
export function timeOf(t: Pick<Item, 'time' | 'title'>): string {
  if (t.time) return t.time;
  const m = /^(\d{1,2}:\d{2})/.exec(t.title || '');
  return m ? m[1].padStart(5, '0') : '00:00';
}

/** 去掉标题头部的时间前缀 */
export function stripTime(title: string): string {
  return (title || '').replace(/^\d{1,2}:\d{2}\s*/, '');
}

/** 月视图条目的原生 tooltip：两行 */
export function tooltipOf(t: Item): string {
  const head = (t.type === 'event' ? `${timeOf(t)}  ` : '') + stripTime(t.title);
  const meta =
    (t.group || '未分组') + (t.repeatOn ? ' · 每周重复' : '') + (t.reminder ? ` · ${t.reminder}` : '');
  return `${head}\n${meta}`;
}

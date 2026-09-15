/**
 * 外部 agent（Claude / MCP / curl）对日历的读写操作。
 *
 * 这里只做「读状态 + 算出结果 + 算出要 dispatch 的 action」，不碰传输层，
 * 所以浏览器里可以直接测；真正的收发在 agentBridge.ts。
 *
 * 之所以让前端来执行而不是让 Rust 直接改 calendar.json：
 * 运行中的 app 状态在内存里，外部写文件会被下一次防抖落盘整份盖掉。
 * 统一由前端这一个写入口执行，就不存在竞争。
 */
import type { Action, CalendarState } from '../store/reducer';
import type { Item, ItemType, Priority } from '../types';
import { stripTime } from './item';

/** 对外契约版本，改了字段含义就要 +1 */
export const AGENT_API_VERSION = 1;

export interface AgentOp {
  op?: unknown;
  [k: string]: unknown;
}

export interface AgentPlan {
  result: Record<string, unknown>;
  action?: Action;
}

/** 对外暴露的条目形状：只给稳定字段，内部的 repeatId / spanId 之类不外泄 */
export interface PublicItem {
  id: number;
  type: ItemType;
  group: string;
  title: string;
  date: string;
  time: string;
  withTime: boolean;
  done: boolean;
  priority: string;
  location: string;
  reminder: string;
  notes: string;
}

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;
const TIME_RE = /^([01]?\d|2[0-3]):[0-5]\d$/;
const PRIORITIES = ['', '低', '中', '高'];

function str(v: unknown): string {
  return typeof v === 'string' ? v.trim() : '';
}

function bool(v: unknown, dflt = false): boolean {
  return typeof v === 'boolean' ? v : dflt;
}

export function publicItem(t: Item): PublicItem {
  return {
    id: t.id,
    type: t.type,
    group: t.group,
    title: t.title,
    date: t.date,
    time: t.time || '00:00',
    withTime: !!t.withTime,
    done: !!t.done,
    priority: t.priority ?? '',
    location: t.location ?? '',
    reminder: t.reminder ?? '',
    notes: t.notes ?? '',
  };
}

/**
 * 把外部传来的字段读成 Item 的一部分。
 * 只认显式出现的键 —— update 时没写的字段必须保持原样，不能被默认值冲掉。
 */
function readFields(raw: Record<string, unknown>): { patch: Partial<Item>; error?: string } {
  const patch: Partial<Item> = {};

  if ('title' in raw) {
    const t = str(raw.title);
    if (!t) return { patch, error: 'title 不能为空' };
    patch.title = t;
  }
  if ('date' in raw) {
    const d = str(raw.date);
    if (d && !DATE_RE.test(d)) return { patch, error: `date 要写成 YYYY-MM-DD，收到「${d}」` };
    patch.date = d;
  }
  if ('time' in raw) {
    const t = str(raw.time);
    if (t && !TIME_RE.test(t)) return { patch, error: `time 要写成 HH:MM，收到「${t}」` };
    // 给了时间就是定点日程，清空时间则回到全天
    patch.time = t || '00:00';
    patch.withTime = !!t;
  }
  if ('type' in raw) {
    const v = str(raw.type);
    if (v !== 'task' && v !== 'event') return { patch, error: 'type 只能是 task 或 event' };
    patch.type = v;
  }
  if ('priority' in raw) {
    const v = str(raw.priority);
    if (!PRIORITIES.includes(v)) return { patch, error: 'priority 只能是 低 / 中 / 高，或留空' };
    patch.priority = v as Priority;
  }
  if ('group' in raw) patch.group = str(raw.group);
  if ('notes' in raw) patch.notes = str(raw.notes);
  if ('location' in raw) patch.location = str(raw.location);
  if ('reminder' in raw) patch.reminder = str(raw.reminder);
  if ('done' in raw) patch.done = bool(raw.done);

  return { patch };
}

/** 有日期的按日期 + 时间排，无日期的排在最后 */
function byDate(a: Item, b: Item): number {
  if (!a.date !== !b.date) return a.date ? -1 : 1;
  return a.date.localeCompare(b.date) || (a.time || '').localeCompare(b.time || '') || a.id - b.id;
}

function fail(error: string): AgentPlan {
  return { result: { error } };
}

/**
 * 算出一个操作的结果与副作用。纯函数：不 dispatch、不碰 window。
 */
export function planAgentOp(s: CalendarState, raw: AgentOp): AgentPlan {
  const op = str(raw.op);

  switch (op) {
    case 'health':
      return {
        result: {
          ok: true,
          app: 'local-calendar',
          api: AGENT_API_VERSION,
          mode: 'live',
          items: s.items.length,
          groups: s.groups.length,
        },
      };

    case 'groups':
      return {
        result: {
          groups: s.groups.map((g) => ({ name: g.name, color: g.color, archived: !!g.archived })),
        },
      };

    case 'list': {
      const from = str(raw.from);
      const to = str(raw.to);
      const group = str(raw.group);
      const q = str(raw.query).toLowerCase();
      const includeDone = bool(raw.includeDone, true);
      const undatedOnly = bool(raw.undatedOnly);
      if (from && !DATE_RE.test(from)) return fail(`from 要写成 YYYY-MM-DD，收到「${from}」`);
      if (to && !DATE_RE.test(to)) return fail(`to 要写成 YYYY-MM-DD，收到「${to}」`);

      const hit = s.items.filter((t) => {
        if (!includeDone && t.done) return false;
        if (undatedOnly) {
          if (t.date) return false;
        } else {
          if (from && (!t.date || t.date < from)) return false;
          if (to && (!t.date || t.date > to)) return false;
        }
        if (group && t.group !== group) return false;
        if (q && !`${t.title} ${t.notes ?? ''}`.toLowerCase().includes(q)) return false;
        return true;
      });

      const n = Number(raw.limit);
      const limit = Number.isFinite(n) && n > 0 ? Math.floor(n) : 200;
      const sorted = hit.slice().sort(byDate);
      return { result: { total: hit.length, items: sorted.slice(0, limit).map(publicItem) } };
    }

    case 'add': {
      const src = (
        raw.item && typeof raw.item === 'object' ? raw.item : raw
      ) as Record<string, unknown>;
      const { patch, error } = readFields(src);
      if (error) return fail(error);
      if (!patch.title) return fail('title 不能为空');

      const item: Item = {
        id: s.nextId,
        type: 'task',
        group: '',
        title: patch.title,
        date: '',
        time: '00:00',
        withTime: false,
        ...patch,
      };
      // 没指定类型时：给了具体时间的当日程，其余当任务
      if (!('type' in src)) item.type = item.withTime && item.date ? 'event' : 'task';

      return {
        result: { id: item.id, item: publicItem(item) },
        action: { type: 'agentAdd', item },
      };
    }

    case 'update': {
      const id = Number(raw.id);
      const t = s.items.find((x) => x.id === id);
      if (!t) return fail(`没有 id=${str(raw.id) || raw.id} 的条目`);
      const src = (
        raw.patch && typeof raw.patch === 'object' ? raw.patch : raw
      ) as Record<string, unknown>;
      const { patch, error } = readFields(src);
      if (error) return fail(error);
      if (!Object.keys(patch).length) return fail('没有要修改的字段');

      const next: Item = { ...t, ...patch, id: t.id };
      return {
        result: { item: publicItem(next) },
        action: {
          type: 'agentPatch',
          id,
          patch,
          label: `已更新「${stripTime(next.title)}」`,
        },
      };
    }

    case 'delete': {
      const id = Number(raw.id);
      const t = s.items.find((x) => x.id === id);
      if (!t) return fail(`没有 id=${str(raw.id) || raw.id} 的条目`);
      return {
        result: { deleted: publicItem(t) },
        action: { type: 'agentDelete', id, label: `已删除「${stripTime(t.title)}」` },
      };
    }

    case '':
      return fail('缺少 op 字段');
    default:
      return fail(`不认识的 op「${op}」`);
  }
}

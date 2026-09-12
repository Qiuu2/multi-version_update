import { HISTORY_LIMIT, NEW_GROUP_COLORS } from '../constants';
import { daysBetween, fmtShort, nextMonday, shiftDate, todayIso } from '../lib/date';
import { stripTime, timeOf } from '../lib/item';
import type {
  Anchor,
  ContextMenuState,
  CustomGroup,
  Item,
  ItemType,
  ModalDraft,
  Selection,
  Theme,
} from '../types';
import { SEED_HIDDEN, SEED_NEXT_ID, seedItems } from './seed';

/** 进撤销栈的那部分状态 */
interface Snapshot {
  items: Item[];
  nextId: number;
  hidden: Record<string, boolean>;
  customGroups: CustomGroup[];
}

export interface CalendarState {
  /** 当前月份 */
  year: number;
  month: number;

  items: Item[];
  nextId: number;
  hidden: Record<string, boolean>;
  customGroups: CustomGroup[];
  showDone: boolean;
  showOther: boolean;
  theme: Theme;

  search: string;
  searchFocused: boolean;

  settingsOpen: boolean;
  undatedOpen: boolean;
  themeMenuOpen: boolean;
  panelClosed: boolean;

  dayOpen: string | null;
  dayAnchor: Anchor | null;

  sel: Selection | null;
  /** 已松手，批量操作条可见 */
  selActive: boolean;
  selAnchor: Anchor | null;
  rangeTitle: string;
  rangeGroupsOpen: boolean;

  ctx: ContextMenuState | null;
  modal: ModalDraft | null;
  toast: string | null;

  newGroupName: string;
  newGroupColor: string;

  dragId: number | null;
  dragOver: string | null;

  history: Snapshot[];
}

/** 持久化的字段；UI 瞬时状态不落盘 */
export type Persisted = Pick<
  CalendarState,
  'items' | 'nextId' | 'hidden' | 'customGroups' | 'showDone' | 'showOther' | 'theme'
>;

export function pickPersisted(s: CalendarState): Persisted {
  return {
    items: s.items,
    nextId: s.nextId,
    hidden: s.hidden,
    customGroups: s.customGroups,
    showDone: s.showDone,
    showOther: s.showOther,
    theme: s.theme,
  };
}

export function initialState(): CalendarState {
  const t0 = todayIso();
  return {
    year: Number(t0.slice(0, 4)),
    month: Number(t0.slice(5, 7)),
    items: seedItems(),
    nextId: SEED_NEXT_ID,
    hidden: { ...SEED_HIDDEN },
    customGroups: [],
    showDone: true,
    showOther: true,
    theme: 'light',
    search: '',
    searchFocused: false,
    settingsOpen: false,
    undatedOpen: false,
    themeMenuOpen: false,
    panelClosed: false,
    dayOpen: null,
    dayAnchor: null,
    sel: null,
    selActive: false,
    selAnchor: null,
    rangeTitle: '',
    rangeGroupsOpen: false,
    ctx: null,
    modal: null,
    toast: null,
    newGroupName: '',
    newGroupColor: NEW_GROUP_COLORS[0],
    dragId: null,
    dragOver: null,
    history: [],
  };
}

export type Action =
  | { type: 'hydrate'; data: Partial<Persisted> }
  | { type: 'prevMonth' }
  | { type: 'nextMonth' }
  | { type: 'goToday' }
  | { type: 'gotoMonth'; year: number; month: number }
  | { type: 'setTheme'; theme: Theme }
  | { type: 'toggleThemeMenu' }
  | { type: 'toggleSettings' }
  | { type: 'toggleShowDone' }
  | { type: 'toggleShowOther' }
  | { type: 'toggleUndated' }
  | { type: 'closePanel' }
  | { type: 'reopenPanel' }
  | { type: 'setSearch'; value: string }
  | { type: 'setSearchFocused'; value: boolean }
  | { type: 'toggleGroupHidden'; name: string }
  | { type: 'openDay'; key: string; anchor: Anchor | null }
  | { type: 'closeDay' }
  | { type: 'openCreate'; date: string; kind: ItemType }
  | { type: 'openEdit'; id: number }
  | { type: 'closeModal' }
  | { type: 'patchModal'; patch: Partial<ModalDraft> }
  | { type: 'saveModal' }
  | { type: 'deleteCurrent' }
  | { type: 'deleteRepeatGroup' }
  | { type: 'setNewGroupName'; value: string }
  | { type: 'setNewGroupColor'; value: string }
  | { type: 'addGroup' }
  | { type: 'toggleItemDone'; id: number }
  | { type: 'selStart'; key: string; anchor: Anchor | null }
  | { type: 'selMove'; key: string; anchor: Anchor | null }
  | { type: 'selCommit' }
  | { type: 'selClear' }
  | { type: 'setRangeTitle'; value: string }
  | { type: 'toggleRangeGroups' }
  | { type: 'rangeEachDay' }
  | { type: 'rangeSpan' }
  | { type: 'rangeSetGroup'; group: string }
  | { type: 'rangeDelete' }
  | { type: 'openCtx'; ctx: ContextMenuState }
  | { type: 'closeCtx' }
  | { type: 'moveItem'; id: number; to: string }
  | { type: 'copyItem'; id: number; to: string }
  | { type: 'clearDay'; key: string }
  | { type: 'deleteItem'; id: number }
  | { type: 'dragStart'; id: number }
  | { type: 'dragOver'; key: string | null }
  | { type: 'dropOn'; key: string }
  | { type: 'showToast'; text: string }
  | { type: 'dismissToast' }
  | { type: 'undo' }
  | { type: 'undoFromToast' };

function snapshot(s: CalendarState): Snapshot {
  return {
    items: s.items.map((t) => ({ ...t })),
    nextId: s.nextId,
    hidden: { ...s.hidden },
    customGroups: s.customGroups.map((g) => ({ ...g })),
  };
}

/** 写操作统一入口：先压快照，再套用补丁 */
function commit(s: CalendarState, patch: Partial<CalendarState>): CalendarState {
  return {
    ...s,
    ...patch,
    history: s.history.concat([snapshot(s)]).slice(-HISTORY_LIMIT),
  };
}

function groupNames(s: CalendarState): string[] {
  return ['学业', '中信实习', '中控项目', '求职'].concat(s.customGroups.map((g) => g.name));
}

function draftFrom(item: Item): ModalDraft {
  const t = timeOf(item);
  return {
    mode: 'edit',
    kind: item.type,
    id: item.id,
    title: item.title,
    group: item.group || '',
    priority: item.priority || '',
    date: item.date || '',
    time: t,
    withTime: item.withTime != null ? !!item.withTime : t !== '00:00',
    location: item.location || '',
    reminder: item.reminder || '',
    notes: item.notes || '',
    parent: item.parent,
    parentDue: item.parentDue,
    subtasks: (item.subtasks || []).map((x) => ({ ...x })),
    repeatOn: !!item.repeatOn,
    repeatWeeks: item.repeatWeeks || '15',
    repeatId: item.repeatId,
    groupListOpen: false,
    pickerOpen: false,
    timeOpen: false,
    reminderOpen: false,
    pickerYear: null,
    pickerMonth: null,
  };
}

function emptyDraft(date: string, kind: ItemType): ModalDraft {
  return {
    mode: 'create',
    kind,
    id: null,
    title: '',
    group: '',
    priority: '',
    date: date || '',
    // 日详情的「+」默认 09:00 并提前 10 分钟提醒
    time: kind === 'event' ? '09:00' : '',
    withTime: kind === 'event',
    location: '',
    reminder: kind === 'event' ? '提前 10 分钟' : '',
    notes: '',
    subtasks: [],
    repeatOn: false,
    repeatWeeks: '15',
    groupListOpen: false,
    pickerOpen: false,
    timeOpen: false,
    reminderOpen: false,
    pickerYear: null,
    pickerMonth: null,
  };
}

/** 草稿落成一条记录 */
function itemFromDraft(d: ModalDraft, id: number): Item {
  return {
    id,
    type: d.kind,
    group: d.group,
    title: d.title.trim(),
    date: d.date,
    time: d.time || '00:00',
    withTime: d.withTime,
    location: d.location,
    reminder: d.reminder,
    notes: d.notes,
    priority: d.priority,
    subtasks: d.subtasks,
    repeatOn: d.repeatOn,
    repeatWeeks: d.repeatWeeks,
    repeatId: d.repeatId,
    parent: d.parent,
    parentDue: d.parentDue,
  };
}

/** 「每周重复 共 N 周」展开成 N 条同 repeatId 的独立记录 */
function expandRepeat(base: Item, weeks: number, startId: number): Item[] {
  const out: Item[] = [];
  for (let i = 0; i < weeks; i++) {
    out.push({ ...base, id: i === 0 ? base.id : startId + i - 1, date: shiftDate(base.date, i * 7) });
  }
  return out;
}

function selectionDays(s: CalendarState): string[] {
  return s.sel ? daysBetween(s.sel.start, s.sel.end) : [];
}

export function reducer(s: CalendarState, a: Action): CalendarState {
  switch (a.type) {
    case 'hydrate':
      return { ...s, ...a.data };

    case 'prevMonth':
      return s.month === 1 ? { ...s, year: s.year - 1, month: 12 } : { ...s, month: s.month - 1 };
    case 'nextMonth':
      return s.month === 12 ? { ...s, year: s.year + 1, month: 1 } : { ...s, month: s.month + 1 };
    case 'goToday': {
      const t0 = todayIso();
      return { ...s, year: Number(t0.slice(0, 4)), month: Number(t0.slice(5, 7)) };
    }
    case 'gotoMonth':
      return { ...s, year: a.year, month: a.month };

    case 'setTheme':
      return { ...s, theme: a.theme, themeMenuOpen: false };
    case 'toggleThemeMenu':
      return { ...s, themeMenuOpen: !s.themeMenuOpen, settingsOpen: false };
    case 'toggleSettings':
      return { ...s, settingsOpen: !s.settingsOpen, undatedOpen: false, themeMenuOpen: false };
    case 'toggleShowDone':
      return { ...s, showDone: !s.showDone };
    case 'toggleShowOther':
      return { ...s, showOther: !s.showOther };
    case 'toggleUndated':
      return { ...s, undatedOpen: !s.undatedOpen, settingsOpen: false, themeMenuOpen: false };

    case 'closePanel':
      return { ...s, panelClosed: true, modal: null };
    case 'reopenPanel':
      return { ...s, panelClosed: false };

    case 'setSearch':
      return { ...s, search: a.value };
    case 'setSearchFocused':
      return { ...s, searchFocused: a.value };

    case 'toggleGroupHidden':
      return commit(s, { hidden: { ...s.hidden, [a.name]: !s.hidden[a.name] } });

    case 'openDay':
      return {
        ...s,
        dayOpen: a.key,
        dayAnchor: a.anchor,
        settingsOpen: false,
        undatedOpen: false,
        themeMenuOpen: false,
      };
    case 'closeDay':
      return { ...s, dayOpen: null, dayAnchor: null };

    case 'openCreate':
      return {
        ...s,
        modal: emptyDraft(a.date, a.kind),
        settingsOpen: false,
        undatedOpen: false,
        themeMenuOpen: false,
        dayOpen: null,
        dayAnchor: null,
      };
    case 'openEdit': {
      const item = s.items.find((t) => t.id === a.id);
      if (!item) return s;
      return {
        ...s,
        modal: draftFrom(item),
        settingsOpen: false,
        undatedOpen: false,
        themeMenuOpen: false,
      };
    }
    case 'closeModal':
      return { ...s, modal: null };
    case 'patchModal':
      return s.modal ? { ...s, modal: { ...s.modal, ...a.patch } } : s;

    case 'saveModal': {
      const d = s.modal;
      if (!d) return s;
      const title = d.title.trim();
      // 空标题当作放弃
      if (!title) return { ...s, modal: null };

      const weeks = Math.max(1, Number(d.repeatWeeks) || 1);
      // 重复开着、周数 > 1、有日期、且还不属于任何一组时才展开
      const shouldExpand = d.repeatOn && weeks > 1 && !!d.date && !d.repeatId;

      if (d.mode === 'create') {
        const base = itemFromDraft({ ...d, title }, s.nextId);
        if (shouldExpand) {
          base.repeatId = `rp${s.nextId}`;
          const batch = expandRepeat(base, weeks, s.nextId + 1);
          return commit(s, {
            items: s.items.concat(batch),
            nextId: s.nextId + weeks,
            modal: null,
          });
        }
        return commit(s, { items: s.items.concat([base]), nextId: s.nextId + 1, modal: null });
      }

      // 编辑
      const repeatId = shouldExpand ? `rp${s.nextId}` : d.repeatId;
      const updated = s.items.map((t) =>
        t.id === d.id ? { ...t, ...itemFromDraft({ ...d, title, repeatId }, t.id) } : t,
      );
      if (shouldExpand) {
        const edited = updated.find((t) => t.id === d.id)!;
        const extra = expandRepeat(edited, weeks, s.nextId).slice(1);
        return commit(s, {
          items: updated.concat(extra),
          nextId: s.nextId + weeks - 1,
          modal: null,
        });
      }
      return commit(s, { items: updated, modal: null });
    }

    case 'deleteCurrent': {
      const d = s.modal;
      if (!d || d.id == null) return s;
      const label = d.title.trim() || '该条目';
      return commit(s, {
        items: s.items.filter((t) => t.id !== d.id),
        modal: null,
        toast: `已删除「${label}」`,
      });
    }

    case 'deleteRepeatGroup': {
      const d = s.modal;
      if (!d || d.id == null) return s;
      const label = d.title.trim() || '该条目';
      const target = s.items.find((t) => t.id === d.id);
      // 有 repeatId 走分组键；种子数据没有，退回「同名且重复」的匹配
      const inGroup = (t: Item) =>
        t.id === d.id ||
        (target?.repeatId ? t.repeatId === target.repeatId : !!t.repeatOn && t.title === target?.title);
      return commit(s, {
        items: s.items.filter((t) => !inGroup(t)),
        modal: null,
        toast: `已删除「${label}」整组重复日程`,
      });
    }

    case 'setNewGroupName':
      return { ...s, newGroupName: a.value };
    case 'setNewGroupColor':
      return { ...s, newGroupColor: a.value };
    case 'addGroup': {
      const name = s.newGroupName.trim();
      if (!name || groupNames(s).includes(name)) return s;
      const color =
        s.newGroupColor || NEW_GROUP_COLORS[s.customGroups.length % NEW_GROUP_COLORS.length];
      return {
        ...s,
        customGroups: s.customGroups.concat([{ name, color }]),
        newGroupName: '',
        modal: s.modal ? { ...s.modal, group: name, groupListOpen: false } : s.modal,
      };
    }

    case 'toggleItemDone':
      return commit(s, {
        items: s.items.map((t) => (t.id === a.id ? { ...t, done: !t.done } : t)),
      });

    case 'selStart':
      return { ...s, sel: { start: a.key, end: a.key }, selActive: false, ctx: null, selAnchor: a.anchor };
    case 'selMove':
      return {
        ...s,
        sel: { start: s.sel ? s.sel.start : a.key, end: a.key },
        selAnchor: a.anchor || s.selAnchor,
      };
    case 'selCommit':
      return { ...s, selActive: true, dayOpen: null, dayAnchor: null };
    case 'selClear':
      return { ...s, sel: null, selActive: false, rangeGroupsOpen: false, rangeTitle: '' };

    case 'setRangeTitle':
      return { ...s, rangeTitle: a.value };
    case 'toggleRangeGroups':
      return { ...s, rangeGroupsOpen: !s.rangeGroupsOpen };

    case 'rangeEachDay': {
      const days = selectionDays(s);
      if (!days.length) return s;
      const name = s.rangeTitle.trim() || '新任务';
      let id = s.nextId;
      const add: Item[] = days.map((date) => ({
        id: id++,
        type: 'task',
        group: '',
        title: name,
        date,
      }));
      return commit(s, {
        items: s.items.concat(add),
        nextId: id,
        sel: null,
        selActive: false,
        rangeGroupsOpen: false,
        rangeTitle: '',
        toast: `已在 ${days.length} 天各新建一条「${name}」`,
      });
    }

    case 'rangeSpan': {
      const days = selectionDays(s);
      if (!days.length) return s;
      const name = s.rangeTitle.trim() || '新日程';
      const spanId = `sp${Date.now()}`;
      let id = s.nextId;
      const add: Item[] = days.map((date, i) => ({
        id: id++,
        type: 'event',
        group: '',
        title: name,
        date,
        time: '09:00',
        withTime: true,
        spanId,
        spanLabel: `${i + 1}/${days.length}`,
      }));
      return commit(s, {
        items: s.items.concat(add),
        nextId: id,
        sel: null,
        selActive: false,
        rangeGroupsOpen: false,
        rangeTitle: '',
        toast: `已建跨 ${days.length} 天的日程「${name}」`,
      });
    }

    case 'rangeSetGroup': {
      const days = selectionDays(s);
      const hit = s.items.filter((t) => t.date && days.includes(t.date));
      if (!hit.length) return { ...s, rangeGroupsOpen: false };
      return commit(s, {
        items: s.items.map((t) => (t.date && days.includes(t.date) ? { ...t, group: a.group } : t)),
        rangeGroupsOpen: false,
        toast: `已把 ${hit.length} 条改到「${a.group}」`,
      });
    }

    case 'rangeDelete': {
      const days = selectionDays(s);
      const hit = s.items.filter((t) => t.date && days.includes(t.date));
      if (!hit.length) return { ...s, sel: null, selActive: false, rangeTitle: '', rangeGroupsOpen: false };
      return commit(s, {
        items: s.items.filter((t) => !(t.date && days.includes(t.date))),
        sel: null,
        selActive: false,
        rangeGroupsOpen: false,
        rangeTitle: '',
        toast: `已删除区间内 ${hit.length} 条`,
      });
    }

    case 'openCtx':
      return {
        ...s,
        ctx: a.ctx,
        dayOpen: null,
        dayAnchor: null,
        settingsOpen: false,
        themeMenuOpen: false,
      };
    case 'closeCtx':
      return { ...s, ctx: null };

    case 'moveItem': {
      const t = s.items.find((x) => x.id === a.id);
      if (!t) return s;
      return commit(s, {
        items: s.items.map((x) => (x.id === a.id ? { ...x, date: a.to } : x)),
        toast: `「${stripTime(t.title)}」已改到 ${fmtShort(a.to)}`,
      });
    }

    case 'copyItem': {
      const t = s.items.find((x) => x.id === a.id);
      if (!t) return s;
      return commit(s, {
        items: s.items.concat([{ ...t, id: s.nextId, date: a.to }]),
        nextId: s.nextId + 1,
        toast: `已复制一条到 ${fmtShort(a.to)}`,
      });
    }

    case 'clearDay': {
      const n = s.items.filter((t) => t.date === a.key).length;
      if (!n) return s;
      return commit(s, {
        items: s.items.filter((t) => t.date !== a.key),
        toast: `已清空 ${fmtShort(a.key)} 的 ${n} 条`,
      });
    }

    case 'deleteItem': {
      const t = s.items.find((x) => x.id === a.id);
      if (!t) return s;
      return commit(s, {
        items: s.items.filter((x) => x.id !== a.id),
        toast: `已删除「${stripTime(t.title)}」`,
      });
    }

    case 'dragStart':
      return { ...s, dragId: a.id };
    case 'dragOver':
      return s.dragOver === a.key ? s : { ...s, dragOver: a.key };
    case 'dropOn': {
      const id = s.dragId;
      if (id == null) return { ...s, dragId: null, dragOver: null };
      const t = s.items.find((x) => x.id === id);
      return commit(s, {
        items: s.items.map((x) => (x.id === id ? { ...x, date: a.key } : x)),
        dragId: null,
        dragOver: null,
        toast: t ? `已改到 ${fmtShort(a.key)}` : s.toast,
      });
    }

    case 'showToast':
      return { ...s, toast: a.text };
    case 'dismissToast':
      return { ...s, toast: null };

    case 'undo':
    case 'undoFromToast': {
      if (!s.history.length) return a.type === 'undoFromToast' ? { ...s, toast: null } : s;
      const history = s.history.slice();
      const snap = history.pop()!;
      return {
        ...s,
        items: snap.items,
        nextId: snap.nextId,
        hidden: snap.hidden,
        customGroups: snap.customGroups,
        history,
        toast: a.type === 'undoFromToast' ? null : s.toast,
      };
    }

    default:
      return s;
  }
}

/** 右键菜单「改到下周一」用 */
export { nextMonday };

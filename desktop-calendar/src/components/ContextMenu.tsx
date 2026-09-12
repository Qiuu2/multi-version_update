import { nextMonday, shiftDate, todayIso } from '../lib/date';
import { useCalendar, useDispatch } from '../store/context';
import base from '../styles/base.module.css';
import type { Action } from '../store/reducer';

interface MenuItem {
  label: string;
  danger?: boolean;
  run: () => void;
}

/** 宽 150px，首行是灰色上下文标题 */
export function ContextMenu() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const ctx = s.ctx;
  if (!ctx) return null;

  const close = () => dispatch({ type: 'closeCtx' });
  const fire = (a: Action) => () => {
    close();
    dispatch(a);
  };

  let items: MenuItem[] = [];

  if (ctx.kind === 'cell') {
    items = [
      { label: '新建日程', run: fire({ type: 'openCreate', date: ctx.key, kind: 'event' }) },
      { label: '新建任务', run: fire({ type: 'openCreate', date: ctx.key, kind: 'task' }) },
      { label: '清空这天', danger: true, run: fire({ type: 'clearDay', key: ctx.key }) },
    ];
  } else {
    const t = s.items.find((x) => x.id === ctx.id);
    if (t) {
      const from = t.date || todayIso();
      items = [
        { label: '改到明天', run: fire({ type: 'moveItem', id: t.id, to: shiftDate(from, 1) }) },
        { label: '改到下周一', run: fire({ type: 'moveItem', id: t.id, to: nextMonday(from) }) },
        { label: '复制到明天', run: fire({ type: 'copyItem', id: t.id, to: shiftDate(from, 1) }) },
        { label: '编辑…', run: fire({ type: 'openEdit', id: t.id }) },
        { label: '删除', danger: true, run: fire({ type: 'deleteItem', id: t.id }) },
      ];
      if (t.type === 'task') {
        items.splice(3, 0, {
          label: t.done ? '标为未完成' : '标记完成',
          run: () => {
            close();
            dispatch({ type: 'toggleItemDone', id: t.id });
            dispatch({ type: 'showToast', text: t.done ? '已标为未完成' : '已标记完成' });
          },
        });
      }
    }
  }

  return (
    <div
      data-daypop="1"
      className={base.popover}
      style={{ left: ctx.left, top: ctx.top, width: 150, boxShadow: 'var(--shadow-popover)', zIndex: 12 }}
    >
      <div
        className={base.ellipsis}
        style={{ padding: '4px 7px 5px', fontSize: 11, color: 'var(--c-faint)' }}
      >
        {ctx.title}
      </div>
      {items.map((it) => (
        <button
          key={it.label}
          type="button"
          className={base.popoverItem}
          style={{ color: it.danger ? 'var(--c-danger)' : 'var(--c-text)', whiteSpace: 'nowrap' }}
          onClick={it.run}
        >
          {it.label}
        </button>
      ))}
    </div>
  );
}

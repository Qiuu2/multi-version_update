import { MAX_CELL_ENTRIES, PANEL_H, PANEL_W, WEEKDAY_LABELS } from '../constants';
import { useRangeSelect } from '../hooks/useRangeSelect';
import { monthGrid, shiftDate, todayIso } from '../lib/date';
import { tooltipOf } from '../lib/item';
import { useCalendar, useDispatch } from '../store/context';
import { colorOf, itemsOn, sortForDay } from '../store/selectors';
import base from '../styles/base.module.css';
import { usePanel } from './PanelContext';
import { RepeatIcon } from './icons';
import styles from './MonthGrid.module.css';

export function MonthGrid() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const { anchorOf, toPanel } = usePanel();
  const range = useRangeSelect(s.sel);
  const today = todayIso();
  const cells = monthGrid(s.year, s.month);

  const inSel = (key: string) => {
    if (!s.sel) return false;
    const lo = s.sel.start <= s.sel.end ? s.sel.start : s.sel.end;
    const hi = s.sel.start <= s.sel.end ? s.sel.end : s.sel.start;
    return key >= lo && key <= hi;
  };

  return (
    <div className={styles.wrap}>
      <div className={styles.weekRow}>
        {WEEKDAY_LABELS.map((label, i) => (
          <div
            key={label}
            className={styles.weekLabel}
            style={{ color: i >= 5 ? 'var(--c-faint)' : 'var(--c-muted)' }}
          >
            {label}
          </div>
        ))}
      </div>

      <div className={styles.grid}>
        {cells.map((key, i) => {
          const y = Number(key.slice(0, 4));
          const m = Number(key.slice(5, 7));
          const day = Number(key.slice(8, 10));
          const isCurrent = y === s.year && m === s.month;
          const isToday = key === today;
          const isWeekend = i % 7 >= 5;

          const all = itemsOn(s, key).slice().sort(sortForDay);
          const shown = all.slice(0, MAX_CELL_ENTRIES);
          const overflow = all.length - shown.length;

          const selected = inSel(key);
          // 跨行时自动断成多段：行首/行尾或邻格不在选区就补侧边
          const openLeft = i % 7 === 0 || !inSel(shiftDate(key, -1));
          const openRight = i % 7 === 6 || !inSel(shiftDate(key, 1));

          return (
            <div
              key={key}
              data-daycell="1"
              className={styles.cell}
              style={{
                background: isCurrent
                  ? isWeekend
                    ? 'var(--c-weekend)'
                    : 'var(--c-bg)'
                  : isWeekend
                    ? 'var(--c-other-weekend)'
                    : 'var(--c-other)',
              }}
              onMouseDown={(e) => {
                if (e.button !== 0) return;
                range.begin(key, anchorOf(e.currentTarget));
              }}
              onMouseEnter={(e) => range.extend(key, anchorOf(e.currentTarget))}
              onClick={(e) => {
                // 框选松手带出的那一次 click 不算
                if (range.consumeClick()) return;
                dispatch({ type: 'selClear' });
                dispatch({ type: 'openDay', key, anchor: anchorOf(e.currentTarget) });
              }}
              onContextMenu={(e) => {
                e.preventDefault();
                e.stopPropagation();
                openCellMenu(e.clientX, e.clientY, key);
              }}
              onDragOver={(e) => {
                e.preventDefault();
                dispatch({ type: 'dragOver', key });
              }}
              onDrop={(e) => {
                e.preventDefault();
                dispatch({ type: 'dropOn', key });
              }}
            >
              {selected && (
                <div
                  className={styles.selOverlay}
                  style={{
                    borderLeft: openLeft ? '1.5px solid var(--c-accent)' : 'none',
                    borderRight: openRight ? '1.5px solid var(--c-accent)' : 'none',
                    borderRadius: openLeft
                      ? openRight
                        ? '6px'
                        : '6px 0 0 6px'
                      : openRight
                        ? '0 6px 6px 0'
                        : '0',
                  }}
                />
              )}
              {s.dragOver === key && s.dragId != null && <div className={styles.dropPreview} />}

              <div
                className={styles.dateChip}
                style={{
                  fontWeight: isToday ? 700 : 400,
                  color: isToday ? 'var(--c-accent-deep)' : 'var(--c-text)',
                  opacity: isCurrent ? 1 : 0.3,
                  background: isToday ? 'var(--c-accent-soft)' : 'transparent',
                }}
              >
                {!isCurrent && !s.showOther ? '' : day}
              </div>

              <div className={styles.entries}>
                {shown.map((t) => {
                  const color = colorOf(s, t.group);
                  const ring = s.modal?.id === t.id ? '0 0 0 1.5px var(--c-accent)' : 'none';
                  const common = {
                    title: tooltipOf(t),
                    draggable: true,
                    onDragStart: () => dispatch({ type: 'dragStart', id: t.id }),
                    // 条目上的 mousedown 不能冒泡，否则拖条目会被当成区间框选
                    onMouseDown: (e: React.MouseEvent) => e.stopPropagation(),
                    onClick: (e: React.MouseEvent) => {
                      e.stopPropagation();
                      dispatch({ type: 'openDay', key, anchor: anchorOf(e.currentTarget) });
                    },
                    onContextMenu: (e: React.MouseEvent) => {
                      e.preventDefault();
                      e.stopPropagation();
                      openItemMenu(e.clientX, e.clientY, t.id, t.title);
                    },
                  };

                  return t.type === 'event' ? (
                    <div key={t.id} {...common} className={styles.event} style={{ background: color, boxShadow: ring }}>
                      {t.repeatOn && <RepeatIcon stroke="#FFFFFF" style={{ flex: '0 0 auto', opacity: 0.85 }} />}
                      <span className={base.ellipsis}>{t.title}</span>
                    </div>
                  ) : (
                    <div key={t.id} {...common} className={styles.task} style={{ boxShadow: ring }}>
                      <span className={styles.taskBar} style={{ background: color }} />
                      <span
                        className={`${styles.taskText} ${base.ellipsis}`}
                        style={{
                          textDecoration: t.done ? 'line-through' : 'none',
                          opacity: t.done ? 0.4 : 1,
                        }}
                      >
                        {t.title}
                      </span>
                    </div>
                  );
                })}
                {overflow > 0 && <div className={styles.overflow}>+{overflow} 更多</div>}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );

  function openCellMenu(clientX: number, clientY: number, key: string) {
    const { left, top } = clampMenu(clientX, clientY);
    dispatch({ type: 'openCtx', ctx: { kind: 'cell', left, top, key, title: fmt(key) } });
  }

  function openItemMenu(clientX: number, clientY: number, id: number, title: string) {
    const { left, top } = clampMenu(clientX, clientY);
    dispatch({
      type: 'openCtx',
      ctx: { kind: 'item', left, top, id, title: title.replace(/^\d{1,2}:\d{2}\s*/, '') },
    });
  }

  /** 菜单跟随鼠标，靠边自动收进面板内 */
  function clampMenu(clientX: number, clientY: number) {
    const { x, y } = toPanel(clientX, clientY);
    return { left: Math.min(x, PANEL_W - 158), top: Math.min(y, PANEL_H - 180) };
  }
}

function fmt(key: string): string {
  return key.slice(5).replace('-', '/');
}

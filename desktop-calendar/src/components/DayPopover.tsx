import { DAY_POPOVER_MAX_H, DAY_POPOVER_W, MONTH_RIGHT } from '../constants';
import { weekdayLabel } from '../lib/date';
import { stripTime, timeOf } from '../lib/item';
import { useCalendar, useDispatch } from '../store/context';
import { colorOf, itemsOn, sortForDay } from '../store/selectors';
import base from '../styles/base.module.css';
import { BellIcon, RepeatIcon } from './icons';
import styles from './DayPopover.module.css';

export function DayPopover() {
  const s = useCalendar();
  const dispatch = useDispatch();
  // 弹窗打开时让位给弹窗
  if (!s.dayOpen || s.modal) return null;

  const key = s.dayOpen;
  const items = itemsOn(s, key).slice().sort(sortForDay);
  const events = items.filter((t) => t.type === 'event').length;
  const tasks = items.filter((t) => t.type === 'task').length;

  const anchor = s.dayAnchor ?? { left: 340, top: 200, w: 0, h: 0 };
  // 默认贴在格子右侧 8px；越过月视图右边界就翻到左侧
  let left = anchor.left + anchor.w + 8;
  if (left + DAY_POPOVER_W > MONTH_RIGHT) left = anchor.left - DAY_POPOVER_W - 8;
  left = Math.max(8, Math.min(left, MONTH_RIGHT - DAY_POPOVER_W));

  const h = Math.min(DAY_POPOVER_MAX_H, 60 + items.length * 26 + (items.length ? 0 : 40));
  const top = Math.max(56, Math.min(anchor.top + anchor.h / 2 - h / 2, 588 - h));

  const [y, m, d] = key.split('-').map(Number);

  return (
    <div data-daypop="1" className={styles.card} style={{ left, top }}>
      <div className={styles.head}>
        <div className={styles.headText}>
          <span className={styles.date}>
            {y}年{m}月{d}日 {weekdayLabel(key)}
          </span>
          <span className={styles.count}>
            {items.length ? `${events} 个日程 · ${tasks} 项任务` : '暂无安排'}
          </span>
        </div>
        <div className={styles.headBtns}>
          <button
            type="button"
            title="新建日程"
            className={`${base.miniBtn} ${styles.addBtn}`}
            onClick={() => dispatch({ type: 'openCreate', date: key, kind: 'event' })}
          >
            +
          </button>
          <button
            type="button"
            title="关闭"
            className={`${base.miniBtn} ${styles.closeBtn}`}
            onClick={() => dispatch({ type: 'closeDay' })}
          >
            ×
          </button>
        </div>
      </div>

      <div className={base.divider} />

      {items.length === 0 ? (
        <div className={styles.empty}>这天还没有安排</div>
      ) : (
        <div className={styles.list}>
          {items.map((t) => {
            const color = colorOf(s, t.group);
            const time = timeOf(t);
            return (
              <div
                key={t.id}
                role="button"
                tabIndex={0}
                className={styles.row}
                style={{ opacity: t.done ? 0.45 : 1 }}
                onClick={() => dispatch({ type: 'openEdit', id: t.id })}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') dispatch({ type: 'openEdit', id: t.id });
                }}
              >
                {t.type === 'task' ? (
                  <button
                    type="button"
                    className={`${base.checkbox} ${styles.checkbox}`}
                    style={{
                      borderColor: t.done ? color : 'var(--c-muted)',
                      background: t.done ? color : 'transparent',
                    }}
                    onClick={(e) => {
                      e.stopPropagation();
                      dispatch({ type: 'toggleItemDone', id: t.id });
                    }}
                  >
                    {t.done ? '✓' : ''}
                  </button>
                ) : (
                  <span className={styles.swatch} style={{ background: color }} />
                )}
                <span
                  className={styles.time}
                  style={{ color: time === '00:00' ? 'var(--c-faint)' : 'var(--c-text)' }}
                >
                  {time}
                </span>
                <span
                  className={`${styles.title} ${base.ellipsis}`}
                  style={{ textDecoration: t.done ? 'line-through' : 'none' }}
                >
                  {stripTime(t.title)}
                </span>
                {t.repeatOn && <RepeatIcon size={10} stroke="var(--c-faint)" style={{ flex: '0 0 auto' }} />}
                {t.reminder && <BellIcon size={10} stroke="var(--c-faint)" style={{ flex: '0 0 auto' }} />}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

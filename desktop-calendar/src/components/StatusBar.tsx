import { todayIso } from '../lib/date';
import { useCalendar, useDispatch } from '../store/context';
import { colorOf, footerCounts, undatedItems } from '../store/selectors';
import base from '../styles/base.module.css';
import { ChevronUp } from './icons';
import styles from './StatusBar.module.css';

export function StatusBar() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const { events, todos } = footerCounts(s, todayIso());
  const undated = undatedItems(s);

  return (
    <div className={styles.bar}>
      <span>
        今日 · {events} 个日程 / {todos} 项待办
      </span>

      <div data-menu="1" style={{ display: 'flex' }}>
        <button type="button" className={styles.undatedBtn} onClick={() => dispatch({ type: 'toggleUndated' })}>
          <span>无期限 ({undated.length})</span>
          <span
            className={styles.arrow}
            style={{ transform: s.undatedOpen ? 'rotate(180deg)' : 'rotate(0deg)' }}
          >
            <ChevronUp stroke="currentColor" />
          </span>
        </button>
      </div>

      {s.undatedOpen && (
        <div className={styles.drawer} data-menu="1">
          {undated.map((t) => {
            const color = colorOf(s, t.group);
            return (
              <div key={t.id} className={styles.drawerRow}>
                <button
                  type="button"
                  className={`${base.checkbox} ${styles.drawerCheckbox}`}
                  style={{
                    borderColor: t.done ? color : 'var(--c-muted)',
                    background: t.done ? color : 'transparent',
                  }}
                  onClick={() => dispatch({ type: 'toggleItemDone', id: t.id })}
                >
                  {t.done ? '✓' : ''}
                </button>
                <span className={styles.drawerBar} style={{ background: color }} />
                <span
                  className={`${styles.drawerTitle} ${base.ellipsis}`}
                  style={{ textDecoration: t.done ? 'line-through' : 'none' }}
                >
                  {t.title}
                </span>
              </div>
            );
          })}
          {undated.length === 0 && (
            <div style={{ padding: '8px 6px', fontSize: 11, color: 'var(--c-faint)' }}>没有无期限条目</div>
          )}
        </div>
      )}
    </div>
  );
}

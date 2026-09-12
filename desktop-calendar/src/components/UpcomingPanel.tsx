import { URGENCY_SECTIONS } from '../constants';
import { diffDays, todayIso } from '../lib/date';
import { tint } from '../lib/color';
import { useCalendar, useDispatch } from '../store/context';
import { colorOf, hasConflict, isVisible } from '../store/selectors';
import base from '../styles/base.module.css';
import styles from './UpcomingPanel.module.css';

export function UpcomingPanel() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const today = todayIso();

  return (
    <div className={styles.wrap}>
      <div className={styles.heading}>未来 7 天</div>

      {URGENCY_SECTIONS.map((section) => {
        const rows = s.items
          .filter(
            (t) =>
              t.type === 'task' && t.date && isVisible(s, t) && section.test(diffDays(today, t.date)),
          )
          .slice()
          // 已完成的排在各组末尾，再按日期
          .sort((a, b) => (a.done ? 1 : 0) - (b.done ? 1 : 0) || a.date.localeCompare(b.date));

        const badgeBg = tint(section.color, 0.12);

        return (
          <div key={section.name} className={styles.section}>
            <div className={styles.sectionHead}>
              <span className={styles.sectionBar} style={{ background: section.color }} />
              <span className={styles.sectionName}>{section.name}</span>
              <span className={styles.count} style={{ color: section.color, background: badgeBg }}>
                {rows.length}
              </span>
            </div>

            {rows.map((t) => {
              const color = colorOf(s, t.group);
              const d = diffDays(today, t.date);
              const badge =
                d < 0 ? `逾期 ${-d} 天` : d === 0 ? '今天' : d === 1 ? '明天' : `${d} 天`;
              return (
                <div
                  key={t.id}
                  role="button"
                  tabIndex={0}
                  className={styles.row}
                  style={{
                    opacity: t.done ? 0.5 : 1,
                    // 晚于父任务截止日时左侧补红边
                    borderLeftColor: hasConflict(t) ? 'var(--c-danger)' : 'transparent',
                  }}
                  onClick={() => dispatch({ type: 'openEdit', id: t.id })}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') dispatch({ type: 'openEdit', id: t.id });
                  }}
                >
                  <button
                    type="button"
                    className={`${base.checkbox} ${styles.rowCheckbox}`}
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
                  <span className={styles.rowBar} style={{ background: color }} />
                  <div className={styles.rowMain}>
                    {t.parent && <div className={`${styles.rowParent} ${base.ellipsis}`}>{t.parent}</div>}
                    <div
                      className={`${styles.rowTitle} ${base.ellipsis}`}
                      style={{ textDecoration: t.done ? 'line-through' : 'none' }}
                    >
                      {t.title}
                    </div>
                  </div>
                  <span className={styles.badge} style={{ color: section.color, background: badgeBg }}>
                    {badge}
                  </span>
                </div>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}

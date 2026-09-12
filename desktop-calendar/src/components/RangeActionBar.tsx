import { daysBetween, fmtShort } from '../lib/date';
import { useCalendar, useDispatch } from '../store/context';
import { allGroups, groupColors } from '../store/selectors';
import base from '../styles/base.module.css';
import { ChevronDown } from './icons';
import styles from './RangeActionBar.module.css';

export function RangeActionBar() {
  const s = useCalendar();
  const dispatch = useDispatch();
  if (!s.selActive || !s.sel || s.modal) return null;

  const days = daysBetween(s.sel.start, s.sel.end);
  const inRange = s.items.filter((t) => t.date && days.includes(t.date));
  const colors = groupColors(s);

  return (
    <div data-daypop="1" className={styles.bar}>
      <div className={styles.info}>
        <span className={styles.label}>
          {fmtShort(days[0])} – {fmtShort(days[days.length - 1])}
        </span>
        <span className={styles.sub}>
          {days.length} 天 · 区间内 {inRange.length} 条
        </span>
      </div>

      <input
        type="text"
        placeholder="标题"
        className={`${base.textInput} ${styles.title}`}
        value={s.rangeTitle}
        onChange={(e) => dispatch({ type: 'setRangeTitle', value: e.target.value })}
      />

      <button
        type="button"
        className={`${base.btnPrimary} ${styles.action}`}
        onClick={() => dispatch({ type: 'rangeEachDay' })}
      >
        每天各一条
      </button>
      <button
        type="button"
        className={`${base.btnGhost} ${styles.action}`}
        onClick={() => dispatch({ type: 'rangeSpan' })}
      >
        跨天日程
      </button>

      <div className={styles.groupHost}>
        <button
          type="button"
          className={`${base.btnGhost} ${styles.groupBtn}`}
          onClick={() => dispatch({ type: 'toggleRangeGroups' })}
        >
          改分组
          <ChevronDown size={9} />
        </button>
        {s.rangeGroupsOpen && (
          <div className={`${base.popover} ${styles.groupPop}`}>
            {allGroups(s).map((name) => (
              <button
                key={name}
                type="button"
                className={base.popoverItem}
                onClick={() => dispatch({ type: 'rangeSetGroup', group: name })}
              >
                <span className={base.dot} style={{ background: colors[name] || 'var(--c-dot-empty)' }} />
                <span>{name}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      <button
        type="button"
        className={`${base.btnDanger} ${styles.action}`}
        onClick={() => dispatch({ type: 'rangeDelete' })}
      >
        删除 {inRange.length} 条
      </button>

      <button
        type="button"
        className={`${base.miniBtn} ${styles.close}`}
        onClick={() => dispatch({ type: 'selClear' })}
      >
        ×
      </button>
    </div>
  );
}

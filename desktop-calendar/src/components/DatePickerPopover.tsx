import { WEEKDAY_SHORT } from '../constants';
import { isDateKey, monthGrid, monthOf, todayIso, yearOf } from '../lib/date';
import { useDispatch } from '../store/context';
import base from '../styles/base.module.css';
import type { ModalDraft } from '../types';

/** 日期字段点开的内嵌月历 */
export function DatePickerPopover({ draft }: { draft: ModalDraft }) {
  const dispatch = useDispatch();
  const today = todayIso();
  const anchorDate = isDateKey(draft.date) ? draft.date : today;
  const py = draft.pickerYear ?? yearOf(anchorDate);
  const pm = draft.pickerMonth ?? monthOf(anchorDate);

  const patch = (p: Partial<ModalDraft>) => dispatch({ type: 'patchModal', patch: p });

  return (
    <div
      className={base.popover}
      style={{
        top: 32,
        left: 0,
        width: 232,
        borderRadius: 8,
        padding: 8,
        gap: 6,
        zIndex: 7,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button
          type="button"
          className={base.navBtn}
          style={{ width: 20, height: 20, fontSize: 13 }}
          onClick={() => patch(pm === 1 ? { pickerYear: py - 1, pickerMonth: 12 } : { pickerYear: py, pickerMonth: pm - 1 })}
        >
          ‹
        </button>
        <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--c-text)' }}>
          {py}年{pm}月
        </span>
        <button
          type="button"
          className={base.navBtn}
          style={{ width: 20, height: 20, fontSize: 13 }}
          onClick={() => patch(pm === 12 ? { pickerYear: py + 1, pickerMonth: 1 } : { pickerYear: py, pickerMonth: pm + 1 })}
        >
          ›
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 1 }}>
        {WEEKDAY_SHORT.map((w) => (
          <div
            key={w}
            style={{
              height: 18,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 10,
              color: 'var(--c-faint)',
            }}
          >
            {w}
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 1 }}>
        {monthGrid(py, pm).map((key) => {
          const inMonth = yearOf(key) === py && monthOf(key) === pm;
          const selected = key === draft.date;
          const isToday = key === today;
          return (
            <button
              key={key}
              type="button"
              onClick={() => patch({ date: key, pickerOpen: false })}
              style={{
                height: 26,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: 5,
                fontSize: 11,
                fontWeight: isToday ? 700 : 400,
                color: selected ? '#FFFFFF' : inMonth ? 'var(--c-text)' : 'var(--c-faint)',
                background: selected ? 'var(--c-accent)' : isToday ? 'var(--c-accent-soft)' : 'transparent',
                cursor: 'pointer',
                border: 0,
                fontFamily: 'inherit',
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {Number(key.slice(8, 10))}
            </button>
          );
        })}
      </div>

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderTop: '1px solid var(--c-border)',
          paddingTop: 6,
        }}
      >
        <button
          type="button"
          style={{ fontSize: 11, color: 'var(--c-muted)', cursor: 'pointer', background: 'none', border: 0, padding: 0, fontFamily: 'inherit' }}
          onClick={() => patch({ date: '', pickerOpen: false })}
        >
          清除
        </button>
        <button
          type="button"
          style={{ fontSize: 11, color: 'var(--c-accent)', cursor: 'pointer', background: 'none', border: 0, padding: 0, fontFamily: 'inherit' }}
          onClick={() =>
            patch({ date: today, pickerYear: yearOf(today), pickerMonth: monthOf(today), pickerOpen: false })
          }
        >
          今天
        </button>
      </div>
    </div>
  );
}

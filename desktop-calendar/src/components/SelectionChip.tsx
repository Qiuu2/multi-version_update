import { MONTH_RIGHT } from '../constants';
import { daysBetween, fmtShort } from '../lib/date';
import { useCalendar } from '../store/context';

/** 拖动过程中浮在选区上方的强调色小标签 */
export function SelectionChip() {
  const s = useCalendar();
  if (!s.sel || s.selActive) return null;

  const days = daysBetween(s.sel.start, s.sel.end);
  const a = s.selAnchor ?? { left: 300, top: 200, w: 0, h: 0 };
  const left = Math.max(6, Math.min(a.left + a.w / 2 - 52, MONTH_RIGHT - 110));
  const top = Math.max(56, a.top - 26);

  return (
    <div
      style={{
        position: 'absolute',
        left,
        top,
        zIndex: 11,
        pointerEvents: 'none',
        background: 'var(--c-accent)',
        borderRadius: 6,
        padding: '4px 9px',
        boxShadow: '0 6px 16px rgba(43,37,33,0.22)',
        display: 'flex',
        alignItems: 'center',
        gap: 7,
      }}
    >
      <span style={{ fontSize: 11, color: '#FFFFFF', whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' }}>
        {fmtShort(days[0])} – {fmtShort(days[days.length - 1])}
      </span>
      <span style={{ fontSize: 11, fontWeight: 600, color: '#FFFFFF', whiteSpace: 'nowrap' }}>
        {days.length} 天
      </span>
    </div>
  );
}

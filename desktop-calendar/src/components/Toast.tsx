import { useCalendar, useDispatch } from '../store/context';

/** 底部居中，深色底，带撤销链接，5 秒后自动消失 */
export function Toast() {
  const s = useCalendar();
  const dispatch = useDispatch();
  if (!s.toast) return null;

  return (
    <div
      style={{
        position: 'absolute',
        left: '50%',
        bottom: 44,
        transform: 'translateX(-50%)',
        zIndex: 12,
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        background: 'var(--c-toast)',
        borderRadius: 7,
        boxShadow: 'var(--shadow-toast)',
        padding: '9px 12px',
      }}
    >
      <span style={{ fontSize: 12, color: 'var(--c-weekend)', whiteSpace: 'nowrap' }}>{s.toast}</span>
      <button
        type="button"
        style={{
          fontSize: 12,
          fontWeight: 600,
          color: 'var(--c-accent-lite)',
          cursor: 'pointer',
          background: 'none',
          border: 0,
          padding: 0,
          flex: '0 0 auto',
          fontFamily: 'inherit',
        }}
        onClick={() => dispatch({ type: 'undoFromToast' })}
      >
        撤销
      </button>
      <button
        type="button"
        style={{
          fontSize: 13,
          lineHeight: 1,
          color: 'var(--c-muted)',
          cursor: 'pointer',
          background: 'none',
          border: 0,
          padding: 0,
          flex: '0 0 auto',
          fontFamily: 'inherit',
        }}
        onClick={() => dispatch({ type: 'dismissToast' })}
      >
        ×
      </button>
    </div>
  );
}

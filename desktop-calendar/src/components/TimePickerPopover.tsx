import { useDispatch } from '../store/context';
import base from '../styles/base.module.css';
import type { ModalDraft } from '../types';

const cellStyle = (active: boolean): React.CSSProperties => ({
  height: 24,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  borderRadius: 4,
  fontSize: 11,
  fontVariantNumeric: 'tabular-nums',
  color: active ? '#FFFFFF' : 'var(--c-text)',
  background: active ? 'var(--c-accent)' : 'transparent',
  cursor: 'pointer',
  flex: '0 0 auto',
  border: 0,
  fontFamily: 'inherit',
});

/** 双列时分选择器：左 00–23，右 00–55（5 分钟步进） */
export function TimePickerPopover({ draft }: { draft: ModalDraft }) {
  const dispatch = useDispatch();
  const cur = draft.time || '00:00';
  const patch = (p: Partial<ModalDraft>) => dispatch({ type: 'patchModal', patch: p });

  return (
    <div
      className={base.popover}
      style={{ top: 32, right: 0, width: 132, borderRadius: 8, padding: 5, gap: 4, zIndex: 7 }}
    >
      <div style={{ display: 'flex', gap: 4 }}>
        {(['时', '分'] as const).map((head) => {
          const isHour = head === '时';
          const options = isHour
            ? Array.from({ length: 24 }, (_, h) => String(h).padStart(2, '0'))
            : Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, '0'));
          const current = isHour ? cur.slice(0, 2) : cur.slice(3, 5);
          return (
            <div key={head} style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 2 }}>
              <div
                style={{
                  height: 16,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: 10,
                  color: 'var(--c-faint)',
                }}
              >
                {head}
              </div>
              <div style={{ maxHeight: 150, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 1 }}>
                {options.map((v) => (
                  <button
                    key={v}
                    type="button"
                    style={cellStyle(v === current)}
                    onClick={() =>
                      patch({ time: isHour ? `${v}:${cur.slice(3, 5)}` : `${cur.slice(0, 2)}:${v}` })
                    }
                  >
                    {v}
                  </button>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderTop: '1px solid var(--c-border)',
          paddingTop: 5,
        }}
      >
        <button
          type="button"
          style={{ fontSize: 11, color: 'var(--c-muted)', cursor: 'pointer', background: 'none', border: 0, padding: 0, fontFamily: 'inherit' }}
          onClick={() => patch({ time: '', timeOpen: false })}
        >
          不指定
        </button>
        <button
          type="button"
          style={{ fontSize: 11, color: 'var(--c-accent)', cursor: 'pointer', background: 'none', border: 0, padding: 0, fontFamily: 'inherit' }}
          onClick={() => patch({ timeOpen: false })}
        >
          完成
        </button>
      </div>
    </div>
  );
}

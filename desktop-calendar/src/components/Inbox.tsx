import { useState } from 'react';
import { useCalendar, useDispatch } from '../store/context';
import { colorOf, undatedItems } from '../store/selectors';
import base from '../styles/base.module.css';
import styles from './Inbox.module.css';

/**
 * 收集箱 —— 就是原来藏在底栏抽屉里的「无期限」条目，挪到右侧栏底部常驻。
 * 想到什么先敲进来，之后拖到月视图的某一天；反过来把日历上的条目拖回来，
 * 就是取消它的日期、先放一放。
 */
export function Inbox() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const [draft, setDraft] = useState('');
  const [over, setOver] = useState(false);
  const items = undatedItems(s);

  // 只有从日历拖过来的（已有日期的）条目才算有效落点
  const dragged = s.dragId == null ? null : s.items.find((t) => t.id === s.dragId);
  const canDrop = !!dragged?.date;

  return (
    <div
      className={styles.wrap}
      data-over={over && canDrop}
      onDragOver={(e) => {
        if (!canDrop) return;
        e.preventDefault();
        setOver(true);
      }}
      onDragLeave={() => setOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setOver(false);
        dispatch({ type: 'dropToInbox' });
      }}
    >
      <div className={styles.head}>
        <span className={styles.title}>收集箱</span>
        <span className={styles.count}>{items.length}</span>
        <span className={styles.hint}>拖到日期上安排</span>
      </div>

      <input
        type="text"
        placeholder="想到什么先记下来，回车新建"
        className={`${base.textInput} ${styles.input}`}
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={(e) => {
          if (e.key !== 'Enter') return;
          e.preventDefault();
          dispatch({ type: 'addInboxTask', title: draft });
          setDraft('');
        }}
      />

      {items.length === 0 ? (
        <div className={styles.empty}>
          还没有待安排的事。
          <br />
          也可以把日历上的条目拖进来，先放一放。
        </div>
      ) : (
        <div className={styles.list}>
          {items.map((t) => {
            const color = colorOf(s, t.group);
            return (
              <div
                key={t.id}
                className={styles.row}
                draggable
                onDragStart={() => dispatch({ type: 'dragStart', id: t.id })}
                onContextMenu={(e) => {
                  e.preventDefault();
                  dispatch({ type: 'openEdit', id: t.id });
                }}
              >
                <button
                  type="button"
                  className={`${base.checkbox} ${styles.checkbox}`}
                  style={{
                    borderColor: t.done ? color : 'var(--c-muted)',
                    background: t.done ? color : 'transparent',
                  }}
                  onClick={() => dispatch({ type: 'toggleItemDone', id: t.id })}
                >
                  {t.done ? '✓' : ''}
                </button>
                <span className={styles.bar} style={{ background: color }} />
                <span
                  className={`${styles.rowTitle} ${base.ellipsis}`}
                  style={{ textDecoration: t.done ? 'line-through' : 'none' }}
                  onDoubleClick={() => dispatch({ type: 'openEdit', id: t.id })}
                  title="拖到日期上安排 · 双击或右键编辑"
                >
                  {t.title}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

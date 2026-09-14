import { useState } from 'react';
import { isSubmitEnter } from '../lib/keys';
import { useCalendar, useDispatch } from '../store/context';
import { colorOf, undatedItems } from '../store/selectors';
import base from '../styles/base.module.css';
import { PlusIcon } from './icons';
import styles from './Inbox.module.css';

/**
 * 收集箱 —— 待安排的任务暂存在这里，之后拖到月视图的某一天。
 * 反过来把日历上的条目拖回来，就是取消它的日期、先放一放。
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

  const add = () => {
    if (!draft.trim()) return;
    dispatch({ type: 'addInboxTask', title: draft });
    setDraft('');
  };

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
      </div>

      <div className={styles.addRow}>
        <input
          type="text"
          placeholder="新建待安排的任务"
          className={`${base.textInput} ${styles.input}`}
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          // isSubmitEnter 会避开输入法确认候选词时的那次回车
          onKeyDown={(e) => {
            if (!isSubmitEnter(e)) return;
            e.preventDefault();
            add();
          }}
        />
        <button
          type="button"
          className={`${base.btnPrimary} ${styles.addBtn}`}
          title="新建（也可以直接回车）"
          disabled={!draft.trim()}
          onClick={add}
        >
          <PlusIcon size={13} />
        </button>
      </div>

      {items.length === 0 ? (
        <div className={styles.empty}>新建后拖到左边的日期上即可安排</div>
      ) : (
        <div className={styles.list}>
          {items.map((t) => {
            const color = colorOf(s, t.group);
            return (
              <div
                key={t.id}
                className={styles.row}
                draggable
                title="拖到日期上安排 · 双击编辑"
                onDragStart={() => dispatch({ type: 'dragStart', id: t.id })}
                onDoubleClick={() => dispatch({ type: 'openEdit', id: t.id })}
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

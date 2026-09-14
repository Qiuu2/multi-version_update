import { useCalendar, useDispatch } from '../store/context';
import { archivedGroups } from '../store/selectors';
import base from '../styles/base.module.css';
import styles from './ArchivedPanel.module.css';

/**
 * 已归档分组。做完的项目从图例移走，条目也不再出现在月视图和右侧栏，
 * 但数据一条没删 —— 这里随时能恢复，搜索也照样搜得到。
 */
export function ArchivedPanel() {
  const s = useCalendar();
  const dispatch = useDispatch();
  if (!s.archivedOpen) return null;

  const groups = archivedGroups(s);

  return (
    <div className={styles.pop} data-menu="1" data-daypop="1">
      <div className={styles.head}>
        <span className={styles.title}>已归档分组</span>
        <button
          type="button"
          className={base.miniBtn}
          style={{ fontSize: 14 }}
          onClick={() => dispatch({ type: 'toggleArchivedPanel' })}
        >
          ×
        </button>
      </div>

      {groups.length === 0 ? (
        <div className={styles.empty}>
          还没有归档的分组。
          <br />
          项目做完后，右键它的图例 →「归档」。
        </div>
      ) : (
        <>
          <div className={styles.list}>
            {groups.map((g) => {
              const n = s.items.filter((t) => t.group === g.name).length;
              return (
                <div key={g.name} className={styles.row}>
                  <span className={base.dot} style={{ background: g.color }} />
                  <span className={`${styles.name} ${base.ellipsis}`}>{g.name}</span>
                  <span className={styles.count}>{n} 项</span>
                  <button
                    type="button"
                    className={`${base.btnGhost} ${styles.restore}`}
                    onClick={() => dispatch({ type: 'unarchiveGroup', name: g.name })}
                  >
                    恢复
                  </button>
                </div>
              );
            })}
          </div>
          <span className={styles.note}>归档只是收起，条目一条没删，搜索仍能搜到。</span>
        </>
      )}
    </div>
  );
}

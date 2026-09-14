import { NEW_GROUP_COLORS, PANEL_W } from '../constants';
import { isSubmitEnter } from '../lib/keys';
import { useCalendar, useDispatch } from '../store/context';
import base from '../styles/base.module.css';
import styles from './GroupEditor.module.css';

const POP_W = 210;

/**
 * 分组（项目）的新建与编辑。
 * 顶栏图例的「+」打开新建，右键某个分组打开编辑 ——
 * 改名、换色、只看这组、删除都在这一个浮层里，不用翻多级菜单。
 */
export function GroupEditor() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const ed = s.groupEditor;
  if (!ed) return null;

  const isEdit = ed.mode === 'edit';
  const itemCount = isEdit ? s.items.filter((t) => t.group === ed.original).length : 0;
  const others = s.groups.filter((g) => g.name !== ed.original);
  const moveTarget = ed.moveTarget || others[0]?.name || '';
  // 浮层锚在触发它的图例项下方，靠边时收进面板内
  const left = Math.max(8, Math.min(ed.left - POP_W / 2, PANEL_W - POP_W - 8));

  return (
    <div data-daypop="1" data-menu="1" className={styles.pop} style={{ left }}>
      <div className={styles.head}>
        <span className={styles.title}>{isEdit ? '编辑分组' : '新建分组'}</span>
        <button
          type="button"
          className={base.miniBtn}
          style={{ fontSize: 14 }}
          onClick={() => dispatch({ type: 'closeGroupEditor' })}
        >
          ×
        </button>
      </div>

      {!ed.confirmDelete && (
      <input
        type="text"
        autoFocus
        placeholder="分组名称"
        className={`${base.textInput} ${styles.nameInput}`}
        value={ed.name}
        onChange={(e) => dispatch({ type: 'patchGroupEditor', patch: { name: e.target.value } })}
        onKeyDown={(e) => {
          if (isSubmitEnter(e)) {
            e.preventDefault();
            dispatch({ type: 'commitGroupEditor' });
          }
        }}
      />
      )}

      {!ed.confirmDelete && <span className={styles.label}>颜色</span>}
      {!ed.confirmDelete && (
      <div className={styles.swatches}>
        {NEW_GROUP_COLORS.map((c) => (
          <button
            key={c}
            type="button"
            className={styles.swatch}
            style={{
              background: c,
              boxShadow:
                ed.color === c
                  ? '0 0 0 2px var(--c-surface), 0 0 0 3.5px var(--c-text)'
                  : '0 0 0 1px var(--c-border)',
            }}
            onClick={() => dispatch({ type: 'patchGroupEditor', patch: { color: c } })}
          />
        ))}
      </div>
      )}

      {!ed.confirmDelete && (
      <div className={styles.actions}>
        {isEdit && (
          <button
            type="button"
            className={`${base.btnGhost} ${styles.soloBtn}`}
            title="只显示这个分组，再点一次恢复全部"
            onClick={() => dispatch({ type: 'soloGroup', name: ed.original })}
          >
            只看这组
          </button>
        )}
        <div className={styles.right}>
          <button
            type="button"
            className={`${base.btnPrimary} ${styles.saveBtn}`}
            onClick={() => dispatch({ type: 'commitGroupEditor' })}
          >
            {isEdit ? '保存' : '添加'}
          </button>
        </div>
      </div>
      )}

      {isEdit && !ed.confirmDelete && (
        <div className={styles.deleteRow}>
          <button
            type="button"
            className={base.linkDanger}
            style={{ fontSize: 11 }}
            onClick={() => {
              // 空分组没什么可处置的，直接删，不必多问一步
              if (!itemCount) {
                dispatch({ type: 'deleteGroupByName', name: ed.original, mode: 'orphan' });
              } else {
                dispatch({ type: 'patchGroupEditor', patch: { confirmDelete: true } });
              }
            }}
          >
            删除这个分组
          </button>
          <span className={styles.hint}>{itemCount ? `${itemCount} 项` : '暂无条目'}</span>
        </div>
      )}

      {isEdit && ed.confirmDelete && (
        <div className={styles.confirm}>
          <span className={styles.confirmTitle}>
            删除「{ed.original}」，这 {itemCount} 项怎么处理？
          </span>

          <button
            type="button"
            className={styles.choice}
            onClick={() =>
              dispatch({ type: 'deleteGroupByName', name: ed.original, mode: 'orphan' })
            }
          >
            转为未分组
          </button>

          {others.length > 0 && (
            <div className={styles.moveRow}>
              <button
                type="button"
                className={styles.choice}
                style={{ flex: 1 }}
                onClick={() =>
                  dispatch({
                    type: 'deleteGroupByName',
                    name: ed.original,
                    mode: 'move',
                    moveTo: moveTarget,
                  })
                }
              >
                移到
              </button>
              <select
                className={styles.moveSelect}
                value={moveTarget}
                onChange={(e) =>
                  dispatch({ type: 'patchGroupEditor', patch: { moveTarget: e.target.value } })
                }
              >
                {others.map((g) => (
                  <option key={g.name} value={g.name}>
                    {g.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          <button
            type="button"
            className={`${styles.choice} ${styles.choiceDanger}`}
            onClick={() =>
              dispatch({ type: 'deleteGroupByName', name: ed.original, mode: 'purge' })
            }
          >
            连同这 {itemCount} 项一起删除
          </button>

          <button
            type="button"
            className={styles.cancelChoice}
            onClick={() => dispatch({ type: 'patchGroupEditor', patch: { confirmDelete: false } })}
          >
            取消
          </button>
        </div>
      )}
    </div>
  );
}

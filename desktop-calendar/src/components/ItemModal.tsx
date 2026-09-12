import { NEW_GROUP_COLORS, REMINDER_OPTIONS } from '../constants';
import { useCalendar, useDispatch } from '../store/context';
import { allGroups, groupColors } from '../store/selectors';
import base from '../styles/base.module.css';
import type { ModalDraft } from '../types';
import { DatePickerPopover } from './DatePickerPopover';
import { TimePickerPopover } from './TimePickerPopover';
import { BellIcon, CalendarIcon, ChevronDown, ClockIcon } from './icons';
import styles from './ItemModal.module.css';

export function ItemModal() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const m = s.modal;
  if (!m) return null;

  const colors = groupColors(s);
  const patch = (p: Partial<ModalDraft>) => dispatch({ type: 'patchModal', patch: p });
  /** 四个浮层互斥：开一个就关掉其余三个 */
  const openOnly = (which: 'groupListOpen' | 'pickerOpen' | 'timeOpen' | 'reminderOpen') =>
    patch({
      groupListOpen: which === 'groupListOpen' ? !m.groupListOpen : false,
      pickerOpen: which === 'pickerOpen' ? !m.pickerOpen : false,
      timeOpen: which === 'timeOpen' ? !m.timeOpen : false,
      reminderOpen: which === 'reminderOpen' ? !m.reminderOpen : false,
      ...(which === 'pickerOpen' ? { pickerYear: null, pickerMonth: null } : null),
    });

  const heading =
    m.kind === 'event'
      ? m.mode === 'create'
        ? '新建日程'
        : '编辑日程'
      : m.mode === 'create'
        ? '新建任务'
        : '编辑任务';

  return (
    <div className={styles.overlay} onClick={() => dispatch({ type: 'closeModal' })}>
      <div className={styles.dialog} onClick={(e) => e.stopPropagation()}>
        <div className={styles.head}>
          <span className={styles.heading}>{heading}</span>
          <button
            type="button"
            className={`${base.miniBtn} ${styles.closeBtn}`}
            onClick={() => dispatch({ type: 'closeModal' })}
          >
            ×
          </button>
        </div>

        <input
          type="text"
          placeholder={m.kind === 'event' ? '日程标题' : '任务标题'}
          className={`${base.textInput} ${styles.titleInput}`}
          value={m.title}
          onChange={(e) => patch({ title: e.target.value })}
          autoFocus
        />

        {/* 所属日历 + 全天开关 */}
        <div className={styles.row}>
          <div className={styles.field}>
            <span className={styles.fieldLabel}>所属日历</span>
            <button
              type="button"
              className={styles.select}
              data-open={m.groupListOpen}
              onClick={() => openOnly('groupListOpen')}
            >
              <span className={base.dot} style={{ background: colors[m.group] || 'var(--c-dot-empty)' }} />
              <span
                className={styles.selectText}
                style={{ color: m.group ? 'var(--c-text)' : 'var(--c-muted)' }}
              >
                {m.group || '未分组'}
              </span>
              <ChevronDown />
            </button>

            {m.groupListOpen && (
              <div className={`${base.popover} ${styles.groupPop}`}>
                {['未分组', ...allGroups(s)].map((label) => (
                  <button
                    key={label}
                    type="button"
                    className={base.popoverItem}
                    onClick={() => patch({ group: label === '未分组' ? '' : label, groupListOpen: false })}
                  >
                    <span className={base.dot} style={{ background: colors[label] || 'var(--c-dot-empty)' }} />
                    <span>{label}</span>
                  </button>
                ))}

                <div className={base.divider} style={{ margin: '3px 0' }} />

                {/* 新增分组：名称 + 8 个配色 + 添加 */}
                <div className={styles.newGroup}>
                  <input
                    type="text"
                    placeholder="新分组名称…"
                    className={`${base.textInput} ${styles.newGroupInput}`}
                    value={s.newGroupName}
                    onChange={(e) => dispatch({ type: 'setNewGroupName', value: e.target.value })}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        e.stopPropagation();
                        dispatch({ type: 'addGroup' });
                      }
                    }}
                    onClick={(e) => e.stopPropagation()}
                  />
                  <div className={styles.swatchRow}>
                    <div className={styles.swatches}>
                      {NEW_GROUP_COLORS.map((v) => (
                        <button
                          key={v}
                          type="button"
                          className={styles.swatch}
                          style={{
                            background: v,
                            boxShadow:
                              s.newGroupColor === v
                                ? '0 0 0 2px var(--c-surface), 0 0 0 3.5px var(--c-text)'
                                : '0 0 0 1px var(--c-border)',
                          }}
                          onClick={() => dispatch({ type: 'setNewGroupColor', value: v })}
                        />
                      ))}
                    </div>
                    <button
                      type="button"
                      className={`${base.btnPrimary} ${styles.addGroupBtn}`}
                      onClick={() => dispatch({ type: 'addGroup' })}
                    >
                      添加
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className={styles.allDay}>
            <span className={styles.allDayLabel}>全天</span>
            <button
              type="button"
              className={base.pillToggle}
              data-on={!m.withTime}
              onClick={() => patch({ withTime: !m.withTime, timeOpen: false })}
            >
              <span />
            </button>
          </div>
        </div>

        {/* 日期 + 时间；全天开启时整行压到 0.45 */}
        <div className={styles.field} style={{ flex: '0 0 auto' }}>
          <span className={styles.fieldLabel}>时间</span>
          <div className={styles.timeRow} style={{ opacity: m.withTime ? 1 : 0.45 }}>
            <button
              type="button"
              className={`${styles.select} ${styles.dateField}`}
              data-open={m.pickerOpen}
              onClick={() => openOnly('pickerOpen')}
            >
              <CalendarIcon style={{ flex: '0 0 auto' }} />
              <span
                className={styles.selectText}
                style={{ color: m.date ? 'var(--c-text)' : 'var(--c-faint)' }}
              >
                {m.date ? m.date.replace(/-/g, '/') : '选择日期'}
              </span>
            </button>
            {m.pickerOpen && <DatePickerPopover draft={m} />}

            <div className={styles.timeField}>
              <button
                type="button"
                className={`${styles.select} ${styles.timeBtn}`}
                data-open={m.timeOpen}
                onClick={() => openOnly('timeOpen')}
              >
                <ClockIcon style={{ flex: '0 0 auto' }} />
                <span
                  className={styles.selectText}
                  style={{ color: m.time ? 'var(--c-text)' : 'var(--c-faint)' }}
                >
                  {m.time || '00:00'}
                </span>
              </button>
              {m.timeOpen && <TimePickerPopover draft={m} />}
            </div>
          </div>
        </div>

        {/* 地点 + 提醒 */}
        <div style={{ display: 'flex', gap: 10, flex: '0 0 auto' }}>
          <div className={styles.field}>
            <span className={styles.fieldLabel}>地点</span>
            <input
              type="text"
              placeholder="添加地点"
              className={`${base.textInput} ${styles.locationInput}`}
              value={m.location}
              onChange={(e) => patch({ location: e.target.value })}
            />
          </div>

          <div className={styles.reminderField}>
            <span className={styles.fieldLabel}>提醒</span>
            <button
              type="button"
              className={styles.select}
              data-open={m.reminderOpen}
              onClick={() => openOnly('reminderOpen')}
            >
              <BellIcon style={{ flex: '0 0 auto' }} />
              <span
                className={`${styles.selectText} ${base.ellipsis}`}
                style={{ color: m.reminder ? 'var(--c-text)' : 'var(--c-faint)' }}
              >
                {m.reminder || '不提醒'}
              </span>
              <ChevronDown />
            </button>
            {m.reminderOpen && (
              <div
                className={base.popover}
                style={{ top: 54, right: 0, width: 132, borderRadius: 8, zIndex: 8 }}
              >
                {REMINDER_OPTIONS.map((label) => {
                  const active = (m.reminder || '不提醒') === label;
                  return (
                    <button
                      key={label}
                      type="button"
                      className={base.popoverItem}
                      style={{
                        height: 24,
                        padding: '0 8px',
                        fontSize: 11,
                        color: active ? '#FFFFFF' : 'var(--c-text)',
                        background: active ? 'var(--c-accent)' : 'transparent',
                      }}
                      onClick={() => patch({ reminder: label === '不提醒' ? '' : label, reminderOpen: false })}
                    >
                      {label}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        <div className={styles.field} style={{ flex: '0 0 auto' }}>
          <span className={styles.fieldLabel}>备注</span>
          <textarea
            placeholder="添加备注"
            className={`${base.textInput} ${styles.notes}`}
            value={m.notes}
            onChange={(e) => patch({ notes: e.target.value })}
          />
        </div>

        <div className={base.divider} />

        <div className={styles.repeatBlock}>
          <div className={styles.repeatRow}>
            <button
              type="button"
              className={base.pillToggle}
              data-on={m.repeatOn}
              onClick={() => patch({ repeatOn: !m.repeatOn })}
            >
              <span />
            </button>
            <span className={styles.repeatLabel}>每周重复</span>
            <div className={styles.weeksBox}>
              <span>共</span>
              <input
                type="text"
                className={`${base.textInput} ${styles.weeksInput}`}
                value={m.repeatWeeks}
                onChange={(e) => patch({ repeatWeeks: e.target.value.replace(/[^0-9]/g, '').slice(0, 3) })}
              />
              <span>周</span>
            </div>
          </div>
          <span className={styles.repeatNote}>
            {m.repeatOn
              ? `将生成 ${m.repeatWeeks || '0'} 条独立日程，可整组删除`
              : '仅这一条日程'}
          </span>
        </div>

        <div className={styles.foot}>
          <div className={styles.footLeft}>
            {m.mode === 'edit' && (
              <button type="button" className={base.linkDanger} onClick={() => dispatch({ type: 'deleteCurrent' })}>
                删除本条
              </button>
            )}
            {m.mode === 'edit' && m.repeatOn && (
              <button
                type="button"
                className={base.linkDanger}
                onClick={() => dispatch({ type: 'deleteRepeatGroup' })}
              >
                删除整组
              </button>
            )}
          </div>
          <div className={styles.footRight}>
            <button
              type="button"
              className={`${base.btnGhost} ${styles.cancelBtn}`}
              onClick={() => dispatch({ type: 'closeModal' })}
            >
              取消
            </button>
            <button
              type="button"
              className={`${base.btnPrimary} ${styles.saveBtn}`}
              onClick={() => dispatch({ type: 'saveModal' })}
            >
              {m.mode === 'create' ? '创建' : '保存'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

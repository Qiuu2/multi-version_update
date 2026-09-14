import { NEW_GROUP_COLORS, SCALE_STEPS, THEME_OPTIONS } from '../constants';
import { fmtShort, monthOf, todayIso, yearOf } from '../lib/date';
import { stripTime } from '../lib/item';
import { isSubmitEnter } from '../lib/keys';
import { hideToTray, isTauri, setAutostart, snapCorner } from '../lib/platform';
import { usePanel } from './PanelContext';
import { useCalendar, useDispatch } from '../store/context';
import { allGroups, colorOf, groupColors, searchMatches } from '../store/selectors';
import base from '../styles/base.module.css';
import { GearIcon, PlusIcon, SearchIcon, ThemeIcon, UndoIcon } from './icons';
import styles from './TopBar.module.css';

export function TopBar() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const { anchorOf } = usePanel();
  const colors = groupColors(s);
  const matches = searchMatches(s);
  // 只有确实存在未分组条目时才占图例的位置
  const hasUngrouped = s.items.some((t) => !t.group);
  const searchOpen = s.searchFocused && !!s.search.trim();

  return (
    <div className={styles.bar} data-tauri-drag-region>
      <div className={styles.left} data-tauri-drag-region>
        <button type="button" className={base.navBtn} title="上一月" onClick={() => dispatch({ type: 'prevMonth' })}>
          ‹
        </button>
        <div className={styles.monthLabel}>
          {s.year}年{s.month}月
        </div>
        <button type="button" className={base.navBtn} title="下一月" onClick={() => dispatch({ type: 'nextMonth' })}>
          ›
        </button>
        <button type="button" className={styles.today} onClick={() => dispatch({ type: 'goToday' })}>
          今天
        </button>
      </div>

      {/* 点色点切换该分组在月视图与右侧栏的显隐；隐藏时色点变空心 */}
      <div className={styles.legend} data-tauri-drag-region>
        {allGroups(s).map((name) => {
          const off = !!s.hidden[name];
          return (
            <button
              key={name}
              type="button"
              className={styles.legendItem}
              title={`${name} · 点击切换显隐，右键编辑分组`}
              onClick={() => dispatch({ type: 'toggleGroupHidden', name })}
              onContextMenu={(e) => {
                e.preventDefault();
                e.stopPropagation();
                const a = anchorOf(e.currentTarget);
                dispatch({
                  type: 'openGroupEditor',
                  editor: {
                    mode: 'edit',
                    original: name,
                    name,
                    color: colors[name],
                    left: a ? a.left + a.w / 2 : 480,
                    confirmDelete: false,
                    moveTarget: '',
                  },
                });
              }}
            >
              <span
                className={base.dot}
                style={{
                  background: off ? 'transparent' : colors[name],
                  border: off ? `1.5px solid ${colors[name]}` : 'none',
                }}
              />
              <span
                className={styles.legendText}
                style={{ color: off ? 'var(--c-faint)' : 'var(--c-text)' }}
              >
                {name}
              </span>
            </button>
          );
        })}
        {hasUngrouped && (
          <button
            type="button"
            className={styles.legendItem}
            title="未分组 · 点击切换显隐"
            onClick={() => dispatch({ type: 'toggleGroupHidden', name: '' })}
          >
            <span
              className={base.dot}
              style={{
                background: s.hidden[''] ? 'transparent' : 'var(--c-dot-empty)',
                border: s.hidden[''] ? '1.5px solid var(--c-dot-empty)' : 'none',
              }}
            />
            <span
              className={styles.legendText}
              style={{ color: s.hidden[''] ? 'var(--c-faint)' : 'var(--c-muted)' }}
            >
              未分组
            </span>
          </button>
        )}
        <button
          type="button"
          className={styles.legendAdd}
          title="新建分组"
          onClick={(e) => {
            const a = anchorOf(e.currentTarget);
            dispatch({
              type: 'openGroupEditor',
              editor: {
                mode: 'create',
                original: '',
                name: '',
                color: NEW_GROUP_COLORS[s.groups.length % NEW_GROUP_COLORS.length],
                left: a ? a.left + a.w / 2 : 480,
                confirmDelete: false,
                moveTarget: '',
              },
            });
          }}
        >
          +
        </button>
      </div>

      <div className={styles.right}>
        <div className={styles.searchWrap}>
          <SearchIcon className={styles.searchIcon} />
          <input
            type="text"
            placeholder="搜索日程与任务"
            className={`${base.textInput} ${styles.searchInput}`}
            data-focused={s.searchFocused}
            value={s.search}
            onChange={(e) => dispatch({ type: 'setSearch', value: e.target.value })}
            onFocus={() => dispatch({ type: 'setSearchFocused', value: true })}
            // 结果项用 mousedown 阻止冒泡，blur 仍会在点击后触发，延后一拍让 click 先跑
            onBlur={() => window.setTimeout(() => dispatch({ type: 'setSearchFocused', value: false }), 120)}
            onKeyDown={(e) => {
              if (isSubmitEnter(e) && matches.length) openResult(matches[0].id, matches[0].date);
              else if (e.key === 'Escape') dispatch({ type: 'setSearch', value: '' });
            }}
          />
          {searchOpen && (
            <div className={`${base.popover} ${styles.searchPop}`} data-menu="1">
              {matches.slice(0, 8).map((t, i) => (
                <button
                  key={t.id}
                  type="button"
                  className={`${base.popoverItem} ${styles.searchRow}`}
                  style={{ background: i === 0 ? 'var(--c-bg)' : 'transparent' }}
                  onClick={() => openResult(t.id, t.date)}
                >
                  <span className={base.dot} style={{ background: colorOf(s, t.group) }} />
                  <span className={`${styles.searchTitle} ${base.ellipsis}`}>{stripTime(t.title)}</span>
                  <span className={`${styles.searchDate} ${base.tnum}`}>
                    {t.date ? fmtShort(t.date) : '无期限'}
                  </span>
                </button>
              ))}
              {matches.length === 0 && <div className={styles.searchEmpty}>没有匹配的日程或任务</div>}
              {matches.length > 0 && <div className={styles.searchFoot}>回车打开第一条</div>}
            </div>
          )}
        </div>

        <button
          type="button"
          title="新建任务"
          className={`${base.btnPrimary} ${styles.newBtn}`}
          onClick={() => dispatch({ type: 'openCreate', date: todayIso(), kind: 'task' })}
        >
          <PlusIcon />
        </button>

        <div className={styles.menuHost} data-menu="1">
          <button
            type="button"
            title="配色风格"
            className={base.iconBtn}
            data-active={s.themeMenuOpen}
            onClick={() => dispatch({ type: 'toggleThemeMenu' })}
          >
            <ThemeIcon />
          </button>
          {s.themeMenuOpen && (
            <div className={`${base.popover} ${styles.themePop}`}>
              {THEME_OPTIONS.map((o) => (
                <button
                  key={o.key}
                  type="button"
                  className={base.popoverItem}
                  style={{ background: s.theme === o.key ? 'var(--c-accent-soft)' : 'transparent' }}
                  onClick={() => dispatch({ type: 'setTheme', theme: o.key })}
                >
                  <span className={styles.themeSwatch} style={{ background: o.swatch }} />
                  <span style={{ flex: 1 }}>{o.label}</span>
                  {s.theme === o.key && <span style={{ color: 'var(--c-accent)', fontSize: 11 }}>✓</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        <button
          type="button"
          title="撤销"
          className={base.iconBtn}
          style={{ opacity: s.history.length ? 1 : 0.35 }}
          onClick={() => dispatch({ type: 'undo' })}
        >
          <UndoIcon />
        </button>

        <div data-menu="1" style={{ display: 'flex' }}>
          <button
            type="button"
            title="设置"
            className={base.iconBtn}
            data-active={s.settingsOpen}
            onClick={() => dispatch({ type: 'toggleSettings' })}
          >
            <GearIcon />
          </button>
        </div>

        <button
          type="button"
          className={`${base.iconBtn} ${styles.closeBtn}`}
          title={isTauri() ? '收进托盘' : '关闭'}
          onClick={() => {
            void hideToTray().then((hidden: boolean) => {
              if (!hidden) dispatch({ type: 'closePanel' });
            });
          }}
        >
          ×
        </button>
      </div>

      {s.settingsOpen && (
        <div className={styles.settingsPop} data-menu="1">
          <button type="button" className={styles.settingsRow} onClick={() => dispatch({ type: 'toggleShowDone' })}>
            <span>显示已完成</span>
            <span className={styles.mark}>{s.showDone ? '✓' : ''}</span>
          </button>
          <button type="button" className={styles.settingsRow} onClick={() => dispatch({ type: 'toggleShowOther' })}>
            <span>显示非本月日期</span>
            <span className={styles.mark}>{s.showOther ? '✓' : ''}</span>
          </button>
          {isTauri() && (
            <button
              type="button"
              className={styles.settingsRow}
              title="贴在桌面上，压在其他窗口之下"
              onClick={() => dispatch({ type: 'setDesktopMode', value: !s.desktopMode })}
            >
              <span>桌面模式</span>
              <span className={styles.mark}>{s.desktopMode ? '✓' : ''}</span>
            </button>
          )}

          {isTauri() && (
            <button
              type="button"
              className={styles.settingsRow}
              title="开机后自动运行"
              onClick={() => {
                const next = !s.autostart;
                dispatch({ type: 'setAutostart', value: next });
                void setAutostart(next);
              }}
            >
              <span>开机自启</span>
              <span className={styles.mark}>{s.autostart ? '✓' : ''}</span>
            </button>
          )}

          <button
            type="button"
            className={styles.settingsRow}
            onClick={() => dispatch({ type: 'setGuideOpen', value: true })}
          >
            <span>使用说明</span>
            <span className={styles.mark}>›</span>
          </button>

          <div className={base.divider} style={{ margin: '4px 0' }} />

          {/* 缩放：整块面板等比缩放，窗口大小跟着变 */}
          <div className={styles.settingsRow} style={{ cursor: 'default' }}>
            <span>面板大小</span>
            <span className={styles.stepper}>
              <button
                type="button"
                className={styles.stepBtn}
                disabled={s.scale <= SCALE_STEPS[0]}
                onClick={() => dispatch({ type: 'stepScale', delta: -1 })}
              >
                −
              </button>
              <span className={`${styles.scaleValue} ${base.tnum}`}>{Math.round(s.scale * 100)}%</span>
              <button
                type="button"
                className={styles.stepBtn}
                disabled={s.scale >= SCALE_STEPS[SCALE_STEPS.length - 1]}
                onClick={() => dispatch({ type: 'stepScale', delta: 1 })}
              >
                +
              </button>
            </span>
          </div>

          {isTauri() && (
            <div className={styles.settingsRow} style={{ cursor: 'default' }}>
              <span>吸附到</span>
              <span className={styles.corners}>
                {([
                  ['tl', '左上'],
                  ['tr', '右上'],
                  ['bl', '左下'],
                  ['br', '右下'],
                ] as const).map(([key, label]) => (
                  <button
                    key={key}
                    type="button"
                    className={styles.cornerBtn}
                    onClick={() => void snapCorner(key)}
                  >
                    {label}
                  </button>
                ))}
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  );

  /** 点搜索结果：跳到它所在月份，再打开编辑弹窗 */
  function openResult(id: number, date: string) {
    dispatch({ type: 'setSearch', value: '' });
    dispatch({ type: 'setSearchFocused', value: false });
    dispatch({ type: 'closeDay' });
    if (date) dispatch({ type: 'gotoMonth', year: yearOf(date), month: monthOf(date) });
    dispatch({ type: 'openEdit', id });
  }
}

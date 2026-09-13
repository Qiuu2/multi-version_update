import { useCallback, useMemo, useRef } from 'react';
import { PANEL_H, PANEL_W } from '../constants';
import { useGlobalKeys } from '../hooks/useGlobalKeys';
import { usePanelDismiss } from '../hooks/usePanelDismiss';
import { useToastTimer } from '../hooks/useToastTimer';
import { useCalendar, useDispatch } from '../store/context';
import base from '../styles/base.module.css';
import { ContextMenu } from './ContextMenu';
import { DayPopover } from './DayPopover';
import { GroupEditor } from './GroupEditor';
import { Guide } from './Guide';
import { ItemModal } from './ItemModal';
import { MonthGrid } from './MonthGrid';
import { PanelContext } from './PanelContext';
import { RangeActionBar } from './RangeActionBar';
import { SelectionChip } from './SelectionChip';
import { StatusBar } from './StatusBar';
import { Toast } from './Toast';
import { TopBar } from './TopBar';
import { UpcomingPanel } from './UpcomingPanel';
import styles from './CalendarPanel.module.css';

export function CalendarPanel() {
  const s = useCalendar();
  const dispatch = useDispatch();
  const rootRef = useRef<HTMLDivElement>(null);
  const pointerIn = useRef(false);

  useGlobalKeys(rootRef, pointerIn);
  usePanelDismiss();
  useToastTimer();

  // 面板被 transform 缩放后，getBoundingClientRect 给的是缩放后的值，
  // 而浮层用的是面板内部的 CSS 像素坐标，所以这里统一除回去。
  const anchorOf = useCallback((el: Element | null) => {
    const root = rootRef.current;
    if (!root || !el) return null;
    const rr = root.getBoundingClientRect();
    const k = rr.width / PANEL_W || 1;
    const r = el.getBoundingClientRect();
    return {
      left: (r.left - rr.left) / k,
      top: (r.top - rr.top) / k,
      w: r.width / k,
      h: r.height / k,
    };
  }, []);

  const toPanel = useCallback((clientX: number, clientY: number) => {
    const root = rootRef.current;
    if (!root) return { x: clientX, y: clientY };
    const rr = root.getBoundingClientRect();
    const k = rr.width / PANEL_W || 1;
    return { x: (clientX - rr.left) / k, y: (clientY - rr.top) / k };
  }, []);

  const api = useMemo(() => ({ anchorOf, toPanel }), [anchorOf, toPanel]);

  return (
    <PanelContext.Provider value={api}>
      {/* 视口占掉缩放后的实际尺寸，面板本体在里面按 scale 绘制 */}
      <div
        className={styles.viewport}
        style={{ width: PANEL_W * s.scale, height: PANEL_H * s.scale }}
      >
      <div
        ref={rootRef}
        data-cal-theme={s.theme}
        className={styles.panel}
        style={{ transform: `scale(${s.scale})`, transformOrigin: 'top left' }}
        onMouseEnter={() => {
          pointerIn.current = true;
        }}
        onMouseLeave={() => {
          pointerIn.current = false;
        }}
      >
        {s.panelClosed ? (
          <div className={styles.closed}>
            <span className={styles.closedHint}>面板已关闭</span>
            <button
              type="button"
              className={`${base.btnPrimary} ${styles.reopen}`}
              onClick={() => dispatch({ type: 'reopenPanel' })}
            >
              重新打开
            </button>
          </div>
        ) : (
          <>
            <div className={styles.inner}>
              <TopBar />
              <div className={styles.body}>
                <MonthGrid />
                <UpcomingPanel />
              </div>
              <StatusBar />
            </div>

            {/* 浮层挂在面板上，坐标都是相对面板左上角 */}
            <DayPopover />
            <GroupEditor />
            <SelectionChip />
            <RangeActionBar />
            <ContextMenu />
            <Toast />
            <ItemModal />
            <Guide />
          </>
        )}
      </div>
      </div>
    </PanelContext.Provider>
  );
}

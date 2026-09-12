import { useCallback, useMemo, useRef } from 'react';
import { useGlobalKeys } from '../hooks/useGlobalKeys';
import { usePanelDismiss } from '../hooks/usePanelDismiss';
import { useToastTimer } from '../hooks/useToastTimer';
import { useCalendar, useDispatch } from '../store/context';
import base from '../styles/base.module.css';
import { ContextMenu } from './ContextMenu';
import { DayPopover } from './DayPopover';
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

  const anchorOf = useCallback((el: Element | null) => {
    const root = rootRef.current;
    if (!root || !el) return null;
    const r = el.getBoundingClientRect();
    const rr = root.getBoundingClientRect();
    return { left: r.left - rr.left, top: r.top - rr.top, w: r.width, h: r.height };
  }, []);

  const toPanel = useCallback((clientX: number, clientY: number) => {
    const root = rootRef.current;
    if (!root) return { x: clientX, y: clientY };
    const rr = root.getBoundingClientRect();
    return { x: clientX - rr.left, y: clientY - rr.top };
  }, []);

  const api = useMemo(() => ({ anchorOf, toPanel }), [anchorOf, toPanel]);

  return (
    <PanelContext.Provider value={api}>
      <div
        ref={rootRef}
        data-cal-theme={s.theme}
        className={styles.panel}
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
            <SelectionChip />
            <RangeActionBar />
            <ContextMenu />
            <Toast />
            <ItemModal />
          </>
        )}
      </div>
    </PanelContext.Provider>
  );
}

import { useEffect } from 'react';
import { useCalendar, useDispatch } from '../store/context';

/**
 * 「点外部关闭」。
 * 不用透明遮罩 —— 那会吞掉用户这一次点击；改成在捕获阶段看 mousedown 的落点
 * 是否在浮层内。日期格算「内」，所以点另一格是切换日详情而不是先关掉。
 */
export function usePanelDismiss() {
  const s = useCalendar();
  const dispatch = useDispatch();

  useEffect(() => {
    const onDown = (e: MouseEvent) => {
      const t = e.target as Element | null;
      const inPopover = !!t?.closest?.('[data-daypop]');
      const inCell = !!t?.closest?.('[data-daycell]');

      if (s.ctx && !inPopover) dispatch({ type: 'closeCtx' });
      if (s.selActive && !inPopover && !inCell) dispatch({ type: 'selClear' });
      if (s.dayOpen && !inPopover && !inCell) dispatch({ type: 'closeDay' });
      if ((s.settingsOpen || s.themeMenuOpen || s.undatedOpen) && !t?.closest?.('[data-menu]')) {
        if (s.settingsOpen) dispatch({ type: 'toggleSettings' });
        if (s.themeMenuOpen) dispatch({ type: 'toggleThemeMenu' });
        if (s.undatedOpen) dispatch({ type: 'toggleUndated' });
      }
    };
    window.addEventListener('mousedown', onDown, true);
    return () => window.removeEventListener('mousedown', onDown, true);
  }, [s.ctx, s.selActive, s.dayOpen, s.settingsOpen, s.themeMenuOpen, s.undatedOpen, dispatch]);
}

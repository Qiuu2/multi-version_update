import { useEffect, type RefObject } from 'react';
import { useCalendar, useDispatch } from '../store/context';

/**
 * 全局快捷键。
 * ←/→ 翻月只在鼠标或焦点落在面板内时生效，避免误触；
 * Esc 按 右键菜单 → 框选 → 弹窗 → 日详情 → 设置/抽屉 → 清空搜索 逐层关闭。
 */
export function useGlobalKeys(rootRef: RefObject<HTMLElement>, pointerIn: RefObject<boolean>) {
  const s = useCalendar();
  const dispatch = useDispatch();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement | null)?.tagName || '';
      const typing = tag === 'INPUT' || tag === 'TEXTAREA';

      // 弹窗内回车保存；备注多行框里不触发
      if (e.key === 'Enter' && s.modal && tag !== 'TEXTAREA') {
        e.preventDefault();
        dispatch({ type: 'saveModal' });
        return;
      }

      if ((e.key === 'ArrowLeft' || e.key === 'ArrowRight') && !typing && !s.modal && !s.dayOpen) {
        const root = rootRef.current;
        const inside =
          pointerIn.current ||
          !!(root && (root.contains(e.target as Node) || root.contains(document.activeElement)));
        if (!inside) return;
        e.preventDefault();
        dispatch({ type: e.key === 'ArrowLeft' ? 'prevMonth' : 'nextMonth' });
        return;
      }

      if (e.key !== 'Escape') return;
      if (s.ctx) return void dispatch({ type: 'closeCtx' });
      if (s.selActive) return void dispatch({ type: 'selClear' });
      if (s.modal) return void dispatch({ type: 'closeModal' });
      if (s.dayOpen) return void dispatch({ type: 'closeDay' });
      if (s.settingsOpen) return void dispatch({ type: 'toggleSettings' });
      if (s.undatedOpen) return void dispatch({ type: 'toggleUndated' });
      if (s.themeMenuOpen) return void dispatch({ type: 'toggleThemeMenu' });
      if (s.search) dispatch({ type: 'setSearch', value: '' });
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [
    s.modal,
    s.dayOpen,
    s.ctx,
    s.selActive,
    s.settingsOpen,
    s.undatedOpen,
    s.themeMenuOpen,
    s.search,
    dispatch,
    rootRef,
    pointerIn,
  ]);
}

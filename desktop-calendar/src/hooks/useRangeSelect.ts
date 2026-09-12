import { useEffect, useRef } from 'react';
import { useDispatch } from '../store/context';
import type { Anchor, Selection } from '../types';

/**
 * 日期格横拖框选。
 * 松手时只有真的跨过 ≥ 2 天才留下选区并浮出批量操作条。
 */
export function useRangeSelect(sel: Selection | null) {
  const dispatch = useDispatch();
  const dragging = useRef(false);
  const suppress = useRef(false);
  const selRef = useRef(sel);
  selRef.current = sel;

  useEffect(() => {
    const onUp = () => {
      if (!dragging.current) return;
      dragging.current = false;
      const cur = selRef.current;
      if (cur && cur.start !== cur.end) {
        // 吞掉松手带出的那一次 click，并在同一事件循环结束后立刻解除，
        // 否则会连带吞掉之后任意一次点击
        suppress.current = true;
        window.setTimeout(() => {
          suppress.current = false;
        }, 0);
        dispatch({ type: 'selCommit' });
      } else {
        dispatch({ type: 'selClear' });
      }
    };
    window.addEventListener('mouseup', onUp);
    return () => window.removeEventListener('mouseup', onUp);
  }, [dispatch]);

  return {
    begin(key: string, anchor: Anchor | null) {
      dragging.current = true;
      dispatch({ type: 'selStart', key, anchor });
    },
    extend(key: string, anchor: Anchor | null) {
      if (dragging.current) dispatch({ type: 'selMove', key, anchor });
    },
    /** 读一次即清零 */
    consumeClick(): boolean {
      if (!suppress.current) return false;
      suppress.current = false;
      return true;
    },
  };
}

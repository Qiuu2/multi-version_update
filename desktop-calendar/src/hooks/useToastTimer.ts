import { useEffect } from 'react';
import { TOAST_MS } from '../constants';
import { useCalendar, useDispatch } from '../store/context';

/** 提示条 5 秒后自动消失；每次换文案重新计时 */
export function useToastTimer() {
  const { toast } = useCalendar();
  const dispatch = useDispatch();

  useEffect(() => {
    if (!toast) return;
    const t = window.setTimeout(() => dispatch({ type: 'dismissToast' }), TOAST_MS);
    return () => window.clearTimeout(t);
  }, [toast, dispatch]);
}

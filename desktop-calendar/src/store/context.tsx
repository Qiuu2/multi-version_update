import { createContext, useContext, useEffect, useReducer, useRef, type ReactNode } from 'react';
import { storage } from './persistence';
import { initialState, pickPersisted, reducer, type Action, type CalendarState } from './reducer';

const StateCtx = createContext<CalendarState | null>(null);
const DispatchCtx = createContext<((a: Action) => void) | null>(null);

export function CalendarProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, undefined, initialState);
  const hydrated = useRef(false);
  const latest = useRef(state);
  latest.current = state;

  // 启动读盘
  useEffect(() => {
    let alive = true;
    void storage.load().then((data) => {
      if (alive && data) dispatch({ type: 'hydrate', data });
      hydrated.current = true;
    });
    return () => {
      alive = false;
    };
  }, []);

  // 变更后防抖落盘。依赖列的每一项在未被改动时引用不变（reducer 只浅拷贝顶层），
  // 所以翻月、开浮层这类纯 UI 状态不会触发写盘。
  useEffect(() => {
    if (!hydrated.current) return;
    const t = window.setTimeout(() => void storage.save(pickPersisted(latest.current)), 250);
    return () => window.clearTimeout(t);
  }, [
    state.items,
    state.nextId,
    state.hidden,
    state.customGroups,
    state.showDone,
    state.showOther,
    state.theme,
  ]);

  return (
    <StateCtx.Provider value={state}>
      <DispatchCtx.Provider value={dispatch}>{children}</DispatchCtx.Provider>
    </StateCtx.Provider>
  );
}

export function useCalendar(): CalendarState {
  const s = useContext(StateCtx);
  if (!s) throw new Error('useCalendar 必须在 CalendarProvider 内使用');
  return s;
}

export function useDispatch(): (a: Action) => void {
  const d = useContext(DispatchCtx);
  if (!d) throw new Error('useDispatch 必须在 CalendarProvider 内使用');
  return d;
}

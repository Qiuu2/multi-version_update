import { createContext, useContext, useEffect, useReducer, useRef, type ReactNode } from 'react';
import { startAgentBridge } from '../lib/agentBridge';
import { getAutostart, setDesktopMode, setWindowScale } from '../lib/platform';
import { storage } from './persistence';
import { initialState, pickPersisted, reducer, type Action, type CalendarState } from './reducer';

const StateCtx = createContext<CalendarState | null>(null);
const DispatchCtx = createContext<((a: Action) => void) | null>(null);

export function CalendarProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, undefined, initialState);
  const hydrated = useRef(false);
  const latest = useRef(state);
  latest.current = state;

  // 启动读盘。读不到存档说明是头一回运行，顺手把使用说明展开
  useEffect(() => {
    let alive = true;
    void storage.load().then((data) => {
      if (!alive) return;
      if (data) dispatch({ type: 'hydrate', data });
      else dispatch({ type: 'setGuideOpen', value: true });
      hydrated.current = true;
    });
    void getAutostart().then((on) => {
      if (alive) dispatch({ type: 'setAutostart', value: on });
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
    state.groups,
    state.showDone,
    state.showOther,
    state.showHolidays,
    state.theme,
    state.desktopMode,
    state.scale,
  ]);

  // 本地 agent 接口：Claude / MCP / curl 经 127.0.0.1 下达的读写都从这里进来
  useEffect(() => startAgentBridge(() => latest.current, dispatch), []);

  // 把桌面模式与缩放同步到真实窗口（浏览器下是空操作）
  useEffect(() => {
    void setDesktopMode(state.desktopMode);
  }, [state.desktopMode]);

  useEffect(() => {
    void setWindowScale(state.scale);
  }, [state.scale]);

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

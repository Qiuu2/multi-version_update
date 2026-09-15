/**
 * 本地 agent 接口的前端这一半。
 *
 * Rust 侧在 127.0.0.1 上开了个只认 token 的小服务（src-tauri/src/bridge.rs），
 * 收到请求后 emit 一个 `agent-request` 事件过来，这里执行完再 invoke 回去。
 * 绕这一圈是为了让「运行中的界面」始终是唯一的写入口 —— 外部直接改
 * calendar.json 会被下一次防抖落盘整份盖掉。
 */
import { flushSync } from 'react-dom';
import type { Action, CalendarState } from '../store/reducer';
import { planAgentOp, type AgentOp } from './agentOps';

interface TauriEventApi {
  listen<T>(event: string, cb: (e: { payload: T }) => void): Promise<() => void>;
}

interface TauriCoreApi {
  invoke<T>(cmd: string, args?: Record<string, unknown>): Promise<T>;
}

interface TauriGlobal {
  core?: TauriCoreApi;
  event?: TauriEventApi;
}

function tauri(): TauriGlobal | null {
  return (window as unknown as { __TAURI__?: TauriGlobal }).__TAURI__ ?? null;
}

/** 执行一个操作并落到 store 上，返回给调用方的结果 */
export function runAgentOp(
  getState: () => CalendarState,
  dispatch: (a: Action) => void,
  op: AgentOp,
): Record<string, unknown> {
  try {
    const plan = planAgentOp(getState(), op);
    // flushSync：下一条请求可能紧跟着来，状态必须已经落定，
    // 否则两次 add 会读到同一个 nextId，撞成同一个 id。
    if (plan.action) flushSync(() => dispatch(plan.action as Action));
    return plan.result;
  } catch (err) {
    return { error: err instanceof Error ? err.message : String(err) };
  }
}

/**
 * 挂上监听，返回取消函数。不在 Tauri 里（浏览器 / 测试）时挂一个
 * window.__calendarAgent 方便直接驱动同一套逻辑。
 */
export function startAgentBridge(
  getState: () => CalendarState,
  dispatch: (a: Action) => void,
): () => void {
  const g = tauri();

  if (!g?.event?.listen || !g.core?.invoke) {
    const probe = (op: AgentOp) => runAgentOp(getState, dispatch, op);
    (window as unknown as Record<string, unknown>).__calendarAgent = probe;
    return () => {
      delete (window as unknown as Record<string, unknown>).__calendarAgent;
    };
  }

  const { core, event } = g;
  let stop: (() => void) | null = null;
  let dropped = false;

  void event
    .listen<{ id: number; op: AgentOp }>('agent-request', (e) => {
      const id = e.payload?.id;
      const result = runAgentOp(getState, dispatch, e.payload?.op ?? {});
      if (typeof id === 'number') {
        void core.invoke('agent_reply', { id, payload: JSON.stringify(result) }).catch(() => {
          /* 回包失败时 Rust 侧会自己超时，不必在界面上报错 */
        });
      }
    })
    .then((un) => {
      if (dropped) un();
      else stop = un;
    })
    .catch(() => {
      /* 没拿到监听权限就等于没开接口，界面照常用 */
    });

  return () => {
    dropped = true;
    stop?.();
  };
}

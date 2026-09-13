/**
 * 宿主环境差异。
 * 同一份前端既要能在浏览器里开发，也要能跑在 Tauri 窗口里，
 * 所有平台相关的动作都从这里走，浏览器下静默降级。
 */

interface TauriCore {
  invoke<T>(cmd: string, args?: Record<string, unknown>): Promise<T>;
}

function core(): TauriCore | null {
  return (window as unknown as { __TAURI__?: { core?: TauriCore } }).__TAURI__?.core ?? null;
}

export function isTauri(): boolean {
  return core() !== null;
}

/**
 * 桌面模式：窗口压到底层并从任务栏隐去，像桌面小组件一样待在桌面上。
 * 浏览器里没有这个概念，直接忽略。
 */
export async function setDesktopMode(enabled: boolean): Promise<void> {
  const c = core();
  if (!c) return;
  try {
    await c.invoke('set_desktop_mode', { enabled });
  } catch {
    /* 切换失败不该影响界面本身 */
  }
}

/** 无边框窗口没有系统关闭按钮，由面板自己的 × 触发 */
export async function closeWindow(): Promise<boolean> {
  const c = core();
  if (!c) return false;
  try {
    await c.invoke('close_window');
    return true;
  } catch {
    return false;
  }
}

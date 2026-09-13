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

/** 缩放变化时把窗口调成 设计尺寸 × 缩放 */
export async function setWindowScale(scale: number): Promise<void> {
  const c = core();
  if (!c) return;
  try {
    await c.invoke('set_window_scale', { scale });
  } catch {
    /* 调不动窗口不该影响界面 */
  }
}

export type Corner = 'tl' | 'tr' | 'bl' | 'br';

/** 吸附到屏幕四角之一，按可用区算，不会被任务栏压住 */
export async function snapCorner(corner: Corner): Promise<void> {
  const c = core();
  if (!c) return;
  try {
    await c.invoke('snap_corner', { corner });
  } catch {
    /* 同上 */
  }
}

/** 开机自启的真实状态在系统里，不在我们的存档里 */
export async function getAutostart(): Promise<boolean> {
  const c = core();
  if (!c) return false;
  try {
    return await c.invoke<boolean>('get_autostart');
  } catch {
    return false;
  }
}

export async function setAutostart(enabled: boolean): Promise<void> {
  const c = core();
  if (!c) return;
  try {
    await c.invoke('set_autostart', { enabled });
  } catch {
    /* 写系统启动项失败不该影响使用 */
  }
}

/**
 * 面板右上角的 × ：收进托盘而不是退出。
 * 窗口没有任务栏按钮，真正退出走托盘菜单，免得关掉后找不回来。
 * 浏览器里返回 false，调用方退回原来的「面板已关闭」。
 */
export async function hideToTray(): Promise<boolean> {
  const c = core();
  if (!c) return false;
  try {
    await c.invoke('hide_to_tray');
    return true;
  } catch {
    return false;
  }
}

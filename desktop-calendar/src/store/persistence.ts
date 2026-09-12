import type { Persisted } from './reducer';

/**
 * 存储适配器。
 * 产品形态是纯本地应用：跑在 Tauri 里时走 Rust 侧的 JSON 文件（后续换 SQLite
 * 只需要替换 src-tauri 里那两个命令，前端不动）；在浏览器里跑回退到 localStorage。
 */
export interface CalendarStorage {
  load(): Promise<Partial<Persisted> | null>;
  save(data: Persisted): Promise<void>;
}

const KEY = 'local-calendar/state/v1';

const browserStorage: CalendarStorage = {
  async load() {
    try {
      const raw = window.localStorage.getItem(KEY);
      return raw ? (JSON.parse(raw) as Partial<Persisted>) : null;
    } catch {
      return null;
    }
  },
  async save(data) {
    try {
      window.localStorage.setItem(KEY, JSON.stringify(data));
    } catch {
      /* 存储被禁用时静默降级，不影响使用 */
    }
  },
};

interface TauriGlobal {
  core?: { invoke<T>(cmd: string, args?: Record<string, unknown>): Promise<T> };
}

function tauriInvoke(): TauriGlobal['core'] | null {
  const g = (window as unknown as { __TAURI__?: TauriGlobal }).__TAURI__;
  return g?.core ?? null;
}

const tauriStorage: CalendarStorage = {
  async load() {
    const core = tauriInvoke();
    if (!core) return browserStorage.load();
    try {
      const raw = await core.invoke<string | null>('load_state');
      return raw ? (JSON.parse(raw) as Partial<Persisted>) : null;
    } catch {
      return null;
    }
  },
  async save(data) {
    const core = tauriInvoke();
    if (!core) return browserStorage.save(data);
    try {
      await core.invoke('save_state', { contents: JSON.stringify(data, null, 2) });
    } catch {
      /* 落盘失败不阻塞 UI */
    }
  },
};

export const storage: CalendarStorage = tauriInvoke() ? tauriStorage : browserStorage;

import type { Theme } from './types';

/** 面板固定尺寸，浮层的夹取边界都按这两个数算 */
export const PANEL_W = 980;
export const PANEL_H = 620;

/** 日详情浮卡翻转判定用的月视图右边界 */
export const MONTH_RIGHT = 604;

export const DAY_POPOVER_W = 248;
export const DAY_POPOVER_MAX_H = 292;

/** 内置分组识别色，不随主题变化 */
export const BUILTIN_GROUPS = ['学业', '中信实习', '中控项目', '求职'] as const;

export const BUILTIN_GROUP_COLORS: Record<string, string> = {
  学业: '#4E7A8C',
  中信实习: '#C25B3A',
  中控项目: '#7A8B4A',
  求职: '#9A6B8C',
};

/** 新建分组的 8 个可选配色 */
export const NEW_GROUP_COLORS = [
  '#6E8CA0',
  '#B98A3C',
  '#5F8F7A',
  '#A2647B',
  '#7E7AA8',
  '#A8794E',
  '#3C6E9F',
  '#3A3632',
];

export const REMINDER_OPTIONS = [
  '不提醒',
  '准时',
  '提前 5 分钟',
  '提前 10 分钟',
  '提前 30 分钟',
  '提前 1 小时',
  '提前 1 天',
];

/** 右侧栏四个临期分组；色值固定，不是主题色 */
export const URGENCY_SECTIONS: {
  name: string;
  color: string;
  test: (d: number) => boolean;
}[] = [
  { name: '已逾期', color: 'var(--c-danger)', test: (d) => d < 0 },
  { name: '今明', color: 'var(--c-accent)', test: (d) => d === 0 || d === 1 },
  { name: '3 天内', color: '#DE9A2E', test: (d) => d === 2 || d === 3 },
  { name: '7 天内', color: '#C9AE4A', test: (d) => d >= 4 && d <= 7 },
];

export const WEEKDAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
/** 内嵌月历的单字列头 */
export const WEEKDAY_SHORT = ['一', '二', '三', '四', '五', '六', '日'];

export const THEME_OPTIONS: { key: Theme; label: string; swatch: string }[] = [
  { key: 'light', label: '浅色', swatch: '#FAF6F1' },
  { key: 'dark', label: '深色', swatch: '#262320' },
  { key: 'blue', label: '蓝色', swatch: '#3C6E9F' },
];

/** 撤销栈上限 */
export const HISTORY_LIMIT = 20;
/** 提示条自动消失 */
export const TOAST_MS = 5000;
/** 单元格内最多显示的条目数，超出走「+N 更多」 */
export const MAX_CELL_ENTRIES = 3;

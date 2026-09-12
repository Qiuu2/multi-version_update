/** 条目类型：日程整块填色，任务是色条 + 文字 */
export type ItemType = 'event' | 'task';

export type Theme = 'light' | 'dark' | 'blue';

export type Priority = '低' | '中' | '高' | '';

export interface Subtask {
  title: string;
  date: string;
  done: boolean;
}

/**
 * 一条日程或任务。
 * 形状沿用交接文档「数据模型」，date/time 仍是两个字符串；
 * 换成 ISO 时间戳 + 时区是文档列出的产品化改动，见 README「与原型的差异」。
 */
export interface Item {
  id: number;
  type: ItemType;
  /** 分组名，'' = 未分组 */
  group: string;
  title: string;
  /** 'YYYY-MM-DD'，'' = 无期限 */
  date: string;
  time?: string;
  /** false = 全天 */
  withTime?: boolean;
  location?: string;
  reminder?: string;
  notes?: string;
  done?: boolean;
  priority?: Priority;
  /** 每周重复 */
  repeatOn?: boolean;
  repeatWeeks?: string;
  /** 同一组重复日程共用，用于「删除整组」 */
  repeatId?: string;
  /** 跨天日程的分组键 */
  spanId?: string;
  spanLabel?: string;
  /** 父任务名与其截止日，用于右侧栏的冲突提示 */
  parent?: string;
  parentDue?: string;
  subtasks?: Subtask[];
}

export interface CustomGroup {
  name: string;
  color: string;
}

/** 编辑弹窗的草稿，含各互斥浮层的开关 */
export interface ModalDraft {
  mode: 'create' | 'edit';
  kind: ItemType;
  id: number | null;
  title: string;
  group: string;
  priority: Priority;
  date: string;
  time: string;
  withTime: boolean;
  location: string;
  reminder: string;
  notes: string;
  parent?: string;
  parentDue?: string;
  subtasks: Subtask[];
  repeatOn: boolean;
  repeatWeeks: string;
  repeatId?: string;
  /** 四个浮层互斥，同一时刻只开一个 */
  groupListOpen: boolean;
  pickerOpen: boolean;
  timeOpen: boolean;
  reminderOpen: boolean;
  /** 内嵌月历自己的翻页位置，null = 跟随 date */
  pickerYear: number | null;
  pickerMonth: number | null;
}

/** 浮卡锚点，相对面板左上角 */
export interface Anchor {
  left: number;
  top: number;
  w: number;
  h: number;
}

export interface Selection {
  start: string;
  end: string;
}

export type ContextMenuState =
  | { kind: 'cell'; left: number; top: number; title: string; key: string }
  | { kind: 'item'; left: number; top: number; title: string; id: number };

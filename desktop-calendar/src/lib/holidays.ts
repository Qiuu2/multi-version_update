/**
 * 中国法定节假日。
 *
 * 数据只能照抄国务院办公厅每年发的通知 —— 春节、清明、端午、中秋跟农历走，
 * 调休上班日更是逐年拍的，算不出来。所以这里是一张手抄的表，
 * 抄一年管一年；没抄到的年份宁可什么都不显示，也不猜。
 *
 * 2026 年据《国务院办公厅关于2026年部分节假日安排的通知》（国办发明电〔2025〕7号，
 * 2025-11-04 发布）。2027 年的通知一般在 2026 年 11 月前后发布，到时候往这里加一段即可。
 */
import { shiftDate } from './date';

interface HolidaySpec {
  name: string;
  /** 放假区间，含首尾 */
  off: [string, string];
  /** 为了凑出连休而被调成上班的周末 */
  work: string[];
}

const TABLE: HolidaySpec[] = [
  { name: '元旦', off: ['2026-01-01', '2026-01-03'], work: ['2026-01-04'] },
  { name: '春节', off: ['2026-02-15', '2026-02-23'], work: ['2026-02-14', '2026-02-28'] },
  { name: '清明', off: ['2026-04-04', '2026-04-06'], work: [] },
  { name: '劳动节', off: ['2026-05-01', '2026-05-05'], work: ['2026-05-09'] },
  { name: '端午', off: ['2026-06-19', '2026-06-21'], work: [] },
  { name: '中秋', off: ['2026-09-25', '2026-09-27'], work: [] },
  { name: '国庆', off: ['2026-10-01', '2026-10-07'], work: ['2026-09-20', '2026-10-10'] },
];

/** 有官方通知可抄的年份。不在其中的年份一律不标。 */
export const HOLIDAY_YEARS: number[] = [2026];

export interface HolidayMark {
  kind: 'off' | 'work';
  /** 所属节日，如「国庆」 */
  name: string;
  /** 格子里那一小块字：放假首日写节日名，其余写「休」，调休上班写「班」 */
  label: string;
  /** 鼠标悬停时的完整说明 */
  hint: string;
}

const INDEX: Record<string, HolidayMark> = {};

for (const h of TABLE) {
  const [from, to] = h.off;
  for (let d = from, i = 0; d <= to; d = shiftDate(d, 1), i += 1) {
    INDEX[d] = {
      kind: 'off',
      name: h.name,
      // 首日写节日名，认起来快；后面几天只留一个「休」，免得把格子塞满
      label: i === 0 ? h.name : '休',
      hint: `${h.name}放假（${from} 至 ${to}）`,
    };
  }
  for (const d of h.work) {
    INDEX[d] = {
      kind: 'work',
      name: h.name,
      label: '班',
      hint: `${h.name}调休，这天要上班`,
    };
  }
}

export function markOf(key: string): HolidayMark | null {
  return INDEX[key] ?? null;
}

/** 这一年有没有抄到通知 —— 用来决定要不要在界面上说明「还没公布」 */
export function hasHolidayData(year: number): boolean {
  return HOLIDAY_YEARS.includes(year);
}

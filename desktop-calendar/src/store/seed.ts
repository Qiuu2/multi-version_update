import { shiftDate, todayIso } from '../lib/date';
import type { Item } from '../types';

/**
 * 示例数据。
 * 原型里日期是写死的 2026-09-xx；这里换成相对今天的偏移，
 * 保持同样的疏密分布，任何一天跑起来都不会看到一堆过期条目。
 */
export function seedItems(): Item[] {
  const t0 = todayIso();
  const d = (n: number) => shiftDate(t0, n);

  return [
    {
      id: 1,
      type: 'event',
      group: '中信实习',
      title: '14:30 量化组周会',
      date: d(0),
      time: '14:30',
      withTime: true,
      location: '产品支持中心会议室',
      reminder: '提前 10 分钟',
      repeatOn: true,
      repeatWeeks: '15',
    },
    { id: 2, type: 'task', group: '中信实习', title: '跑净值清洗脚本', date: d(0) },
    { id: 3, type: 'task', group: '中信实习', title: '实习周报', date: d(3) },
    { id: 4, type: 'event', group: '中控项目', title: '10:00 中控设备联调', date: d(4) },
    { id: 5, type: 'task', group: '求职', title: 'HSBC 面试准备', date: d(6) },
    { id: 6, type: 'event', group: '求职', title: '19:00 模拟面试', date: d(6) },
    { id: 7, type: 'task', group: '学业', title: '统计推断 assignment', date: d(8) },
    { id: 8, type: 'event', group: '中控项目', title: '09:00 项目评审', date: d(10) },
    { id: 9, type: 'task', group: '学业', title: '论文修改', date: d(10) },
    { id: 10, type: 'task', group: '求职', title: '简历更新', date: d(10) },
    { id: 11, type: 'task', group: '中信实习', title: '对账单核对', date: d(10) },
    { id: 12, type: 'task', group: '学业', title: '文献精读', date: d(10) },
    // 晚于父任务截止日 → 右侧栏这一行带红色左描边
    {
      id: 13,
      type: 'task',
      group: '求职',
      title: '过一遍信用评分卡模型',
      date: d(-2),
      parent: 'HSBC 面试准备',
      parentDue: d(-3),
    },
    { id: 14, type: 'task', group: '学业', title: '机器学习作业提交', date: d(0) },
    { id: 15, type: 'task', group: '中信实习', title: '季度总结初稿', date: d(1) },
    { id: 16, type: 'task', group: '中控项目', title: '读书笔记', date: d(3), done: true },
    { id: 17, type: 'task', group: '求职', title: '健身房续费', date: d(3) },
    { id: 18, type: 'task', group: '求职', title: '更新作品集网站', date: d(7), parent: '求职季度计划' },
    { id: 19, type: 'task', group: '学业', title: '预约体检', date: d(6) },
    { id: 20, type: 'task', group: '中控项目', title: '回复邮件-导师', date: d(7) },
    // 无期限，落在底栏抽屉
    { id: 21, type: 'task', group: '学业', title: '期中复习计划', date: '' },
    { id: 22, type: 'task', group: '学业', title: '整理课程笔记', date: '' },
    { id: 23, type: 'task', group: '中信实习', title: '读因子研报', date: '' },
    { id: 24, type: 'task', group: '中信实习', title: '整理估值模板', date: '' },
    { id: 25, type: 'task', group: '中控项目', title: '补设备台账', date: '' },
    { id: 26, type: 'task', group: '求职', title: '联系内推', date: '' },
    { id: 27, type: 'task', group: '求职', title: '更新简历英文版', date: '' },
  ];
}

export const SEED_NEXT_ID = 28;
/** 原型的初始状态里「求职」是收起的 */
export const SEED_HIDDEN: Record<string, boolean> = { 求职: true };

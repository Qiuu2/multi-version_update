import { todayIso } from '../lib/date';
import { useCalendar } from '../store/context';
import { footerCounts, undatedItems } from '../store/selectors';
import styles from './StatusBar.module.css';

export function StatusBar() {
  const s = useCalendar();
  const { events, todos } = footerCounts(s, todayIso());

  return (
    <div className={styles.bar}>
      <span>
        今日 · {events} 个日程 / {todos} 项待办
      </span>
      {/* 无期限条目已经常驻右侧栏的收集箱，这里只留个计数 */}
      <span>收集箱 {undatedItems(s).length} 项</span>
    </div>
  );
}

import { Inbox } from './Inbox';
import { UpcomingPanel } from './UpcomingPanel';
import styles from './RightRail.module.css';

/** 右侧栏：上半是「未来 7 天」，下半是常驻的收集箱 */
export function RightRail() {
  return (
    <div className={styles.rail}>
      <UpcomingPanel />
      <Inbox />
    </div>
  );
}

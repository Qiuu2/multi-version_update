import { useCalendar, useDispatch } from '../store/context';
import { isTauri } from '../lib/platform';
import base from '../styles/base.module.css';
import { GUIDE } from './guideContent';
import styles from './Guide.module.css';

/** 首次运行自动展开；之后可从齿轮菜单再打开 */
export function Guide() {
  const s = useCalendar();
  const dispatch = useDispatch();
  if (!s.guideOpen) return null;

  const close = () => dispatch({ type: 'setGuideOpen', value: false });

  return (
    <div className={styles.overlay} onClick={close}>
      <div className={styles.card} onClick={(e) => e.stopPropagation()}>
        <div className={styles.head}>
          <span className={styles.title}>本地日历 · 使用说明</span>
          <span className={styles.sub}>随时可从齿轮菜单再次打开</span>
        </div>

        <div className={styles.body}>
          {GUIDE.map((section) => (
            <div key={section.name} className={styles.section}>
              <span className={styles.sectionName}>{section.name}</span>
              {section.rows.map(([k, v]) => (
                <div key={k} className={styles.row}>
                  <span className={styles.key}>{k}</span>
                  <span className={styles.val}>{v}</span>
                </div>
              ))}
            </div>
          ))}
        </div>

        <div className={styles.foot}>
          <span className={styles.where}>
            {isTauri()
              ? '数据存在 %APPDATA%\\com.local.calendar\\calendar.json，重启电脑不会丢'
              : '数据存在浏览器本地存储里'}
          </span>
          <button type="button" className={`${base.btnPrimary} ${styles.okBtn}`} onClick={close}>
            知道了
          </button>
        </div>
      </div>
    </div>
  );
}

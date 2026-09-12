/**
 * 内联图标：线宽 2、round 端点，描边默认取 --c-muted。
 * 与 Lucide 的 search / plus / calendar / clock / bell / repeat / settings /
 * rotate-ccw / check / chevron-down 一一对应，需要时可整批换掉。
 */
interface IconProps {
  size?: number;
  stroke?: string;
  strokeWidth?: number;
  className?: string;
  style?: React.CSSProperties;
}

function base({ size = 14, stroke = 'var(--c-muted)', strokeWidth = 2, className, style }: IconProps) {
  return {
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none' as const,
    stroke,
    strokeWidth,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    className,
    style,
  };
}

export function SearchIcon(p: IconProps) {
  return (
    <svg {...base({ size: 13, stroke: 'var(--c-faint)', ...p })}>
      <circle cx="11" cy="11" r="7" />
      <line x1="21" y1="21" x2="16.65" y2="16.65" />
    </svg>
  );
}

export function PlusIcon(p: IconProps) {
  return (
    <svg {...base({ size: 14, stroke: '#FFFFFF', strokeWidth: 2.2, ...p })}>
      <line x1="12" y1="5" x2="12" y2="19" />
      <line x1="5" y1="12" x2="19" y2="12" />
    </svg>
  );
}

/** 半明半暗的圆，表示配色风格 */
export function ThemeIcon(p: IconProps) {
  const a = base({ size: 15, ...p });
  return (
    <svg {...a}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 3a9 9 0 0 1 0 18z" fill={a.stroke} stroke="none" />
    </svg>
  );
}

export function UndoIcon(p: IconProps) {
  return (
    <svg {...base({ size: 15, ...p })}>
      <path d="M3 7v6h6" />
      <path d="M3 13a9 9 0 1 0 3-7.7L3 8" />
    </svg>
  );
}

export function GearIcon(p: IconProps) {
  return (
    <svg {...base({ size: 15, ...p })}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09A1.65 1.65 0 0 0 15 4.6a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  );
}

export function CalendarIcon(p: IconProps) {
  return (
    <svg {...base({ size: 12, ...p })}>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <line x1="16" y1="2" x2="16" y2="6" />
      <line x1="8" y1="2" x2="8" y2="6" />
      <line x1="3" y1="10" x2="21" y2="10" />
    </svg>
  );
}

export function ClockIcon(p: IconProps) {
  return (
    <svg {...base({ size: 12, ...p })}>
      <circle cx="12" cy="12" r="9" />
      <polyline points="12 7 12 12 15 14" />
    </svg>
  );
}

export function BellIcon(p: IconProps) {
  return (
    <svg {...base({ size: 12, ...p })}>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  );
}

export function RepeatIcon(p: IconProps) {
  return (
    <svg {...base({ size: 9, strokeWidth: 2.5, ...p })}>
      <polyline points="17 1 21 5 17 9" />
      <path d="M3 11V9a4 4 0 0 1 4-4h14" />
      <polyline points="7 23 3 19 7 15" />
      <path d="M21 13v2a4 4 0 0 1-4 4H3" />
    </svg>
  );
}

export function ChevronDown(p: IconProps) {
  return (
    <svg {...base({ size: 10, strokeWidth: 2.5, ...p })}>
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

export function ChevronUp(p: IconProps) {
  return (
    <svg {...base({ size: 10, strokeWidth: 2.5, ...p })}>
      <polyline points="18 15 12 9 6 15" />
    </svg>
  );
}

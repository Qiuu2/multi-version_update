/** 把十六进制色按透明度调淡，用于分组徽章底（= 该色 12%） */
export function tint(hex: string, alpha: number): string {
  if (!hex || hex.charAt(0) !== '#') {
    // var(--c-danger) 这类 CSS 变量走 color-mix
    return `color-mix(in srgb, ${hex} ${Math.round(alpha * 100)}%, transparent)`;
  }
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

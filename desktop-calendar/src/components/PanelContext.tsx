import { createContext, useContext } from 'react';
import type { Anchor } from '../types';

interface PanelApi {
  /** 元素矩形换算成相对面板左上角的锚点 */
  anchorOf(el: Element | null): Anchor | null;
  /** 视口坐标换算成面板内坐标 */
  toPanel(clientX: number, clientY: number): { x: number; y: number };
}

export const PanelContext = createContext<PanelApi>({
  anchorOf: () => null,
  toPanel: (x, y) => ({ x, y }),
});

export function usePanel(): PanelApi {
  return useContext(PanelContext);
}

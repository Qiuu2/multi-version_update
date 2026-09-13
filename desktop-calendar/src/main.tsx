import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { CalendarPanel } from './components/CalendarPanel';
import { isTauri } from './lib/platform';
import { CalendarProvider } from './store/context';
import './styles/tokens.css';

// Tauri 里窗口是无边框透明的，页面底色要让开，否则会盖住圆角外的透明区
if (isTauri()) document.documentElement.dataset.shell = 'tauri';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <CalendarProvider>
      <CalendarPanel />
    </CalendarProvider>
  </StrictMode>,
);

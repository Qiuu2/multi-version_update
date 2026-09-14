/**
 * 输入法合成期的按键要放行。
 *
 * 用中文 / 日文输入法打字时，按 Enter 是在确认候选词，按 Esc 是在取消候选词。
 * 这两次按键同样会以 keydown 送到页面，并带 isComposing = true。
 * 如果不加判断，「回车提交」会在候选词还没落进输入框时就触发 ——
 * 表现为：按了回车什么也没发生，字随后才出现在框里。
 */
export function isComposing(e: { nativeEvent?: Event } | KeyboardEvent): boolean {
  const native = 'nativeEvent' in e && e.nativeEvent ? e.nativeEvent : e;
  return (native as KeyboardEvent).isComposing === true;
}

/** 真正意义上的「提交」回车：不在输入法合成期 */
export function isSubmitEnter(e: React.KeyboardEvent | KeyboardEvent): boolean {
  return e.key === 'Enter' && !isComposing(e as KeyboardEvent);
}

use std::fs;
use std::path::PathBuf;
use tauri::Manager;

/// 状态文件落在系统的应用数据目录里，例如
/// Windows: %APPDATA%\com.local.calendar\calendar.json
fn state_path(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    let dir = app
        .path()
        .app_data_dir()
        .map_err(|e| format!("取应用数据目录失败: {e}"))?;
    fs::create_dir_all(&dir).map_err(|e| format!("建目录失败: {e}"))?;
    Ok(dir.join("calendar.json"))
}

/// 读状态；文件不存在时返回 None，前端会退回种子数据
#[tauri::command]
fn load_state(app: tauri::AppHandle) -> Result<Option<String>, String> {
    let path = state_path(&app)?;
    if !path.exists() {
        return Ok(None);
    }
    fs::read_to_string(&path)
        .map(Some)
        .map_err(|e| format!("读取 {} 失败: {e}", path.display()))
}

/// 先写临时文件再改名，避免写一半掉电把状态弄坏
#[tauri::command]
fn save_state(app: tauri::AppHandle, contents: String) -> Result<(), String> {
    let path = state_path(&app)?;
    let tmp = path.with_extension("json.tmp");
    fs::write(&tmp, contents).map_err(|e| format!("写入失败: {e}"))?;
    fs::rename(&tmp, &path).map_err(|e| format!("落盘失败: {e}"))
}

/// 桌面模式：把窗口压到所有窗口之下、并从任务栏和 Alt+Tab 里隐去，
/// 让它像桌面小组件一样贴在桌面上，而不是一个会挡住工作的浮窗。
#[tauri::command]
fn set_desktop_mode(window: tauri::Window, enabled: bool) -> Result<(), String> {
    window
        .set_always_on_bottom(enabled)
        .map_err(|e| format!("设置窗口层级失败: {e}"))?;
    window
        .set_skip_taskbar(enabled)
        .map_err(|e| format!("设置任务栏显示失败: {e}"))?;
    Ok(())
}

/// 窗口是无边框的，没有系统标题栏，关闭要由面板自己的按钮触发
#[tauri::command]
fn close_window(window: tauri::Window) -> Result<(), String> {
    window.close().map_err(|e| format!("关闭窗口失败: {e}"))
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            load_state,
            save_state,
            set_desktop_mode,
            close_window
        ])
        .run(tauri::generate_context!())
        .expect("启动失败");
}

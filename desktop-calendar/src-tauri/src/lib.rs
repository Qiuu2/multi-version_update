use std::fs;
use std::path::PathBuf;
use std::sync::Mutex;

use tauri::menu::{Menu, MenuItem};
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::{LogicalSize, Manager, PhysicalPosition, WindowEvent};

/// 面板的设计尺寸（含 1px 描边），窗口大小 = 设计尺寸 × 缩放
const PANEL_W: f64 = 982.0;
const PANEL_H: f64 = 622.0;

const MAIN_WINDOW: &str = "main";

/// 窗口最近一次的位置，随拖动更新，在关闭 / 收进托盘时落盘
#[derive(Default)]
struct LastPosition(Mutex<Option<(i32, i32)>>);

fn data_dir(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    let dir = app
        .path()
        .app_data_dir()
        .map_err(|e| format!("取应用数据目录失败: {e}"))?;
    fs::create_dir_all(&dir).map_err(|e| format!("建目录失败: {e}"))?;
    Ok(dir)
}

/// 日程数据文件，例如 Windows 下的
/// %APPDATA%\com.local.calendar\calendar.json
fn state_path(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    Ok(data_dir(app)?.join("calendar.json"))
}

/// 窗口位置单独存，免得和日程数据互相干扰
fn window_path(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    Ok(data_dir(app)?.join("window.json"))
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

/// 桌面模式：压到所有窗口之下，像桌面小组件一样待在桌面上。
/// 任务栏按钮一直是关掉的（改由托盘图标进入），所以这里只管层级。
#[tauri::command]
fn set_desktop_mode(window: tauri::Window, enabled: bool) -> Result<(), String> {
    window
        .set_always_on_bottom(enabled)
        .map_err(|e| format!("设置窗口层级失败: {e}"))
}

/// 缩放变化时把窗口调成 设计尺寸 × 缩放
#[tauri::command]
fn set_window_scale(window: tauri::Window, scale: f64) -> Result<(), String> {
    let k = scale.clamp(0.5, 2.0);
    window
        .set_size(LogicalSize::new(PANEL_W * k, PANEL_H * k))
        .map_err(|e| format!("调整窗口大小失败: {e}"))
}

/// 吸附到屏幕某个角。用 work_area 而不是整块屏幕，这样不会被任务栏压住。
#[tauri::command]
fn snap_corner(window: tauri::Window, corner: String) -> Result<(), String> {
    let monitor = window
        .current_monitor()
        .map_err(|e| format!("取显示器信息失败: {e}"))?
        .ok_or_else(|| "找不到当前显示器".to_string())?;

    let area = monitor.work_area();
    let size = window
        .outer_size()
        .map_err(|e| format!("取窗口尺寸失败: {e}"))?;

    // 留一点边距，别顶死屏幕边缘
    let margin = (16.0 * monitor.scale_factor()).round() as i32;
    let left = area.position.x + margin;
    let top = area.position.y + margin;
    let right = area.position.x + area.size.width as i32 - size.width as i32 - margin;
    let bottom = area.position.y + area.size.height as i32 - size.height as i32 - margin;

    let (x, y) = match corner.as_str() {
        "tl" => (left, top),
        "tr" => (right, top),
        "bl" => (left, bottom),
        _ => (right, bottom),
    };

    window
        .set_position(PhysicalPosition::new(x, y))
        .map_err(|e| format!("移动窗口失败: {e}"))
}

/// 收进托盘。窗口是无边框的，面板右上角的 × 走这里，
/// 真正退出要用托盘菜单，免得关掉之后只能回开始菜单找。
#[tauri::command]
fn hide_to_tray(app: tauri::AppHandle, window: tauri::Window) -> Result<(), String> {
    persist_position(&app);
    window.hide().map_err(|e| format!("隐藏窗口失败: {e}"))
}

fn persist_position(app: &tauri::AppHandle) {
    let pos = app.state::<LastPosition>().0.lock().ok().and_then(|g| *g);
    if let (Some((x, y)), Ok(path)) = (pos, window_path(app)) {
        let _ = fs::write(path, format!("{{\"x\":{x},\"y\":{y}}}"));
    }
}

fn restore_position(app: &tauri::AppHandle, window: &tauri::Window) {
    let Ok(path) = window_path(app) else {
        return;
    };
    let Ok(raw) = fs::read_to_string(path) else {
        return;
    };
    let Ok(v) = serde_json::from_str::<serde_json::Value>(&raw) else {
        return;
    };
    if let (Some(x), Some(y)) = (
        v.get("x").and_then(serde_json::Value::as_i64),
        v.get("y").and_then(serde_json::Value::as_i64),
    ) {
        let _ = window.set_position(PhysicalPosition::new(x as i32, y as i32));
    }
}

fn toggle_window(app: &tauri::AppHandle) {
    let Some(window) = app.get_window(MAIN_WINDOW) else {
        return;
    };
    if window.is_visible().unwrap_or(false) {
        persist_position(app);
        let _ = window.hide();
    } else {
        let _ = window.show();
        let _ = window.set_focus();
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .manage(LastPosition::default())
        .invoke_handler(tauri::generate_handler![
            load_state,
            save_state,
            set_desktop_mode,
            set_window_scale,
            snap_corner,
            hide_to_tray
        ])
        .setup(|app| {
            let handle = app.handle().clone();

            // 托盘：任务栏里不占位置，从通知区域进出
            let toggle = MenuItem::with_id(app, "toggle", "显示 / 隐藏", true, None::<&str>)?;
            let snap = MenuItem::with_id(app, "snap_br", "吸附到右下角", true, None::<&str>)?;
            let quit = MenuItem::with_id(app, "quit", "退出", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&toggle, &snap, &quit])?;

            let mut tray = TrayIconBuilder::with_id("main-tray")
                .tooltip("本地日历")
                .menu(&menu)
                .show_menu_on_left_click(false)
                .on_menu_event(|app, event| match event.id.as_ref() {
                    "toggle" => toggle_window(app),
                    "snap_br" => {
                        if let Some(w) = app.get_window(MAIN_WINDOW) {
                            let _ = snap_corner(w, "br".into());
                        }
                    }
                    "quit" => {
                        persist_position(app);
                        app.exit(0);
                    }
                    _ => {}
                })
                .on_tray_icon_event(|tray, event| {
                    // 左键单击切换显示，右键留给菜单
                    if let TrayIconEvent::Click {
                        button: MouseButton::Left,
                        button_state: MouseButtonState::Up,
                        ..
                    } = event
                    {
                        toggle_window(tray.app_handle());
                    }
                });
            if let Some(icon) = app.default_window_icon() {
                tray = tray.icon(icon.clone());
            }
            tray.build(app)?;

            if let Some(window) = app.get_window(MAIN_WINDOW) {
                restore_position(&handle, &window);

                let moved_handle = handle.clone();
                window.on_window_event(move |event| match event {
                    WindowEvent::Moved(pos) => {
                        if let Ok(mut guard) = moved_handle.state::<LastPosition>().0.lock() {
                            *guard = Some((pos.x, pos.y));
                        }
                    }
                    WindowEvent::CloseRequested { .. } | WindowEvent::Destroyed => {
                        persist_position(&moved_handle);
                    }
                    _ => {}
                });
            }

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("启动失败");
}

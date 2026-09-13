use std::fs;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;

use tauri::menu::{Menu, MenuItem};
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::{LogicalSize, Manager, PhysicalPosition, WindowEvent};

/// 面板的设计尺寸（含 1px 描边），窗口大小 = 设计尺寸 × 缩放
const PANEL_W: f64 = 982.0;
const PANEL_H: f64 = 622.0;

const MAIN_WINDOW: &str = "main";

/// 窗口边距，吸附和夹回屏内时都用它
const MARGIN: f64 = 16.0;

#[derive(Default)]
struct AppState {
    /// 窗口最近一次的位置，随拖动更新，关闭 / 收起 / 退出时落盘
    last_position: Mutex<Option<(i32, i32)>>,
    /// 桌面模式是否开启。失焦后要据此决定是否把窗口放回底层，
    /// 所以 Rust 侧也要留一份，不能只存在前端。
    desktop_mode: AtomicBool,
    /// 窗口当前是否已处于底层。
    /// set_skip_taskbar 在 Windows 上会改窗口扩展样式，可能再引发一次失焦；
    /// 记住已应用的状态，重复调用直接跳过，避免失焦 -> 下沉 -> 再失焦的抖动。
    sunk: AtomicBool,
}

fn data_dir(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    let dir = app
        .path()
        .app_data_dir()
        .map_err(|e| format!("取应用数据目录失败: {e}"))?;
    fs::create_dir_all(&dir).map_err(|e| format!("建目录失败: {e}"))?;
    Ok(dir)
}

fn state_path(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    Ok(data_dir(app)?.join("calendar.json"))
}

fn window_path(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    Ok(data_dir(app)?.join("window.json"))
}

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

// ---------- 窗口层级 ----------

/// 应用窗口层级。状态没变就不动手，避免多余的样式改动引发抖动。
fn apply_layer(app: &tauri::AppHandle, window: &tauri::WebviewWindow, on_bottom: bool) {
    let state = app.state::<AppState>();
    if state.sunk.swap(on_bottom, Ordering::Relaxed) == on_bottom {
        return;
    }
    // 桌面模式下压到底层、并从任务栏隐去（改由托盘图标进出）
    let _ = window.set_always_on_bottom(on_bottom);
    let _ = window.set_skip_taskbar(on_bottom);
}

#[tauri::command]
fn set_desktop_mode(
    app: tauri::AppHandle,
    window: tauri::WebviewWindow,
    enabled: bool,
) -> Result<(), String> {
    app.state::<AppState>()
        .desktop_mode
        .store(enabled, Ordering::Relaxed);
    apply_layer(&app, &window, enabled);
    Ok(())
}

#[tauri::command]
fn set_window_scale(window: tauri::WebviewWindow, scale: f64) -> Result<(), String> {
    let k = scale.clamp(0.5, 2.0);
    window
        .set_size(LogicalSize::new(PANEL_W * k, PANEL_H * k))
        .map_err(|e| format!("调整窗口大小失败: {e}"))?;
    // 放大后可能超出屏幕，顺手夹回来
    ensure_on_screen(&window);
    Ok(())
}

/// 取窗口所在显示器的可用区（排除任务栏）；窗口整个跑到屏外时退回主显示器
fn work_area(window: &tauri::WebviewWindow) -> Option<(i32, i32, i32, i32, f64)> {
    let monitor = window
        .current_monitor()
        .ok()
        .flatten()
        .or_else(|| window.primary_monitor().ok().flatten())?;
    let a = monitor.work_area();
    Some((
        a.position.x,
        a.position.y,
        a.size.width as i32,
        a.size.height as i32,
        monitor.scale_factor(),
    ))
}

/// 把窗口夹回可用区内。
/// 换显示器、改分辨率、或存下的坐标已失效时，窗口会整个落在屏幕外，
/// 那时它既看不见也点不到 —— 这里保证它永远至少有一部分在屏内。
fn ensure_on_screen(window: &tauri::WebviewWindow) {
    let (Some((ax, ay, aw, ah, sf)), Ok(size), Ok(pos)) = (
        work_area(window),
        window.outer_size(),
        window.outer_position(),
    ) else {
        return;
    };
    let m = (MARGIN * sf).round() as i32;
    let w = size.width as i32;
    let h = size.height as i32;

    // 窗口比可用区还大时贴左上，否则夹在 [边距, 可用区右/下边 - 窗口尺寸 - 边距]
    let max_x = (ax + aw - w - m).max(ax + m);
    let max_y = (ay + ah - h - m).max(ay + m);
    let x = pos.x.clamp(ax + m, max_x);
    let y = pos.y.clamp(ay + m, max_y);

    if x != pos.x || y != pos.y {
        let _ = window.set_position(PhysicalPosition::new(x, y));
    }
}

#[tauri::command]
fn snap_corner(window: tauri::WebviewWindow, corner: String) -> Result<(), String> {
    let (ax, ay, aw, ah, sf) = work_area(&window).ok_or("找不到当前显示器")?;
    let size = window
        .outer_size()
        .map_err(|e| format!("取窗口尺寸失败: {e}"))?;

    let m = (MARGIN * sf).round() as i32;
    let left = ax + m;
    let top = ay + m;
    let right = ax + aw - size.width as i32 - m;
    let bottom = ay + ah - size.height as i32 - m;

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

/// 把窗口切实带到用户眼前。
/// 托盘左键、托盘菜单「显示」都走这里，且**不做任何 toggle 判断** ——
/// is_visible() 只表示「没被 hide」，桌面模式下窗口永远是 visible 但压在最底层，
/// 用它来回切会让人永远看不到窗口。
fn bring_to_front(app: &tauri::AppHandle) {
    let Some(window) = app.get_webview_window(MAIN_WINDOW) else {
        return;
    };
    let _ = window.unminimize();
    let _ = window.show();
    // 先脱离底层，否则 show 完立刻沉下去，只留一次抢焦点（表现为输入法被切走）
    apply_layer(app, &window, false);
    ensure_on_screen(&window);
    let _ = window.set_focus();
}

/// 失焦后放回桌面层：用户点别处时它自己沉下去，不挡事
fn sink_if_desktop(app: &tauri::AppHandle) {
    if !app.state::<AppState>().desktop_mode.load(Ordering::Relaxed) {
        return;
    }
    if let Some(window) = app.get_webview_window(MAIN_WINDOW) {
        apply_layer(app, &window, true);
    }
}

fn hide_window(app: &tauri::AppHandle) {
    persist_position(app);
    if let Some(window) = app.get_webview_window(MAIN_WINDOW) {
        let _ = window.hide();
    }
}

#[tauri::command]
fn hide_to_tray(app: tauri::AppHandle) -> Result<(), String> {
    hide_window(&app);
    Ok(())
}

/// 卡住时的退路：清掉记住的位置、关掉桌面模式（含写回 calendar.json，
/// 否则下次启动又会沉下去）、吸附到右下角并拿到前台。
fn reset_window(app: &tauri::AppHandle) {
    if let Ok(path) = window_path(app) {
        let _ = fs::remove_file(path);
    }
    if let Ok(mut guard) = app.state::<AppState>().last_position.lock() {
        *guard = None;
    }
    app.state::<AppState>()
        .desktop_mode
        .store(false, Ordering::Relaxed);

    if let Ok(path) = state_path(app) {
        if let Ok(raw) = fs::read_to_string(&path) {
            if let Ok(mut v) = serde_json::from_str::<serde_json::Value>(&raw) {
                if let Some(obj) = v.as_object_mut() {
                    obj.insert("desktopMode".into(), serde_json::Value::Bool(false));
                    if let Ok(out) = serde_json::to_string_pretty(&v) {
                        let _ = fs::write(&path, out);
                    }
                }
            }
        }
    }

    if let Some(window) = app.get_webview_window(MAIN_WINDOW) {
        apply_layer(app, &window, false);
        let _ = snap_corner(window, "br".into());
    }
    bring_to_front(app);
}

// ---------- 位置记忆 ----------

fn persist_position(app: &tauri::AppHandle) {
    let pos = app
        .state::<AppState>()
        .last_position
        .lock()
        .ok()
        .and_then(|g| *g);
    if let (Some((x, y)), Ok(path)) = (pos, window_path(app)) {
        let _ = fs::write(path, format!("{{\"x\":{x},\"y\":{y}}}"));
    }
}

/// 返回 true 表示读到了存档位置；false 表示这是首次运行
fn restore_position(app: &tauri::AppHandle, window: &tauri::WebviewWindow) -> bool {
    let Ok(path) = window_path(app) else {
        return false;
    };
    let Ok(raw) = fs::read_to_string(path) else {
        return false;
    };
    let Ok(v) = serde_json::from_str::<serde_json::Value>(&raw) else {
        return false;
    };
    match (
        v.get("x").and_then(serde_json::Value::as_i64),
        v.get("y").and_then(serde_json::Value::as_i64),
    ) {
        (Some(x), Some(y)) => {
            let _ = window.set_position(PhysicalPosition::new(x as i32, y as i32));
            ensure_on_screen(window);
            true
        }
        _ => false,
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        // 单实例：再次启动只把已有窗口带到前面，不再开一个看不见的新进程
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            bring_to_front(app);
        }))
        .manage(AppState::default())
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

            let show = MenuItem::with_id(app, "show", "显示日历", true, None::<&str>)?;
            let hide = MenuItem::with_id(app, "hide", "隐藏到托盘", true, None::<&str>)?;
            let snap = MenuItem::with_id(app, "snap_br", "吸附到右下角", true, None::<&str>)?;
            let reset =
                MenuItem::with_id(app, "reset", "重置窗口（找不到时用）", true, None::<&str>)?;
            let quit = MenuItem::with_id(app, "quit", "退出", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&show, &hide, &snap, &reset, &quit])?;

            let mut tray = TrayIconBuilder::with_id("main-tray")
                .tooltip("本地日历（左键点出窗口）")
                .menu(&menu)
                .show_menu_on_left_click(false)
                .on_menu_event(|app, event| match event.id.as_ref() {
                    "show" => bring_to_front(app),
                    "hide" => hide_window(app),
                    "snap_br" => {
                        if let Some(w) = app.get_webview_window(MAIN_WINDOW) {
                            let _ = snap_corner(w, "br".into());
                        }
                        bring_to_front(app);
                    }
                    "reset" => reset_window(app),
                    "quit" => {
                        persist_position(app);
                        app.exit(0);
                    }
                    _ => {}
                })
                .on_tray_icon_event(|tray, event| {
                    // 左键只负责「把窗口拿到眼前」，隐藏交给菜单，避免盲目 toggle
                    if let TrayIconEvent::Click {
                        button: MouseButton::Left,
                        button_state: MouseButtonState::Up,
                        ..
                    } = event
                    {
                        bring_to_front(tray.app_handle());
                    }
                });
            if let Some(icon) = app.default_window_icon() {
                tray = tray.icon(icon.clone());
            }
            tray.build(app)?;

            if let Some(window) = app.get_webview_window(MAIN_WINDOW) {
                // 首次运行直接摆到右下角，而不是屏幕正中
                if !restore_position(&handle, &window) {
                    let _ = snap_corner(window.clone(), "br".into());
                }

                let ev_handle = handle.clone();
                window.on_window_event(move |event| match event {
                    WindowEvent::Moved(pos) => {
                        if let Ok(mut guard) = ev_handle.state::<AppState>().last_position.lock() {
                            *guard = Some((pos.x, pos.y));
                        }
                    }
                    // 用户点到别处就放回桌面层
                    WindowEvent::Focused(false) => sink_if_desktop(&ev_handle),
                    WindowEvent::CloseRequested { .. } | WindowEvent::Destroyed => {
                        persist_position(&ev_handle);
                    }
                    _ => {}
                });
            }

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("启动失败");
}

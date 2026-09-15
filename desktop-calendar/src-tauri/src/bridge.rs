//! 本地 agent 接口：只在 127.0.0.1 上开一个口，让 Claude / MCP / curl 读写日历。
//!
//! 为什么要绕一圈交给前端执行，而不是在这里直接改 calendar.json：
//! 应用运行时的状态在内存里，每次变动 250ms 后整份覆盖写盘。外部若直接写文件，
//! 用户在界面上点一下就把它盖没了。所以这里只负责收发，真正的读写由界面那一个
//! 写入口完成 —— 结果立刻可见，也照样进撤销栈。
//!
//! 安全上有三道：只绑回环地址、每次启动换一个随机 token、不发任何 CORS 头
//! （浏览器里的网页既读不到 token 文件，也过不了预检）。
use std::collections::HashMap;
use std::fs;
use std::io::{BufRead, BufReader, Read, Write};
use std::net::{Shutdown, TcpListener, TcpStream};
use std::path::PathBuf;
use std::sync::mpsc::{channel, Sender};
use std::sync::Mutex;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use serde_json::{json, Value};
use tauri::{AppHandle, Emitter, Manager};

/// 对外契约版本，和前端 agentOps.ts 的 AGENT_API_VERSION 对齐
pub const API_VERSION: u32 = 1;

/// 界面回包的等待上限。正常是毫秒级，超时基本意味着窗口还没加载好。
const REPLY_TIMEOUT: Duration = Duration::from_secs(10);
const IO_TIMEOUT: Duration = Duration::from_secs(10);
const MAX_BODY: usize = 256 * 1024;
const MAX_HEADERS: usize = 64;

#[derive(Default)]
pub struct Bridge {
    /// 等待界面回包的请求，key 由 seq 发号
    pending: Mutex<HashMap<u64, Sender<String>>>,
    seq: Mutex<u64>,
}

/// 锁中毒不该让接口整个瘫掉 —— 里面就是个 HashMap，接着用没问题
fn lock<T>(m: &Mutex<T>) -> std::sync::MutexGuard<'_, T> {
    m.lock().unwrap_or_else(|e| e.into_inner())
}

// ---------- 启动与自描述文件 ----------

pub fn descriptor_path(app: &AppHandle) -> Result<PathBuf, String> {
    Ok(crate::data_dir(app)?.join("bridge.json"))
}

/// 端口和 token 写进数据目录，MCP 端照着连。端口用 0 让系统分配，避免撞端口。
fn write_descriptor(app: &AppHandle, port: u16, token: &str) -> Result<(), String> {
    let body = json!({
        "api": API_VERSION,
        "port": port,
        "token": token,
        "pid": std::process::id(),
        "state": crate::state_path(app)?.to_string_lossy(),
    });
    let text = serde_json::to_string_pretty(&body).map_err(|e| e.to_string())?;
    fs::write(descriptor_path(app)?, text).map_err(|e| format!("写 bridge.json 失败: {e}"))
}

/// 退出时抹掉，免得下次 MCP 照着一个死端口连
pub fn cleanup(app: &AppHandle) {
    if let Ok(p) = descriptor_path(app) {
        let _ = fs::remove_file(p);
    }
}

pub fn start(app: &AppHandle) -> Result<u16, String> {
    let listener =
        TcpListener::bind(("127.0.0.1", 0)).map_err(|e| format!("本地接口监听失败: {e}"))?;
    let port = listener
        .local_addr()
        .map_err(|e| format!("取端口失败: {e}"))?
        .port();
    let token = random_token();
    write_descriptor(app, port, &token)?;

    let handle = app.clone();
    std::thread::spawn(move || {
        // 单线程收，请求天然串行：界面那边就不会有两个操作交叠，
        // 也就不会两次 add 抢到同一个 nextId。
        for stream in listener.incoming().flatten() {
            serve(&handle, &token, stream);
        }
    });
    Ok(port)
}

/// token 的活儿是挡住本机上别的程序顺手一连，以及浏览器里的网页
/// （网页读不到这个文件）。熵取自 RandomState —— 它的种子由系统 RNG 提供。
fn random_token() -> String {
    use std::collections::hash_map::RandomState;
    use std::hash::{BuildHasher, Hasher};

    let nanos = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_nanos() as u64)
        .unwrap_or_default();

    let mut out = String::with_capacity(32);
    for i in 0..4u64 {
        let mut h = RandomState::new().build_hasher();
        h.write_u64(nanos.wrapping_add(i));
        h.write_u32(std::process::id());
        h.write_usize(&out as *const String as usize);
        out.push_str(&format!("{:016x}", h.finish()));
    }
    out
}

// ---------- 极简 HTTP ----------

struct Req {
    method: String,
    target: String,
    headers: HashMap<String, String>,
    body: String,
}

fn read_request(r: &mut BufReader<TcpStream>) -> Result<Req, (u16, String)> {
    let mut line = String::new();
    r.read_line(&mut line)
        .map_err(|e| (400u16, format!("读取请求行失败: {e}")))?;
    let mut parts = line.split_whitespace();
    let method = parts.next().unwrap_or_default().to_ascii_uppercase();
    let target = parts.next().unwrap_or("/").to_string();

    let mut headers = HashMap::new();
    loop {
        let mut h = String::new();
        let n = r
            .read_line(&mut h)
            .map_err(|e| (400u16, format!("读取请求头失败: {e}")))?;
        if n == 0 || h.trim().is_empty() {
            break;
        }
        if let Some((k, v)) = h.split_once(':') {
            headers.insert(k.trim().to_ascii_lowercase(), v.trim().to_string());
        }
        if headers.len() > MAX_HEADERS {
            return Err((431, "请求头太多".into()));
        }
    }

    if let Some(te) = headers.get("transfer-encoding") {
        if te.to_ascii_lowercase().contains("chunked") {
            return Err((411, "请带 Content-Length，这里不收分块传输".into()));
        }
    }
    let len: usize = headers
        .get("content-length")
        .and_then(|v| v.parse().ok())
        .unwrap_or(0);
    if len > MAX_BODY {
        return Err((413, "请求体太大".into()));
    }

    let mut buf = vec![0u8; len];
    if len > 0 {
        r.read_exact(&mut buf)
            .map_err(|e| (400u16, format!("读取正文失败: {e}")))?;
    }
    let body = String::from_utf8(buf).map_err(|_| (400u16, "正文不是 UTF-8".to_string()))?;

    Ok(Req {
        method,
        target,
        headers,
        body,
    })
}

fn respond(out: &mut TcpStream, code: u16, body: &Value) {
    let text = serde_json::to_string(body).unwrap_or_else(|_| "{}".into());
    let reason = match code {
        200 => "OK",
        400 => "Bad Request",
        401 => "Unauthorized",
        404 => "Not Found",
        405 => "Method Not Allowed",
        411 => "Length Required",
        413 => "Payload Too Large",
        431 => "Request Header Fields Too Large",
        503 => "Service Unavailable",
        _ => "Error",
    };
    // 刻意不给任何 Access-Control-* ：网页跨源打不进来
    let head = format!(
        "HTTP/1.1 {code} {reason}\r\nContent-Type: application/json; charset=utf-8\r\n\
         Content-Length: {}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n",
        text.len()
    );
    let _ = out.write_all(head.as_bytes());
    let _ = out.write_all(text.as_bytes());
    let _ = out.flush();
    let _ = out.shutdown(Shutdown::Both);
}

fn serve(app: &AppHandle, token: &str, stream: TcpStream) {
    let _ = stream.set_read_timeout(Some(IO_TIMEOUT));
    let _ = stream.set_write_timeout(Some(IO_TIMEOUT));
    let Ok(mut out) = stream.try_clone() else {
        return;
    };
    let mut reader = BufReader::new(stream);

    let req = match read_request(&mut reader) {
        Ok(r) => r,
        Err((code, msg)) => return respond(&mut out, code, &json!({ "error": msg })),
    };

    if req.headers.get("x-calendar-token").map(String::as_str) != Some(token) {
        return respond(
            &mut out,
            401,
            &json!({"error": "缺少或不匹配的 X-Calendar-Token，token 在数据目录的 bridge.json 里"}),
        );
    }

    let path = req.target.split('?').next().unwrap_or("/");
    let op = match (req.method.as_str(), path) {
        ("GET", "/health") => json!({ "op": "health" }),
        ("POST", "/rpc") => match serde_json::from_str::<Value>(&req.body) {
            Ok(v) => v,
            Err(e) => {
                return respond(&mut out, 400, &json!({"error": format!("正文不是合法 JSON: {e}")}))
            }
        },
        _ => {
            return respond(
                &mut out,
                404,
                &json!({"error": "只认 GET /health 和 POST /rpc"}),
            )
        }
    };

    match call_ui(app, op) {
        Ok(v) => {
            let code = if v.get("error").is_some() { 400 } else { 200 };
            respond(&mut out, code, &v);
        }
        // retryWithFile：界面这条路走不通时，告诉调用方可以退回直接读写 calendar.json，
        // 免得「日历明明开着」反而比关着还不如。
        Err(e) => respond(&mut out, 503, &json!({ "error": e, "retryWithFile": true })),
    }
}

// ---------- 转给界面执行 ----------

fn call_ui(app: &AppHandle, op: Value) -> Result<Value, String> {
    let bridge = app.state::<Bridge>();
    let id = {
        let mut seq = lock(&bridge.seq);
        *seq += 1;
        *seq
    };

    let (tx, rx) = channel();
    lock(&bridge.pending).insert(id, tx);

    let sent = app.emit("agent-request", json!({ "id": id, "op": op }));
    if let Err(e) = sent {
        lock(&bridge.pending).remove(&id);
        return Err(format!("日历窗口还没就绪: {e}"));
    }

    let got = rx.recv_timeout(REPLY_TIMEOUT);
    lock(&bridge.pending).remove(&id);

    match got {
        Ok(text) => serde_json::from_str(&text).map_err(|e| format!("界面回包解析失败: {e}")),
        Err(_) => Err("日历窗口没有在限时内响应".into()),
    }
}

#[tauri::command]
pub fn agent_reply(app: AppHandle, id: u64, payload: String) {
    let tx = lock(&app.state::<Bridge>().pending).remove(&id);
    if let Some(tx) = tx {
        let _ = tx.send(payload);
    }
}

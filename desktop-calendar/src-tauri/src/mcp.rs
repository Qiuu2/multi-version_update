//! MCP 服务器模式：`LocalCalendar --mcp` 进这里，不开窗口，在 stdio 上说 JSON-RPC。
//!
//! 这样接 Claude / 别的 agent 不用另装 Node —— 装好的那个 exe 自己就是 MCP server。
//!
//! 两种后端，自动切：
//!   * 日历正开着 → 走 127.0.0.1 上的本地接口，界面立刻刷新，且能撤销；
//!   * 日历没开   → 直接读写 calendar.json，下次启动就能看到。
//!
//! 本模块刻意不依赖 tauri，好让它能单独编译和测试。
use std::io::{BufRead, Read, Write};
use std::net::TcpStream;
use std::path::{Path, PathBuf};
use std::time::Duration;

use serde_json::{json, Map, Value};

const APP_ID: &str = "com.local.calendar";
const SERVER_NAME: &str = "local-calendar";
const SERVER_VERSION: &str = env!("CARGO_PKG_VERSION");
const DEFAULT_PROTOCOL: &str = "2025-06-18";
const HTTP_TIMEOUT: Duration = Duration::from_secs(15);

const INSTRUCTIONS: &str = "本地日历。日期一律用 YYYY-MM-DD，时间用 HH:MM。\
给了具体时间的当「日程」，没给的当「任务」；不填日期就落到收集箱等待安排。\
写操作在日历开着时立刻生效，用户可以点提示条上的「撤销」或标题栏的撤销按钮撤回。";

// ---------- 定位数据目录 ----------

/// 与 Tauri 的 app_data_dir() 同一套约定：Windows 走 %APPDATA%
fn data_dir() -> Option<PathBuf> {
    #[cfg(windows)]
    let base = std::env::var_os("APPDATA").map(PathBuf::from);
    #[cfg(target_os = "macos")]
    let base = std::env::var_os("HOME")
        .map(|v| PathBuf::from(v).join("Library").join("Application Support"));
    #[cfg(all(unix, not(target_os = "macos")))]
    let base = std::env::var_os("XDG_DATA_HOME")
        .map(PathBuf::from)
        .or_else(|| std::env::var_os("HOME").map(|v| PathBuf::from(v).join(".local").join("share")));

    base.map(|b| b.join(APP_ID))
}

struct Desc {
    port: u16,
    token: String,
    state: PathBuf,
}

fn read_descriptor(dir: &Path) -> Option<Desc> {
    let raw = std::fs::read_to_string(dir.join("bridge.json")).ok()?;
    let v: Value = serde_json::from_str(&raw).ok()?;
    Some(Desc {
        port: u16::try_from(v.get("port")?.as_u64()?).ok()?,
        token: v.get("token")?.as_str()?.to_string(),
        state: v
            .get("state")
            .and_then(Value::as_str)
            .map(PathBuf::from)
            .unwrap_or_else(|| dir.join("calendar.json")),
    })
}

// ---------- 极简 HTTP 客户端 ----------

/// 只发一个请求就收工：带 Connection: close，读到 EOF 为止，不必解析 Content-Length。
fn http_rpc(port: u16, token: &str, body: &str) -> Result<String, String> {
    let mut sock = TcpStream::connect(("127.0.0.1", port)).map_err(|e| format!("连不上: {e}"))?;
    sock.set_read_timeout(Some(HTTP_TIMEOUT)).ok();
    sock.set_write_timeout(Some(HTTP_TIMEOUT)).ok();

    let req = format!(
        "POST /rpc HTTP/1.1\r\nHost: 127.0.0.1:{port}\r\nX-Calendar-Token: {token}\r\n\
         Content-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{body}",
        body.len()
    );
    sock.write_all(req.as_bytes())
        .map_err(|e| format!("发送失败: {e}"))?;

    let mut raw = Vec::new();
    sock.read_to_end(&mut raw)
        .map_err(|e| format!("读取失败: {e}"))?;

    let sep = raw
        .windows(4)
        .position(|w| w == b"\r\n\r\n")
        .ok_or("响应不完整")?;
    String::from_utf8(raw[sep + 4..].to_vec()).map_err(|e| format!("响应不是 UTF-8: {e}"))
}

// ---------- 调用：先试运行中的日历，再退到文件 ----------

pub fn call(op: Value) -> Value {
    let Some(dir) = data_dir() else {
        return json!({ "error": "找不到日历的数据目录（HOME / APPDATA 没设）" });
    };
    let desc = read_descriptor(&dir);

    if let Some(d) = &desc {
        if let Ok(text) = http_rpc(d.port, &d.token, &op.to_string()) {
            let parsed: Value = match serde_json::from_str(&text) {
                Ok(v) => v,
                Err(e) => return json!({ "error": format!("日历返回的内容解析不了: {e}") }),
            };
            // 日历在跑但界面没应答（窗口还没加载好之类）：与其报错，不如落到文件里，
            // 用户下次打开照样能看到。
            if parsed.get("retryWithFile").and_then(Value::as_bool) != Some(true) {
                return parsed;
            }
        }
        // 连不上就是应用没开着（或 bridge.json 过期），同样退到文件模式
    }

    let path = desc
        .map(|d| d.state)
        .unwrap_or_else(|| dir.join("calendar.json"));
    file_call(&path, op)
}

// ---------- 文件后端 ----------

fn file_load(p: &Path) -> Value {
    std::fs::read_to_string(p)
        .ok()
        .and_then(|s| serde_json::from_str::<Value>(&s).ok())
        .filter(Value::is_object)
        .unwrap_or_else(|| json!({ "items": [], "nextId": 1, "groups": [], "hidden": {} }))
}

fn file_save(p: &Path, doc: &Value) -> Result<(), String> {
    if let Some(dir) = p.parent() {
        std::fs::create_dir_all(dir).map_err(|e| format!("建目录失败: {e}"))?;
    }
    let text = serde_json::to_string_pretty(doc).map_err(|e| e.to_string())?;
    let tmp = p.with_extension("json.tmp");
    std::fs::write(&tmp, text).map_err(|e| format!("写入失败: {e}"))?;
    std::fs::rename(&tmp, p).map_err(|e| format!("落盘失败: {e}"))
}

fn items_of(doc: &Value) -> Vec<Value> {
    doc.get("items")
        .and_then(Value::as_array)
        .cloned()
        .unwrap_or_default()
}

fn s(v: Option<&Value>) -> String {
    v.and_then(Value::as_str).unwrap_or("").trim().to_string()
}

fn is_date(v: &str) -> bool {
    let b = v.as_bytes();
    b.len() == 10
        && b[4] == b'-'
        && b[7] == b'-'
        && b.iter()
            .enumerate()
            .all(|(i, c)| i == 4 || i == 7 || c.is_ascii_digit())
}

fn is_time(v: &str) -> bool {
    match v.split_once(':') {
        Some((h, m)) => {
            matches!(h.parse::<u32>(), Ok(n) if n < 24 && !h.is_empty() && h.len() <= 2)
                && m.len() == 2
                && matches!(m.parse::<u32>(), Ok(n) if n < 60)
        }
        None => false,
    }
}

/// 与前端 agentOps.ts 的 readFields 同规则：只认显式出现的键。
fn read_fields(src: &Map<String, Value>) -> Result<Map<String, Value>, String> {
    let mut out = Map::new();

    if src.contains_key("title") {
        let t = s(src.get("title"));
        if t.is_empty() {
            return Err("title 不能为空".into());
        }
        out.insert("title".into(), json!(t));
    }
    if src.contains_key("date") {
        let d = s(src.get("date"));
        if !d.is_empty() && !is_date(&d) {
            return Err(format!("date 要写成 YYYY-MM-DD，收到「{d}」"));
        }
        out.insert("date".into(), json!(d));
    }
    if src.contains_key("time") {
        let t = s(src.get("time"));
        if !t.is_empty() && !is_time(&t) {
            return Err(format!("time 要写成 HH:MM，收到「{t}」"));
        }
        out.insert("withTime".into(), json!(!t.is_empty()));
        out.insert(
            "time".into(),
            json!(if t.is_empty() { "00:00".into() } else { t }),
        );
    }
    if src.contains_key("type") {
        let v = s(src.get("type"));
        if v != "task" && v != "event" {
            return Err("type 只能是 task 或 event".into());
        }
        out.insert("type".into(), json!(v));
    }
    if src.contains_key("priority") {
        let v = s(src.get("priority"));
        if !["", "低", "中", "高"].contains(&v.as_str()) {
            return Err("priority 只能是 低 / 中 / 高，或留空".into());
        }
        out.insert("priority".into(), json!(v));
    }
    for k in ["group", "notes", "location", "reminder"] {
        if src.contains_key(k) {
            out.insert(k.into(), json!(s(src.get(k))));
        }
    }
    if src.contains_key("done") {
        out.insert(
            "done".into(),
            json!(src.get("done").and_then(Value::as_bool).unwrap_or(false)),
        );
    }
    Ok(out)
}

fn public_item(t: &Value) -> Value {
    let time = s(t.get("time"));
    json!({
        "id": t.get("id").and_then(Value::as_i64).unwrap_or(0),
        "type": if s(t.get("type")).is_empty() { "task".into() } else { s(t.get("type")) },
        "group": s(t.get("group")),
        "title": s(t.get("title")),
        "date": s(t.get("date")),
        "time": if time.is_empty() { "00:00".to_string() } else { time },
        "withTime": t.get("withTime").and_then(Value::as_bool).unwrap_or(false),
        "done": t.get("done").and_then(Value::as_bool).unwrap_or(false),
        "priority": s(t.get("priority")),
        "location": s(t.get("location")),
        "reminder": s(t.get("reminder")),
        "notes": s(t.get("notes")),
    })
}

/// 分组不存在就补一个；归档过的重新启用 —— 和界面里 ensureGroup 的规则一致
fn ensure_group(doc: &mut Value, name: &str) {
    if name.is_empty() {
        return;
    }
    const COLORS: [&str; 6] = ["#4C6FFF", "#00A67E", "#F5A524", "#E5484D", "#8B5CF6", "#0EA5E9"];
    let groups = doc
        .get_mut("groups")
        .filter(|g| g.is_array())
        .map(|g| g.as_array_mut().expect("已判定是数组"));
    let Some(groups) = groups else {
        doc["groups"] = json!([{ "name": name, "color": COLORS[0] }]);
        return;
    };
    if let Some(g) = groups.iter_mut().find(|g| s(g.get("name")) == name) {
        if g.get("archived").and_then(Value::as_bool) == Some(true) {
            g["archived"] = json!(false);
        }
        return;
    }
    let color = COLORS[groups.len() % COLORS.len()];
    groups.push(json!({ "name": name, "color": color }));

    // 分组一旦被手动隐藏过，新条目会当场被过滤掉，看着就是「没加上」
    if let Some(h) = doc.get_mut("hidden").and_then(Value::as_object_mut) {
        h.remove(name);
    }
}

/// 报错时把用户传进来的 id 原样念回去，好让对面知道是哪个参数写错了
fn id_label(op: &Value) -> String {
    match op.get("id") {
        Some(Value::Number(n)) => n.to_string(),
        Some(Value::String(v)) => v.clone(),
        _ => "(未提供)".into(),
    }
}

fn sort_key(t: &Value) -> (bool, String, String, i64) {
    let date = s(t.get("date"));
    (
        date.is_empty(), // 无期限的排最后
        date,
        s(t.get("time")),
        t.get("id").and_then(Value::as_i64).unwrap_or(0),
    )
}

pub fn file_call(path: &Path, op: Value) -> Value {
    let what = s(op.get("op"));
    let empty = Map::new();
    let top = op.as_object().unwrap_or(&empty);
    let mut doc = file_load(path);

    match what.as_str() {
        "health" => json!({
            "ok": true, "app": SERVER_NAME, "api": 1, "mode": "file",
            "items": items_of(&doc).len(),
            "path": path.to_string_lossy(),
            "note": "日历界面当前没在应答（多半是没运行），改动直接写进文件，下次打开生效",
        }),

        "groups" => json!({ "groups": doc.get("groups").cloned().unwrap_or_else(|| json!([])) }),

        "list" => {
            let from = s(op.get("from"));
            let to = s(op.get("to"));
            if !from.is_empty() && !is_date(&from) {
                return json!({ "error": format!("from 要写成 YYYY-MM-DD，收到「{from}」") });
            }
            if !to.is_empty() && !is_date(&to) {
                return json!({ "error": format!("to 要写成 YYYY-MM-DD，收到「{to}」") });
            }
            let group = s(op.get("group"));
            let query = s(op.get("query")).to_lowercase();
            let include_done = op
                .get("includeDone")
                .and_then(Value::as_bool)
                .unwrap_or(true);
            let undated_only = op
                .get("undatedOnly")
                .and_then(Value::as_bool)
                .unwrap_or(false);

            let mut hit: Vec<Value> = items_of(&doc)
                .into_iter()
                .filter(|t| {
                    let date = s(t.get("date"));
                    if !include_done && t.get("done").and_then(Value::as_bool) == Some(true) {
                        return false;
                    }
                    if undated_only {
                        if !date.is_empty() {
                            return false;
                        }
                    } else {
                        if !from.is_empty() && (date.is_empty() || date < from) {
                            return false;
                        }
                        if !to.is_empty() && (date.is_empty() || date > to) {
                            return false;
                        }
                    }
                    if !group.is_empty() && s(t.get("group")) != group {
                        return false;
                    }
                    if !query.is_empty() {
                        let hay = format!("{} {}", s(t.get("title")), s(t.get("notes")))
                            .to_lowercase();
                        if !hay.contains(&query) {
                            return false;
                        }
                    }
                    true
                })
                .collect();
            hit.sort_by_key(sort_key);

            let total = hit.len();
            let limit = op
                .get("limit")
                .and_then(Value::as_u64)
                .filter(|n| *n > 0)
                .unwrap_or(200) as usize;
            json!({
                "total": total,
                "items": hit.iter().take(limit).map(public_item).collect::<Vec<_>>(),
            })
        }

        "add" => {
            let src = op
                .get("item")
                .and_then(Value::as_object)
                .unwrap_or(top)
                .clone();
            let fields = match read_fields(&src) {
                Ok(f) => f,
                Err(e) => return json!({ "error": e }),
            };
            if !fields.contains_key("title") {
                return json!({ "error": "title 不能为空" });
            }

            let id = doc.get("nextId").and_then(Value::as_i64).unwrap_or(1).max(1);
            let mut item = json!({
                "id": id, "type": "task", "group": "", "title": "",
                "date": "", "time": "00:00", "withTime": false,
            });
            let obj = item.as_object_mut().expect("刚构造的就是对象");
            for (k, v) in &fields {
                obj.insert(k.clone(), v.clone());
            }
            // 没指定类型时：给了具体时间的当日程，其余当任务
            if !src.contains_key("type") {
                let ev = obj.get("withTime").and_then(Value::as_bool) == Some(true)
                    && !s(obj.get("date")).is_empty();
                obj.insert("type".into(), json!(if ev { "event" } else { "task" }));
            }

            let group = s(item.get("group"));
            ensure_group(&mut doc, &group);
            let mut items = items_of(&doc);
            items.push(item.clone());
            doc["items"] = json!(items);
            doc["nextId"] = json!(id + 1);

            match file_save(path, &doc) {
                Ok(()) => json!({ "id": id, "item": public_item(&item) }),
                Err(e) => json!({ "error": e }),
            }
        }

        "update" => {
            let id = op.get("id").and_then(Value::as_i64);
            let mut items = items_of(&doc);
            let at = id.and_then(|id| {
                items
                    .iter()
                    .position(|t| t.get("id").and_then(Value::as_i64) == Some(id))
            });
            let Some(at) = at else {
                return json!({ "error": format!("没有 id={} 的条目", id_label(&op)) });
            };
            let src = op
                .get("patch")
                .and_then(Value::as_object)
                .unwrap_or(top)
                .clone();
            let fields = match read_fields(&src) {
                Ok(f) => f,
                Err(e) => return json!({ "error": e }),
            };
            if fields.is_empty() {
                return json!({ "error": "没有要修改的字段" });
            }

            let obj = items[at].as_object_mut().expect("条目是对象");
            for (k, v) in &fields {
                obj.insert(k.clone(), v.clone());
            }
            let updated = items[at].clone();
            let group = s(updated.get("group"));
            ensure_group(&mut doc, &group);
            doc["items"] = json!(items);

            match file_save(path, &doc) {
                Ok(()) => json!({ "item": public_item(&updated) }),
                Err(e) => json!({ "error": e }),
            }
        }

        "delete" => {
            let id = op.get("id").and_then(Value::as_i64);
            let mut items = items_of(&doc);
            let at = id.and_then(|id| {
                items
                    .iter()
                    .position(|t| t.get("id").and_then(Value::as_i64) == Some(id))
            });
            let Some(at) = at else {
                return json!({ "error": format!("没有 id={} 的条目", id_label(&op)) });
            };
            let gone = items.remove(at);
            doc["items"] = json!(items);
            match file_save(path, &doc) {
                Ok(()) => json!({ "deleted": public_item(&gone) }),
                Err(e) => json!({ "error": e }),
            }
        }

        "" => json!({ "error": "缺少 op 字段" }),
        other => json!({ "error": format!("不认识的 op「{other}」") }),
    }
}

// ---------- MCP ----------

fn text_field(desc: &str) -> Value {
    json!({ "type": "string", "description": desc })
}

fn task_fields(with_done: bool) -> Value {
    let mut p = Map::new();
    p.insert("title".into(), text_field("标题"));
    p.insert("date".into(), text_field("日期 YYYY-MM-DD；留空表示无期限，落到收集箱"));
    p.insert("time".into(), text_field("时间 HH:MM；给了就是定点日程，留空是全天"));
    p.insert("group".into(), text_field("所属项目名；不存在会自动新建"));
    p.insert("notes".into(), text_field("备注"));
    p.insert("location".into(), text_field("地点"));
    p.insert("reminder".into(), text_field("提醒，如「提前 10 分钟」"));
    p.insert(
        "priority".into(),
        json!({ "type": "string", "enum": ["", "低", "中", "高"], "description": "优先级" }),
    );
    p.insert(
        "type".into(),
        json!({ "type": "string", "enum": ["task", "event"], "description": "不填则按有无时间自动判断" }),
    );
    if with_done {
        p.insert("done".into(), json!({ "type": "boolean", "description": "是否已完成" }));
    }
    Value::Object(p)
}

fn with_id(mut fields: Value, required: Vec<&str>) -> Value {
    if let Some(p) = fields.as_object_mut() {
        p.insert(
            "id".into(),
            json!({ "type": "integer", "description": "条目 id，先用 list_tasks 查" }),
        );
    }
    json!({ "type": "object", "properties": fields, "required": required })
}

fn tools() -> Value {
    json!([
        {
            "name": "list_tasks",
            "description": "列出日历里的任务与日程，可按日期区间、项目、关键字筛选。",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "from": text_field("起始日期 YYYY-MM-DD（含）"),
                    "to": text_field("结束日期 YYYY-MM-DD（含）"),
                    "group": text_field("只看这个项目"),
                    "query": text_field("标题或备注里的关键字"),
                    "includeDone": { "type": "boolean", "description": "是否含已完成，默认 true" },
                    "undatedOnly": { "type": "boolean", "description": "只看收集箱里无期限的" },
                    "limit": { "type": "integer", "description": "最多返回多少条，默认 200" }
                }
            }
        },
        {
            "name": "add_task",
            "description": "往日历里加一条任务或日程。不填日期就放进收集箱等待安排。",
            "inputSchema": { "type": "object", "properties": task_fields(false), "required": ["title"] }
        },
        {
            "name": "update_task",
            "description": "按 id 修改条目，只改传进来的字段。",
            "inputSchema": with_id(task_fields(true), vec!["id"])
        },
        {
            "name": "complete_task",
            "description": "把条目标记为完成（done 传 false 可以改回未完成）。",
            "inputSchema": json!({
                "type": "object",
                "properties": {
                    "id": { "type": "integer", "description": "条目 id" },
                    "done": { "type": "boolean", "description": "默认 true" }
                },
                "required": ["id"]
            })
        },
        {
            "name": "delete_task",
            "description": "按 id 删除条目。日历开着时删掉的东西用户还能撤销回来。",
            "inputSchema": json!({
                "type": "object",
                "properties": { "id": { "type": "integer", "description": "条目 id" } },
                "required": ["id"]
            })
        },
        { "name": "list_groups", "description": "列出所有项目（分组）及其颜色、是否已归档。",
          "inputSchema": { "type": "object", "properties": {} } },
        { "name": "calendar_status", "description": "看日历是否正在运行、数据在哪、有多少条。",
          "inputSchema": { "type": "object", "properties": {} } }
    ])
}

fn tool_to_op(name: &str, args: &Value) -> Result<Value, String> {
    let mut obj = args.as_object().cloned().unwrap_or_default();
    let op = match name {
        "list_tasks" => "list",
        "add_task" => "add",
        "update_task" => "update",
        "complete_task" => {
            obj.entry("done").or_insert_with(|| json!(true));
            "update"
        }
        "delete_task" => "delete",
        "list_groups" => "groups",
        "calendar_status" => "health",
        _ => return Err(format!("没有名为「{name}」的工具")),
    };
    obj.insert("op".into(), json!(op));
    Ok(Value::Object(obj))
}

pub fn handle(method: &str, params: &Value) -> Result<Value, (i64, String)> {
    match method {
        "initialize" => {
            let protocol = params
                .get("protocolVersion")
                .and_then(Value::as_str)
                .filter(|v| !v.is_empty())
                .unwrap_or(DEFAULT_PROTOCOL);
            Ok(json!({
                "protocolVersion": protocol,
                "capabilities": { "tools": {} },
                "serverInfo": { "name": SERVER_NAME, "version": SERVER_VERSION },
                "instructions": INSTRUCTIONS,
            }))
        }
        "ping" => Ok(json!({})),
        "tools/list" => Ok(json!({ "tools": tools() })),
        "resources/list" => Ok(json!({ "resources": [] })),
        "prompts/list" => Ok(json!({ "prompts": [] })),
        "tools/call" => {
            let name = params.get("name").and_then(Value::as_str).unwrap_or("");
            let args = params.get("arguments").cloned().unwrap_or_else(|| json!({}));
            let op = tool_to_op(name, &args).map_err(|e| (-32602, e))?;
            let out = call(op);
            Ok(json!({
                "content": [{ "type": "text", "text": serde_json::to_string_pretty(&out).unwrap_or_default() }],
                "isError": out.get("error").is_some(),
            }))
        }
        other => Err((-32601, format!("不支持的方法「{other}」"))),
    }
}

/// 一行一条 JSON，这是 MCP stdio 传输的约定
pub fn serve<R: BufRead, W: Write>(input: R, mut out: W) {
    for line in input.lines() {
        let Ok(line) = line else { break };
        let line = line.trim();
        if line.is_empty() {
            continue;
        }

        let msg: Value = match serde_json::from_str(line) {
            Ok(v) => v,
            Err(e) => {
                let err = json!({"jsonrpc":"2.0","id":Value::Null,
                    "error":{"code":-32700,"message":format!("JSON 解析失败: {e}")}});
                if writeln!(out, "{err}").is_err() {
                    return;
                }
                let _ = out.flush();
                continue;
            }
        };

        // 没有 id 的是通知（notifications/initialized 等），照规矩不回包
        let Some(id) = msg.get("id").cloned().filter(|v| !v.is_null()) else {
            continue;
        };
        let method = msg.get("method").and_then(Value::as_str).unwrap_or("");
        let params = msg.get("params").cloned().unwrap_or_else(|| json!({}));

        let reply = match handle(method, &params) {
            Ok(result) => json!({ "jsonrpc": "2.0", "id": id, "result": result }),
            Err((code, message)) => {
                json!({ "jsonrpc": "2.0", "id": id, "error": { "code": code, "message": message } })
            }
        };
        if writeln!(out, "{reply}").is_err() {
            return;
        }
        let _ = out.flush();
    }
}

pub fn run() -> i32 {
    let stdin = std::io::stdin();
    let lock = stdin.lock();
    serve(lock, std::io::stdout());
    0
}

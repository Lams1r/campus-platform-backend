from __future__ import annotations

import json
import os
import subprocess
import threading
import time
from html import escape as escape_text
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from campus_tests.dashboard import write_placeholder_dashboard, write_progress_dashboard, write_reports_index


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "reports"
LOG_FILE = REPORT_DIR / "web-runner.log"
STATE_FILE = REPORT_DIR / "web-runner-state.json"
PROFILES = {
    "smoke": "冒烟测试",
    "blackbox": "黑盒部署测试",
    "deployment": "部署质量测试",
    "frontend": "前端页面测试",
    "contract": "契约测试",
    "negative": "负向输入测试",
    "business": "业务模块测试",
    "api": "接口测试",
    "rbac": "权限测试",
    "security": "安全测试",
    "performance": "性能测试",
    "full": "全量测试",
}

PROFILE_SECTIONS = [
    ("基础验收", "部署后先看这里，确认前端、路由、反代和基础接口是否可用。", ["smoke", "blackbox", "deployment", "frontend"]),
    ("接口与业务", "接口契约、业务读取、登录态和角色权限集中放在这里。", ["contract", "business", "api", "rbac"]),
    ("安全稳定与完整验收", "异常输入、安全基线、响应耗时、并发稳定性和最终全量验收集中放在这里。", ["negative", "security", "performance", "full"]),
]

TEST_CASE_COUNTS = {
    "smoke": 13,
    "blackbox": 10,
    "deployment": 4,
    "frontend": 20,
    "contract": 3,
    "negative": 12,
    "business": 3,
    "api": 40,
    "rbac": 9,
    "security": 8,
    "performance": 5,
    "full": 127,
}

TOTAL_TEST_CASES = TEST_CASE_COUNTS["full"]

PROFILE_DETAILS = {
    "smoke": {
        "tag": "最快验证",
        "what": "确认系统是否活着：前端首页、核心页面、验证码和公共列表接口。",
        "how": "通过 HTTP GET 访问公开页面和公开 API，校验状态码、HTML 应用壳和统一 JSON 返回。",
        "when": "部署后第一步，或发现网站打不开时先跑它。",
    },
    "blackbox": {
        "tag": "公网视角",
        "what": "模拟外部用户访问，检查前端、API、Nginx 转发、SPA 子路由刷新。",
        "how": "只通过公网 URL 发请求，不登录服务器、不连数据库、不依赖源码。",
        "when": "适合验收队友部署是否真的可用。",
    },
    "deployment": {
        "tag": "上线配置",
        "what": "检查静态资源、Content-Type、API 错误页、CORS 预检、部署路径质量。",
        "how": "请求 JS/CSS 资源、OPTIONS 预检、缺失 API 路径，判断是否被错误转发或返回 HTML。",
        "when": "适合 Nginx、域名、反代、前后端同域配置完成后运行。",
    },
    "frontend": {
        "tag": "页面巡检",
        "what": "检查登录、注册、工作台、系统管理、教学管理、校园服务等页面是否能打开。",
        "how": "访问 Vue SPA 各个路由，验证返回应用壳、基础 HTML、构建资源是否存在。",
        "when": "适合 UI 改版后确认页面没有刷新 404 或资源丢失。",
    },
    "contract": {
        "tag": "返回格式",
        "what": "检查接口统一响应 `{code,msg,data}`、分页结构、验证码结构、时间格式。",
        "how": "调用公开和登录态接口，解析 JSON，断言关键字段和不含 `T` 时间格式。",
        "when": "适合后端接口变更后确认前端还能稳定消费数据。",
    },
    "negative": {
        "tag": "异常输入",
        "what": "检查非法分页、错误 token、疑似 SQL/XSS/路径穿越输入、错误 HTTP 方法。",
        "how": "发送非破坏性异常参数，要求服务受控返回，不能 500、不能泄露堆栈。",
        "when": "适合检查系统健壮性和基础安全防护。",
    },
    "business": {
        "tag": "业务读取",
        "what": "集中检查公告、图书、报修、校园卡、宿舍、课程、考勤、请假、消息等模块。",
        "how": "只跑 GET 或无害读取接口，不执行删除、审核、充值等会改数据的操作。",
        "when": "适合上线验收主要业务功能是否还通。",
    },
    "api": {
        "tag": "接口巡检",
        "what": "覆盖公开接口、登录态读取接口、业务接口和时间格式检查。",
        "how": "批量调用后端 API，校验 HTTP 状态、统一响应体、字段结构和异常受控。",
        "when": "适合快速判断后端整体健康度。",
    },
    "rbac": {
        "tag": "权限边界",
        "what": "验证匿名访问、错误 token、学生访问管理员接口等权限隔离。",
        "how": "用无 token、坏 token、学生 token、管理员 token 分别访问受保护接口。",
        "when": "适合账号权限、角色菜单、Sa-Token 配置变更后运行。",
    },
    "security": {
        "tag": "安全基线",
        "what": "检查 Actuator、Swagger、配置文件路径、错误堆栈、安全响应头等风险。",
        "how": "请求敏感路径和错误路径，验证不可公开、不可泄露 Java/Spring 堆栈。",
        "when": "适合上线前或生产配置调整后运行。",
    },
    "performance": {
        "tag": "响应耗时",
        "what": "检查首页、验证码、公告接口和少量并发访问的响应稳定性。",
        "how": "重复请求并统计平均/最大耗时，同时用小并发检测间歇性 500。",
        "when": "适合网络、服务器、数据库或 Redis 调整后运行。",
    },
    "full": {
        "tag": "完整验收",
        "what": "执行全部已配置测试，输出完整可视化报告和失败分析。",
        "how": "按 pytest 全量执行所有测试文件，自动生成实时进度页和最终分析看板。",
        "when": "适合最终验收、每日定时巡检或提交测试报告前运行。",
    },
}

CURRENT_PROCESS: subprocess.Popen[str] | None = None
CURRENT_PROFILE = ""
CURRENT_STARTED_AT = 0.0
CURRENT_FINISHED_AT = 0.0
LOCK = threading.Lock()


def profile_report_dir(profile: str) -> Path:
    return REPORT_DIR / (profile or "full")


def load_env() -> dict[str, str]:
    env = os.environ.copy()
    env_file = ROOT / ".env"
    if not env_file.exists():
        return env
    for raw_line in env_file.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if not env.get(key):
            env[key] = value
        if not os.environ.get(key):
            os.environ[key] = value
    return env


ENV = load_env()
TARGET_BASE_URL = ENV.get("TARGET_BASE_URL", "http://8.138.10.124")
API_BASE_URL = ENV.get("API_BASE_URL", "http://114.132.77.204:8080/api")
RUNNER_TOKEN = ENV.get("WEB_RUNNER_TOKEN", "").strip()


class RunnerHandler(BaseHTTPRequestHandler):
    server_version = "CampusTestRunner/1.0"

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path in {"/", "/index.html"}:
            self._send_html(render_page())
            return
        if parsed.path == "/status":
            self._send_json(current_state())
            return
        if parsed.path == "/log":
            self._send_text(read_log())
            return
        self.send_error(404, "Not found")

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path != "/run":
            self.send_error(404, "Not found")
            return
        if RUNNER_TOKEN and self.headers.get("X-Runner-Token", "") != RUNNER_TOKEN:
            self._send_json({"ok": False, "message": "访问令牌不正确"}, status=403)
            return

        length = int(self.headers.get("content-length", "0") or 0)
        body = self.rfile.read(length).decode("utf-8")
        data = parse_qs(body)
        profile = data.get("profile", [""])[0]
        strict = data.get("strict", ["false"])[0] == "true"

        ok, message = start_test(profile, strict=strict)
        self._send_json({"ok": ok, "message": message, "state": current_state()})

    def log_message(self, format: str, *args: object) -> None:
        return

    def _send_html(self, content: str) -> None:
        encoded = content.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def _send_json(self, data: dict[str, object], status: int = 200) -> None:
        encoded = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def _send_text(self, content: str) -> None:
        encoded = content.encode("utf-8", errors="replace")
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)


def start_test(profile: str, *, strict: bool = False) -> tuple[bool, str]:
    global CURRENT_PROCESS, CURRENT_PROFILE, CURRENT_STARTED_AT, CURRENT_FINISHED_AT

    if profile not in PROFILES:
        return False, "未知测试类型"

    with LOCK:
        if CURRENT_PROCESS and CURRENT_PROCESS.poll() is None:
            return False, f"{PROFILES.get(CURRENT_PROFILE, CURRENT_PROFILE)} 正在运行，请稍后"

        profile_report_dir(profile).mkdir(parents=True, exist_ok=True)
        command = [
            "bash",
            "scripts/run_tests.sh",
            "--target",
            TARGET_BASE_URL,
            "--api",
            API_BASE_URL,
            "--profile",
            profile,
        ]
        if strict:
            command.append("--strict")

        log_handle = LOG_FILE.open("a", encoding="utf-8")
        log_handle.write(f"\n\n[{time.strftime('%Y-%m-%d %H:%M:%S')}] START {' '.join(command)}\n")
        log_handle.flush()

        CURRENT_PROFILE = profile
        CURRENT_STARTED_AT = time.time()
        CURRENT_FINISHED_AT = 0.0
        CURRENT_PROCESS = subprocess.Popen(
            command,
            cwd=ROOT,
            env=ENV,
            stdout=log_handle,
            stderr=subprocess.STDOUT,
            text=True,
        )
        write_state()
        threading.Thread(target=watch_process, args=(CURRENT_PROCESS, log_handle), daemon=True).start()
        return True, f"已开始执行：{PROFILES[profile]}"


def watch_process(process: subprocess.Popen[str], log_handle) -> None:  # type: ignore[no-untyped-def]
    global CURRENT_FINISHED_AT
    try:
        process.wait()
        CURRENT_FINISHED_AT = time.time()
        log_handle.write(f"\n[{time.strftime('%Y-%m-%d %H:%M:%S')}] END exit_code={process.returncode}\n")
        log_handle.flush()
    finally:
        log_handle.close()
        write_state()


def current_state() -> dict[str, object]:
    process = CURRENT_PROCESS
    running = bool(process and process.poll() is None)
    exit_code = None if not process else process.poll()
    profile = CURRENT_PROFILE or "full"
    report_dir = profile_report_dir(profile)
    latest_dashboard = report_dir / "latest-dashboard.html"
    latest_live = report_dir / "latest-live.html"
    if not CURRENT_STARTED_AT:
        elapsed_seconds = 0
    elif running:
        elapsed_seconds = round(time.time() - CURRENT_STARTED_AT, 1)
    else:
        finished_at = CURRENT_FINISHED_AT or time.time()
        elapsed_seconds = round(max(0, finished_at - CURRENT_STARTED_AT), 1)
    return {
        "running": running,
        "profile": CURRENT_PROFILE,
        "profileLabel": PROFILES.get(CURRENT_PROFILE, ""),
        "startedAt": CURRENT_STARTED_AT,
        "finishedAt": CURRENT_FINISHED_AT,
        "elapsedSeconds": elapsed_seconds,
        "exitCode": exit_code,
        "target": TARGET_BASE_URL,
        "api": API_BASE_URL,
        "dashboardReady": latest_dashboard.exists(),
        "liveReady": latest_live.exists(),
        "dashboardUrl": f"/campus-tests/{profile}/latest-dashboard.html",
        "liveUrl": f"/campus-tests/{profile}/latest-live.html",
        "reportsIndexUrl": "/campus-tests/index.html",
        "logTail": read_log()[-8000:],
    }


def write_state() -> None:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    STATE_FILE.write_text(json.dumps(current_state(), ensure_ascii=False, indent=2), encoding="utf-8")


def read_log() -> str:
    if not LOG_FILE.exists():
        return ""
    return LOG_FILE.read_text(encoding="utf-8", errors="replace")


def ensure_report_pages() -> None:
    write_reports_index(REPORT_DIR)
    for profile in PROFILES:
        report_dir = profile_report_dir(profile)
        write_progress_dashboard(report_dir, profile=profile)
        write_placeholder_dashboard(report_dir, profile=profile)
        progress_file = report_dir / "latest-progress.json"
        progress_file.write_text(
            json.dumps(
                {
                    "profile": profile,
                    "target": TARGET_BASE_URL,
                    "api": API_BASE_URL,
                    "summary": {
                        "total": TEST_CASE_COUNTS.get(profile, 0),
                        "passed": 0,
                        "failed": 0,
                        "skipped": 0,
                        "error": 0,
                        "running": 0,
                        "duration": 0,
                        "finished": False,
                    },
                    "tests": [],
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )


def render_profile_card(profile: str, label: str) -> str:
    detail = PROFILE_DETAILS[profile]
    count = TEST_CASE_COUNTS.get(profile, 0)
    return f"""
        <article class="profile-card">
          <div class="profile-top">
            <div class="profile-title-row">
              <span class="profile-kicker">{escape_text(detail['tag'])}</span>
              <span class="case-count">{count} 条</span>
            </div>
            <h3>{escape_text(label)}</h3>
            <p>{escape_text(detail['what'])}</p>
          </div>
          <div class="profile-info">
            <div>
              <b>通过什么方式测</b>
              <span>{escape_text(detail['how'])}</span>
            </div>
            <div>
              <b>什么时候适合跑</b>
              <span>{escape_text(detail['when'])}</span>
            </div>
          </div>
          <div class="profile-actions">
            <button onclick="runTest('{profile}')">运行{escape_text(label)}</button>
            <div class="profile-links">
              <a href="/campus-tests/{profile}/latest-summary.html" target="_blank">AI 总结</a>
              <a href="/campus-tests/{profile}/latest-dashboard.html" target="_blank">结果看板</a>
            </div>
          </div>
        </article>"""


def render_profile_section(title: str, description: str, profiles: list[str]) -> str:
    cards = "\n".join(render_profile_card(profile, PROFILES[profile]) for profile in profiles)
    return f"""
      <section class="test-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">Test Group</span>
            <h3>{escape_text(title)}</h3>
          </div>
          <p>{escape_text(description)}</p>
        </div>
        <div class="buttons">{cards}</div>
      </section>"""


def render_page() -> str:
    sections = "\n".join(
        render_profile_section(title, description, profiles)
        for title, description, profiles in PROFILE_SECTIONS
    )
    token_box = ""
    if RUNNER_TOKEN:
        token_box = '<input id="token" type="password" placeholder="输入 WEB_RUNNER_TOKEN">'

    return f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Campus Platform 一键测试控制台</title>
  <style>
    :root {{
      --bg:#eef3fb; --panel:#fff; --soft:#f7faff; --text:#111827; --muted:#64748b;
      --line:#dbe5f0; --brand:#2563eb; --brand2:#7c3aed; --ok:#16a34a; --bad:#dc2626; --warn:#d97706;
      --shadow:0 18px 45px rgba(15,23,42,.08);
    }}
    * {{ box-sizing:border-box; }}
    body {{ margin:0; background:radial-gradient(circle at top left,#dbeafe 0,#eef3fb 34%,#f8fafc 100%); color:var(--text); font-family:Inter,"Microsoft YaHei",system-ui,sans-serif; }}
    main {{ width:min(1480px,calc(100% - 40px)); margin:0 auto; padding:28px 0 48px; }}
    .topbar {{ display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:18px; color:#334155; }}
    .brand {{ display:flex; align-items:center; gap:10px; font-weight:900; }}
    .brand-dot {{ width:12px; height:12px; border-radius:50%; background:linear-gradient(135deg,var(--brand),var(--brand2)); box-shadow:0 0 0 6px rgba(37,99,235,.12); }}
    .top-links {{ display:flex; flex-wrap:wrap; gap:10px; }}
    .top-links a {{ color:#1d4ed8; text-decoration:none; font-weight:800; font-size:13px; }}
    .hero {{ display:grid; grid-template-columns:minmax(0,1fr) 320px; gap:22px; padding:34px; background:linear-gradient(135deg,#ffffff 0%,#f8fbff 100%); border:1px solid rgba(219,229,240,.9); border-radius:32px; box-shadow:var(--shadow); }}
    h1 {{ margin:0; font-size:clamp(32px,5vw,54px); letter-spacing:-.045em; line-height:1.04; }}
    .sub {{ color:var(--muted); margin:12px 0 0; }}
    .status {{ display:grid; align-content:center; gap:10px; padding:22px; border:1px solid var(--line); border-radius:24px; background:#fff; }}
    .status strong {{ display:block; font-size:32px; color:var(--brand); line-height:1.1; }}
    .hero-actions {{ display:flex; flex-wrap:wrap; gap:12px; margin-top:20px; }}
    .hero-actions button {{ width:auto; min-width:180px; }}
    .hero-actions a {{ display:inline-flex; align-items:center; justify-content:center; border-radius:16px; padding:15px 18px; border:1px solid var(--line); background:#fff; color:var(--brand); text-decoration:none; font-weight:900; }}
    .runner-layout {{ display:grid; grid-template-columns:1fr; gap:20px; margin-top:20px; align-items:start; }}
    .panel {{ background:rgba(255,255,255,.94); border:1px solid var(--line); border-radius:26px; padding:22px; box-shadow:0 12px 32px rgba(15,23,42,.05); }}
    h2 {{ margin:0 0 8px; }}
    h3 {{ margin:0; }}
    .muted {{ color:var(--muted); }}
    .test-section {{ margin-top:18px; padding:18px; border:1px solid var(--line); border-radius:24px; background:var(--soft); }}
    .section-head {{ display:grid; grid-template-columns:220px 1fr; gap:18px; align-items:end; margin-bottom:14px; }}
    .section-head p {{ margin:0; color:var(--muted); line-height:1.6; }}
    .section-kicker {{ display:block; color:var(--brand); font-size:12px; font-weight:900; letter-spacing:.08em; text-transform:uppercase; margin-bottom:5px; }}
    .buttons {{ display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; }}
    .profile-card {{ display:flex; flex-direction:column; justify-content:space-between; gap:14px; min-height:300px; border:1px solid var(--line); border-radius:22px; padding:18px; background:#fff; transition:transform .18s ease, box-shadow .18s ease, border-color .18s ease; }}
    .profile-card:hover {{ transform:translateY(-2px); box-shadow:0 16px 36px rgba(37,99,235,.10); border-color:#bfdbfe; }}
    .profile-card p {{ margin:0; color:var(--muted); line-height:1.65; }}
    .profile-top {{ min-height:108px; }}
    .profile-title-row {{ display:flex; justify-content:space-between; align-items:center; gap:10px; }}
    .profile-info {{ display:grid; gap:10px; }}
    .profile-info div {{ padding:12px; border:1px solid var(--line); border-radius:14px; background:#fff; }}
    .profile-info b {{ display:block; margin-bottom:5px; font-size:13px; color:var(--text); }}
    .profile-info span {{ display:block; color:var(--muted); font-size:13px; line-height:1.6; }}
    .profile-kicker {{ display:inline-flex; color:var(--brand); font-size:12px; font-weight:900; letter-spacing:.06em; }}
    .case-count {{ display:inline-flex; align-items:center; border-radius:999px; padding:5px 10px; background:#eef6ff; color:#1d4ed8; font-size:12px; font-weight:900; white-space:nowrap; }}
    .case-note {{ font-size:13px; max-width:900px; }}
    .profile-actions {{ display:grid; gap:10px; }}
    .profile-links {{ display:flex; flex-wrap:wrap; gap:8px; }}
    .profile-links a {{ display:inline-flex; border-radius:999px; padding:7px 10px; background:#fff; border:1px solid var(--line); color:var(--brand); font-size:12px; font-weight:900; text-decoration:none; }}
    button {{ border:0; border-radius:16px; padding:15px 18px; background:linear-gradient(135deg,var(--brand),#4f46e5); color:#fff; font-weight:900; cursor:pointer; width:100%; box-shadow:0 10px 22px rgba(37,99,235,.18); }}
    button:hover {{ filter:brightness(1.04); }}
    button:disabled {{ cursor:not-allowed; opacity:.55; }}
    input {{ width:100%; border:1px solid var(--line); border-radius:14px; padding:13px 14px; margin-top:14px; }}
    .links a {{ display:inline-flex; margin:8px 10px 0 0; color:var(--brand); font-weight:800; text-decoration:none; }}
    .hint-list {{ margin:14px 0 0; padding:0; list-style:none; color:var(--muted); }}
    .hint-list li {{ padding:9px 0; border-bottom:1px solid var(--line); }}
    .hint-list li:last-child {{ border-bottom:0; }}
    .quick-grid {{ display:grid; grid-template-columns:repeat(3,1fr); gap:12px; margin-top:18px; }}
    .quick {{ padding:16px; border:1px solid var(--line); border-radius:18px; background:#fff; }}
    .quick b {{ display:block; font-size:24px; }}
    .quick span {{ color:var(--muted); font-size:12px; }}
    pre {{ max-height:420px; overflow:auto; background:#0f172a; color:#dbeafe; padding:18px; border-radius:18px; white-space:pre-wrap; font-size:13px; line-height:1.55; }}
    details.log-panel {{ margin-top:20px; }}
    details.log-panel summary {{ cursor:pointer; list-style:none; font-size:22px; font-weight:900; }}
    details.log-panel summary::-webkit-details-marker {{ display:none; }}
    .current-links {{ display:flex; flex-wrap:wrap; gap:8px; margin-top:10px; }}
    .current-links a {{ display:inline-flex; border-radius:999px; padding:7px 10px; background:#eef6ff; color:var(--brand); font-size:12px; font-weight:900; text-decoration:none; }}
    .footer {{ margin-top:20px; padding:18px 22px; border:1px solid var(--line); border-radius:22px; background:rgba(255,255,255,.72); color:var(--muted); text-align:center; font-weight:800; }}
    .pill {{ display:inline-flex; border-radius:99px; padding:5px 10px; font-size:12px; font-weight:900; background:#e2e8f0; color:#475569; }}
    .pill.running {{ background:#dbeafe; color:#1d4ed8; }}
    .pill.done {{ background:#dcfce7; color:#166534; }}
    .pill.fail {{ background:#fee2e2; color:#991b1b; }}
    @media (max-width:1100px) {{ .hero,.runner-layout,.section-head {{ grid-template-columns:1fr; }} }}
    @media (max-width:760px) {{ main {{ width:min(100% - 24px,1480px); }} .buttons,.quick-grid {{ grid-template-columns:1fr; }} .topbar {{ align-items:flex-start; flex-direction:column; }} }}
  </style>
</head>
<body>
<main>
  <nav class="topbar">
    <div class="brand"><span class="brand-dot"></span><span>Campus Platform 自动化测试中心</span></div>
    <div class="top-links">
      <a href="/campus-tests/index.html" target="_blank">报告总入口</a>
    </div>
  </nav>
  <section class="hero">
    <div>
      <h1>一键自动化测试控制台</h1>
      <p class="sub">前端地址：<b>{TARGET_BASE_URL}</b>；后端 API：<b>{API_BASE_URL}</b>。点击按钮即可执行对应测试套餐，跑完自动生成可视化报告。</p>
      <div class="quick-grid">
        <div class="quick"><b>12</b><span>测试套餐</span></div>
        <div class="quick"><b>{TOTAL_TEST_CASES}</b><span>全量去重用例</span></div>
        <div class="quick"><b>实时</b><span>进度与报告</span></div>
      </div>
      <p class="sub case-note">每张卡绑定独立测试文件，只展示自己的用例；也可以点击“一键全量测试”一次性跑完全部 {TOTAL_TEST_CASES} 条。</p>
      <div class="hero-actions">
        <button onclick="runTest('full')">一键全量测试</button>
        <a href="/campus-tests/full/latest-summary.html" target="_blank">查看全量 AI 总结</a>
        <a href="/campus-tests/index.html" target="_blank">进入报告总入口</a>
      </div>
    </div>
    <div class="status">
      <span id="pill" class="pill">读取中</span>
      <strong id="state">-</strong>
      <p class="sub" id="elapsed">-</p>
      <div class="current-links" id="currentLinks"></div>
    </div>
  </section>
  <section class="runner-layout">
    <article class="panel">
      <h2>测试套餐</h2>
      <p class="muted">按场景分区展示，每个套餐都有独立实时页、看板和报告目录。运行期间按钮会锁定，避免重复触发。</p>
      {token_box}
      {sections}
    </article>
  </section>
  <details class="panel log-panel">
    <summary>运行日志</summary>
    <p class="muted">重点查看上方执行状态与各测试独立报告；日志用于定位异常原因。</p>
    <pre id="log">等待运行...</pre>
  </details>
  <footer class="footer">Campus Platform 质量验收中心 · 支持一键执行、独立报告、AI 总结与问题追踪</footer>
</main>
<script>
async function refresh() {{
  try {{
    const res = await fetch('status?ts=' + Date.now());
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const data = await res.json();
    const running = data.running;
    const failed = data.exitCode !== null && data.exitCode !== 0;
    document.getElementById('pill').className = 'pill ' + (running ? 'running' : failed ? 'fail' : 'done');
    document.getElementById('pill').textContent = running ? '运行中' : failed ? '失败' : '空闲';
    document.getElementById('state').textContent = data.profileLabel || '等待测试';
    document.getElementById('elapsed').textContent = '耗时：' + formatDuration(data.elapsedSeconds || 0);
    document.getElementById('log').textContent = data.logTail || '暂无日志';
    document.getElementById('currentLinks').innerHTML = [
      data.liveUrl ? `<a href="${{data.liveUrl}}" target="_blank">实时进度</a>` : '',
      data.dashboardUrl ? `<a href="${{data.dashboardUrl}}" target="_blank">结果看板</a>` : '',
      data.profile ? `<a href="/campus-tests/${{data.profile}}/latest-summary.html" target="_blank">AI 总结</a>` : '',
      data.reportsIndexUrl ? `<a href="${{data.reportsIndexUrl}}" target="_blank">报告总入口</a>` : ''
    ].join('');
    document.querySelectorAll('button').forEach(btn => btn.disabled = running);
  }} catch (error) {{
    document.getElementById('pill').className = 'pill fail';
    document.getElementById('pill').textContent = '连接失败';
    document.getElementById('state').textContent = '控制台接口不可用';
    document.getElementById('elapsed').textContent = '请检查 Nginx /campus-test-runner/ 反代或 web_runner 服务';
    document.getElementById('log').textContent = String(error);
  }}
}}
function formatDuration(seconds) {{
  const total = Math.max(0, Math.floor(Number(seconds) || 0));
  const h = String(Math.floor(total / 3600)).padStart(2, '0');
  const m = String(Math.floor((total % 3600) / 60)).padStart(2, '0');
  const s = String(total % 60).padStart(2, '0');
  return `${{h}}:${{m}}:${{s}}`;
}}
async function runTest(profile) {{
  const body = new URLSearchParams();
  body.set('profile', profile);
  const headers = {{'Content-Type':'application/x-www-form-urlencoded'}};
  const token = document.getElementById('token');
  if (token && token.value) headers['X-Runner-Token'] = token.value;
  try {{
    const res = await fetch('run', {{method:'POST', headers, body}});
    const data = await res.json();
    alert(data.message || (data.ok ? '已开始执行' : '启动失败'));
    refresh();
  }} catch (error) {{
    alert('按钮请求失败：' + String(error) + '\\n请检查 /campus-test-runner/ 反代配置。');
  }}
}}
refresh();
setInterval(refresh, 3000);
</script>
</body>
</html>"""


def main() -> int:
    host = ENV.get("WEB_RUNNER_HOST", "127.0.0.1")
    port = int(ENV.get("WEB_RUNNER_PORT", "8099"))
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    ensure_report_pages()
    print(f"Campus test runner listening on http://{host}:{port}")
    ThreadingHTTPServer((host, port), RunnerHandler).serve_forever()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

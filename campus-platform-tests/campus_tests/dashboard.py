from __future__ import annotations

import json
from collections import Counter, defaultdict
from datetime import datetime
from html import escape
from pathlib import Path
from typing import Any

from .ai_summary import build_summary, render_summary_page


STATUS_LABELS = {
    "passed": "通过",
    "failed": "失败",
    "skipped": "跳过",
    "error": "错误",
}

PROFILE_LABELS = {
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

PROFILE_ORDER = [
    "smoke",
    "blackbox",
    "deployment",
    "frontend",
    "contract",
    "negative",
    "business",
    "api",
    "rbac",
    "security",
    "performance",
    "full",
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

PROFILE_DESCRIPTIONS = {
    "smoke": "最快确认系统是否可访问，适合作为上线验收第一步。",
    "blackbox": "从公网用户视角检查部署效果，不依赖服务器内部信息。",
    "deployment": "验证 Nginx、静态资源、CORS、API 错误页等上线配置。",
    "frontend": "巡检主要前端路由，证明页面刷新和资源加载正常。",
    "contract": "检查接口统一返回格式，说明前后端协作契约稳定。",
    "negative": "使用异常输入验证系统健壮性和错误响应是否受控。",
    "business": "覆盖公告、图书、报修、校园卡、宿舍、课程等核心业务读取。",
    "api": "集中巡检公开接口、登录态接口和业务接口健康度。",
    "rbac": "验证匿名、学生、教师、管理员之间的权限边界。",
    "security": "检查敏感路径、堆栈泄露和基础安全响应。",
    "performance": "通过重复请求和小并发观察响应耗时与稳定性。",
    "full": "汇总执行全部测试，适合作为最终验收报告展示。",
}


def write_progress_dashboard(report_dir: Path, profile: str = "full") -> Path:
    report_dir.mkdir(parents=True, exist_ok=True)
    output = report_dir / "latest-live.html"
    output.write_text(_live_html(profile), encoding="utf-8")
    return output


def write_final_dashboard(payload: dict[str, Any], report_dir: Path) -> Path:
    report_dir.mkdir(parents=True, exist_ok=True)
    html = render_dashboard(payload)
    latest = report_dir / "latest-dashboard.html"
    latest.write_text(html, encoding="utf-8")
    write_summary_dashboard(payload, report_dir)

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    archive = report_dir / f"campus-visual-report-{timestamp}.html"
    archive.write_text(html, encoding="utf-8")
    return latest


def write_summary_dashboard(payload: dict[str, Any], report_dir: Path, *, use_ai: bool = True) -> Path:
    report_dir.mkdir(parents=True, exist_ok=True)
    summary = build_summary(payload) if use_ai else build_summary({**payload, "disable_ai": True})
    html = render_summary_page(payload, summary, _css())
    latest = report_dir / "latest-summary.html"
    latest.write_text(html, encoding="utf-8")
    (report_dir / "latest-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return latest


def write_placeholder_dashboard(report_dir: Path, profile: str = "full") -> Path:
    report_dir.mkdir(parents=True, exist_ok=True)
    latest = report_dir / "latest-dashboard.html"
    if latest.exists():
        return latest
    payload = {
        "profile": profile,
        "target": "-",
        "api": "-",
        "summary": {"total": TEST_CASE_COUNTS.get(profile, 0), "passed": 0, "failed": 0, "skipped": 0, "error": 0, "duration": 0},
        "tests": [],
    }
    latest.write_text(render_dashboard(payload), encoding="utf-8")
    write_summary_dashboard(payload, report_dir, use_ai=False)
    return latest


def write_reports_index(reports_root: Path) -> Path:
    reports_root.mkdir(parents=True, exist_ok=True)
    html = _reports_index_html()
    latest = reports_root / "index.html"
    latest.write_text(html, encoding="utf-8")
    return latest


def render_dashboard(payload: dict[str, Any]) -> str:
    tests = payload.get("tests", [])
    summary = payload.get("summary", {})
    profile = str(payload.get("profile", "full") or "full")
    profile_label = PROFILE_LABELS.get(profile, profile)
    profile_description = PROFILE_DESCRIPTIONS.get(profile, "展示本轮自动化测试执行情况。")
    duration = float(summary.get("duration", 0) or 0)
    total = int(summary.get("total", len(tests)) or 0)
    passed = int(summary.get("passed", 0) or 0)
    failed = int(summary.get("failed", 0) or 0)
    skipped = int(summary.get("skipped", 0) or 0)
    errors = int(summary.get("error", 0) or 0)
    completed = passed + failed + skipped + errors
    pass_rate = round((passed / total) * 100, 1) if total else 0

    categories = _category_counts(tests)
    slow_tests = sorted(tests, key=lambda item: item.get("duration", 0), reverse=True)[:10]
    failed_tests = [item for item in tests if item.get("outcome") in {"failed", "error"}]

    return f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Campus Platform 自动化测试看板</title>
  <style>{_css()}</style>
</head>
<body>
  <main class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">Campus Platform Test Dashboard</p>
        <h1>{escape(profile_label)}可视化看板</h1>
        <p class="sub">{escape(profile_description)}</p>
        <p class="sub">目标：{escape(payload.get("target", "-"))} · API：{escape(payload.get("api", "-"))}</p>
      </div>
      <div class="score">
        <span>{pass_rate}%</span>
        <small>通过率</small>
      </div>
    </section>

    <section class="grid metrics">
      {_metric_card("总用例", total, "本次收集并执行的测试数量")}
      {_metric_card("已完成", completed, f"总耗时 {duration:.2f}s")}
      {_metric_card("通过", passed, "行为符合预期", "ok")}
      {_metric_card("失败/错误", failed + errors, "需要优先排查", "bad")}
      {_metric_card("跳过", skipped, "通常因为未配置 token", "warn")}
    </section>

    {_summary_insight(pass_rate, failed + errors, skipped, duration)}

    <section class="split">
      <article class="panel">
        <div class="panel-head">
          <h2>测试类型分布</h2>
          <p>按 pytest marker 聚合，判断覆盖是否均衡。</p>
        </div>
        <div class="bars">
          {_bars(categories, total)}
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <h2>结果状态</h2>
          <p>通过、失败、跳过的整体比例。</p>
        </div>
        <div class="status-stack">
          {_status_segment("通过", passed, total, "ok")}
          {_status_segment("失败", failed, total, "bad")}
          {_status_segment("错误", errors, total, "bad")}
          {_status_segment("跳过", skipped, total, "warn")}
        </div>
      </article>
    </section>

    {_failure_section(failed_tests)}

    {_grouped_test_sections(tests)}

    <section class="panel">
      <div class="panel-head">
        <h2>耗时 Top 10</h2>
        <p>定位慢接口、网络延迟或页面资源加载问题。</p>
      </div>
      <div class="bars slow">
        {_slow_bars(slow_tests)}
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <h2>每一条测试明细</h2>
        <p>完整展示用例、分类、状态、耗时和失败摘要。</p>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>状态</th>
              <th>测试用例</th>
              <th>分类</th>
              <th>耗时</th>
              <th>分析</th>
            </tr>
          </thead>
          <tbody>
            {_test_rows(tests)}
          </tbody>
        </table>
      </div>
    </section>
    <footer class="footer">Campus Platform 自动化测试报告 · 每类测试独立目录，支撑交付验收与质量复盘</footer>
  </main>
</body>
</html>"""


def write_progress_json(payload: dict[str, Any], report_dir: Path) -> None:
    report_dir.mkdir(parents=True, exist_ok=True)
    (report_dir / "latest-progress.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def _metric_card(title: str, value: int, note: str, tone: str = "") -> str:
    return f"""
      <article class="metric {tone}">
        <span>{escape(title)}</span>
        <strong>{value}</strong>
        <small>{escape(note)}</small>
      </article>"""


def _summary_insight(pass_rate: float, failed_count: int, skipped: int, duration: float) -> str:
    if failed_count == 0 and pass_rate >= 95:
        tone = "ok"
        title = "测试结论：整体通过"
        message = "本轮测试未发现阻塞问题，可以重点查看跳过用例是否因为未配置 token。"
    elif failed_count <= 3:
        tone = "warn"
        title = "测试结论：存在少量风险"
        message = "建议先查看失败分析区，优先处理权限、转发、接口契约或时间格式问题。"
    else:
        tone = "bad"
        title = "测试结论：需要重点排查"
        message = "失败或错误用例较多，建议先跑冒烟测试定位服务可用性，再逐类排查。"

    return f"""
    <section class="insight {tone}">
      <div>
        <h2>{escape(title)}</h2>
        <p>{escape(message)}</p>
      </div>
      <div class="insight-meta">
        <span>通过率 <b>{pass_rate}%</b></span>
        <span>失败/错误 <b>{failed_count}</b></span>
        <span>跳过 <b>{skipped}</b></span>
        <span>耗时 <b>{duration:.2f}s</b></span>
      </div>
    </section>"""


def _category_counts(tests: list[dict[str, Any]]) -> Counter[str]:
    counter: Counter[str] = Counter()
    for item in tests:
        for marker in item.get("markers", []):
            counter[marker] += 1
    return counter


def _grouped_test_sections(tests: list[dict[str, Any]]) -> str:
    if not tests:
        return ""

    groups: dict[str, list[dict[str, Any]]] = {key: [] for key in PROFILE_ORDER}
    uncategorized: list[dict[str, Any]] = []
    for item in tests:
        markers = item.get("markers", [])
        matched = False
        for marker in PROFILE_ORDER:
            if marker in markers:
                groups[marker].append(item)
                matched = True
        if not matched:
            uncategorized.append(item)

    cards = []
    for marker in PROFILE_ORDER:
        items = groups[marker]
        if not items:
            continue
        cards.append(_group_card(marker, items))
    if uncategorized:
        cards.append(_group_card("uncategorized", uncategorized))

    return f"""
    <section class="panel">
      <div class="panel-head">
        <h2>按测试类型查看用例</h2>
        <p>同一条用例可能同时属于多个类型，所以分类数量相加可能大于总用例数。</p>
      </div>
      <div class="group-grid">
        {''.join(cards)}
      </div>
    </section>"""


def _group_card(marker: str, items: list[dict[str, Any]]) -> str:
    counts = Counter(item.get("outcome", "unknown") for item in items)
    rows = "".join(
        f"<li><span class=\"pill {escape(item.get('outcome', 'unknown'))}\">"
        f"{escape(STATUS_LABELS.get(item.get('outcome', 'unknown'), item.get('outcome', 'unknown')))}</span>"
        f"<b>{escape(_short_name(item.get('nodeid', '')))}</b></li>"
        for item in items[:8]
    )
    more = f"<p class=\"muted\">还有 {len(items) - 8} 条未展开，完整内容见下方总明细。</p>" if len(items) > 8 else ""
    label = PROFILE_LABELS.get(marker, "未分类用例")
    return f"""
        <article class="group-card">
          <div class="group-head">
            <h3>{escape(label)}</h3>
            <strong>{len(items)} 条</strong>
          </div>
          <p class="group-summary">通过 {counts.get('passed', 0)}，失败 {counts.get('failed', 0)}，错误 {counts.get('error', 0)}，跳过 {counts.get('skipped', 0)}</p>
          <ul>{rows}</ul>
          {more}
        </article>"""


def _bars(counter: Counter[str], total: int) -> str:
    if not counter:
        return "<p class=\"muted\">暂无分类数据。</p>"
    rows = []
    for name, count in counter.most_common():
        width = round((count / max(total, 1)) * 100, 1)
        rows.append(
            f"""
            <div class="bar-row">
              <div class="bar-label"><span>{escape(name)}</span><b>{count}</b></div>
              <div class="bar-track"><i style="width:{width}%"></i></div>
            </div>"""
        )
    return "\n".join(rows)


def _slow_bars(tests: list[dict[str, Any]]) -> str:
    if not tests:
        return "<p class=\"muted\">暂无耗时数据。</p>"
    max_duration = max(float(item.get("duration", 0) or 0) for item in tests) or 1
    rows = []
    for item in tests:
        duration = float(item.get("duration", 0) or 0)
        width = round((duration / max_duration) * 100, 1)
        rows.append(
            f"""
            <div class="bar-row">
              <div class="bar-label"><span>{escape(_short_name(item.get("nodeid", "")))}</span><b>{duration:.2f}s</b></div>
              <div class="bar-track"><i style="width:{width}%"></i></div>
            </div>"""
        )
    return "\n".join(rows)


def _status_segment(label: str, value: int, total: int, tone: str) -> str:
    width = round((value / max(total, 1)) * 100, 1)
    return f"""
      <div class="status-line">
        <div class="status-meta"><span>{escape(label)}</span><b>{value}</b></div>
        <div class="status-track"><i class="{tone}" style="width:{width}%"></i></div>
      </div>"""


def _failure_section(failed_tests: list[dict[str, Any]]) -> str:
    if not failed_tests:
        return """
    <section class="panel success-panel">
      <div class="panel-head">
        <h2>失败分析</h2>
        <p>本次没有失败或错误用例。</p>
      </div>
    </section>"""

    grouped: defaultdict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in failed_tests:
        grouped[_failure_reason(item)].append(item)

    cards = []
    for reason, items in grouped.items():
        cards.append(
            f"""
            <article class="failure-card">
              <h3>{escape(reason)}</h3>
              <p>{len(items)} 条用例命中该问题。</p>
              <p class="suggestion">{escape(_failure_suggestion(reason))}</p>
              <ul>{''.join(f'<li>{escape(_short_name(test.get("nodeid", "")))}</li>' for test in items[:5])}</ul>
            </article>"""
        )

    return f"""
    <section class="panel">
      <div class="panel-head">
        <h2>失败分析</h2>
        <p>按错误特征聚合，优先排查命中数量最多的问题。</p>
      </div>
      <div class="failure-grid">
        {''.join(cards)}
      </div>
    </section>"""


def _failure_reason(item: dict[str, Any]) -> str:
    text = (item.get("longrepr") or item.get("message") or "").lower()
    if "connection" in text or "timeout" in text:
        return "网络连接或接口超时"
    if "404" in text:
        return "路由或 Nginx 转发 404"
    if "401" in text or "403" in text:
        return "登录态或权限配置异常"
    if "500" in text or "server error" in text:
        return "后端服务内部错误"
    if "response contains unformatted iso datetime" in text:
        return "时间格式仍包含 T"
    if "api must return json" in text or "missing code" in text:
        return "API 返回格式不符合契约"
    return "断言不通过，需要查看明细"


def _failure_suggestion(reason: str) -> str:
    suggestions = {
        "网络连接或接口超时": "先确认目标服务器、Nginx、后端进程和网络连通性，再看性能测试耗时。",
        "路由或 Nginx 转发 404": "检查前端 SPA fallback 和 /api 反代规则，尤其是刷新子路由时的配置。",
        "登录态或权限配置异常": "检查 token 是否过期、角色权限是否正确，以及 Sa-Token 拦截配置。",
        "后端服务内部错误": "查看后端日志、数据库和 Redis 连接，优先复现对应接口。",
        "时间格式仍包含 T": "检查后端序列化或前端响应归一化是否漏掉该字段。",
        "API 返回格式不符合契约": "确认接口是否统一返回 {code,msg,data}，以及 Nginx 是否把 HTML 错误页转给 API。",
    }
    return suggestions.get(reason, "打开明细表查看断言信息，再按测试文件名定位对应测试场景。")


def _test_rows(tests: list[dict[str, Any]]) -> str:
    rows = []
    for item in tests:
        outcome = item.get("outcome", "unknown")
        markers = ", ".join(item.get("markers", []))
        duration = float(item.get("duration", 0) or 0)
        message = _first_line(item.get("longrepr") or item.get("message") or "正常")
        rows.append(
            f"""
            <tr>
              <td><span class="pill {escape(outcome)}">{escape(STATUS_LABELS.get(outcome, outcome))}</span></td>
              <td class="name">{escape(item.get("nodeid", ""))}</td>
              <td>{escape(markers)}</td>
              <td>{duration:.2f}s</td>
              <td>{escape(message)}</td>
            </tr>"""
        )
    return "\n".join(rows)


def _short_name(nodeid: str) -> str:
    return nodeid.split("::")[-1] if nodeid else "-"


def _first_line(text: str) -> str:
    line = str(text).strip().splitlines()[0] if str(text).strip() else "正常"
    return line[:180]


def _reports_index_html() -> str:
    cards = []
    for profile in PROFILE_ORDER:
        label = PROFILE_LABELS.get(profile, profile)
        count = TEST_CASE_COUNTS.get(profile, 0)
        description = PROFILE_DESCRIPTIONS.get(profile, "查看该测试套餐的实时进度和最终报告。")
        cards.append(
            f"""
            <article class="group-card">
              <div class="group-head"><h3>{escape(label)}</h3><strong>{count} 条</strong></div>
              <p class="group-summary">{escape(description)}</p>
              <p class="group-summary">独立目录：<code>{escape(profile)}/</code></p>
              <div class="links">
                <a href="/campus-tests/{escape(profile)}/latest-live.html">实时进度</a>
                <a href="/campus-tests/{escape(profile)}/latest-dashboard.html">最终看板</a>
                <a href="/campus-tests/{escape(profile)}/latest-summary.html">AI 总结</a>
              </div>
            </article>"""
        )
    return f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Campus Platform 测试报告总入口</title>
  <style>{_css()}</style>
</head>
<body>
  <main class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">Campus Platform Test Reports</p>
        <h1>测试报告总入口</h1>
        <p class="sub">面向交付验收的测试报告入口：每个测试套餐都有独立目录、实时进度页、最终看板和历史报告。可按“基础可用、接口业务、安全稳定、最终验收”的顺序审阅。</p>
      </div>
      <div class="score"><span>{TEST_CASE_COUNTS["full"]}</span><small>全量用例</small></div>
    </section>
    <section class="panel">
      <div class="panel-head">
        <h2>独立测试页面</h2>
        <p>选择对应测试目录查看该类测试自己的结果。</p>
      </div>
      <div class="group-grid">{''.join(cards)}</div>
    </section>
    <footer class="footer">建议验收顺序：报告总入口 → 冒烟测试 → 接口测试 → 权限/安全测试 → 全量验收看板</footer>
  </main>
</body>
</html>"""


def _live_html(profile: str = "full") -> str:
    profile_label = PROFILE_LABELS.get(profile, profile)
    profile_description = PROFILE_DESCRIPTIONS.get(profile, "实时展示当前测试执行进度。")
    return f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Campus Platform 实时测试进度</title>
  <style>{_css()}</style>
</head>
<body>
  <main class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">Live Test Progress</p>
        <h1>{escape(profile_label)}实时进度</h1>
        <p class="sub">{escape(profile_description)}</p>
        <p class="sub" id="target">等待测试启动...</p>
      </div>
      <div class="score"><span id="rate">0%</span><small>完成率</small></div>
    </section>
    <section class="grid metrics" id="metrics"></section>
    <section class="panel live-progress">
      <div class="panel-head">
        <h2>执行进度</h2>
        <p id="progressText">等待测试开始。</p>
      </div>
      <div class="progress-large"><i id="progressBar" style="width:0%"></i></div>
      <div class="live-summary" id="liveSummary"></div>
    </section>
    <section class="panel">
      <div class="panel-head">
        <h2>正在执行与结果明细</h2>
        <p>页面每 3 秒自动读取 latest-progress.json。</p>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>状态</th><th>测试用例</th><th>分类</th><th>耗时</th></tr></thead>
          <tbody id="rows"></tbody>
        </table>
      </div>
    </section>
    <footer class="footer">该页面专注展示当前测试套餐的实时进度，最终结果请查看同目录下的最终看板。</footer>
  </main>
  <script>
    async function refresh() {{
      try {{
        const response = await fetch('latest-progress.json?ts=' + Date.now());
        const data = await response.json();
        const summary = data.summary || {{}};
        const total = summary.total || 0;
        const done = (summary.passed || 0) + (summary.failed || 0) + (summary.skipped || 0) + (summary.error || 0);
        const rate = total ? Math.round(done / total * 100) : 0;
        document.getElementById('target').textContent = '目标：' + (data.target || '-') + ' · API：' + (data.api || '-');
        document.getElementById('rate').textContent = rate + '%';
        document.getElementById('progressText').textContent = done + ' / ' + total + ' 条用例已完成，页面会自动刷新。';
        document.getElementById('progressBar').style.width = rate + '%';
        document.getElementById('liveSummary').innerHTML = [
          chip('通过', summary.passed || 0, 'passed'),
          chip('失败', summary.failed || 0, 'failed'),
          chip('错误', summary.error || 0, 'error'),
          chip('跳过', summary.skipped || 0, 'skipped'),
          chip('进行中', summary.running || 0, 'running')
        ].join('');
        document.getElementById('metrics').innerHTML = [
          card('总用例', total, '已收集测试数量'),
          card('已完成', done, '当前执行进度'),
          card('通过', summary.passed || 0, '符合预期', 'ok'),
          card('失败/错误', (summary.failed || 0) + (summary.error || 0), '需要排查', 'bad'),
          card('跳过', summary.skipped || 0, '未配置或条件不满足', 'warn')
        ].join('');
        document.getElementById('rows').innerHTML = (data.tests || []).map(row).join('');
      }} catch (error) {{
        document.getElementById('target').textContent = '等待 latest-progress.json 生成...';
        document.getElementById('progressText').textContent = '还没有读取到测试进度，可能尚未开始运行。';
      }}
    }}
    function card(title, value, note, tone) {{
      return `<article class="metric ${{tone || ''}}"><span>${{title}}</span><strong>${{value}}</strong><small>${{note}}</small></article>`;
    }}
    function row(item) {{
      const outcome = item.outcome || 'running';
      const label = {{passed:'通过', failed:'失败', skipped:'跳过', error:'错误', running:'进行中'}}[outcome] || outcome;
      return `<tr><td><span class="pill ${{outcome}}">${{label}}</span></td><td class="name">${{item.nodeid || '-'}}</td><td>${{(item.markers || []).join(', ')}}</td><td>${{Number(item.duration || 0).toFixed(2)}}s</td></tr>`;
    }}
    function chip(label, value, cls) {{
      return `<span class="summary-chip ${{cls}}"><b>${{value}}</b>${{label}}</span>`;
    }}
    refresh();
    setInterval(refresh, 3000);
  </script>
</body>
</html>"""


def _css() -> str:
    return """
:root {
  color-scheme: light;
  --bg: #eef3fb;
  --panel: #ffffff;
  --soft: #f8fbff;
  --text: #121826;
  --muted: #64748b;
  --line: #dbe5f0;
  --brand: #2563eb;
  --brand2: #7c3aed;
  --ok: #16a34a;
  --bad: #dc2626;
  --warn: #d97706;
  --shadow: 0 18px 45px rgba(15, 23, 42, .08);
}
* { box-sizing: border-box; }
body {
  margin: 0;
  background: radial-gradient(circle at top left, #dbeafe 0, var(--bg) 34%, #f8fafc 100%);
  color: var(--text);
  font-family: Inter, "Microsoft YaHei", "PingFang SC", system-ui, sans-serif;
}
.page { width: min(1480px, calc(100% - 40px)); margin: 0 auto; padding: 30px 0 52px; }
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 210px;
  gap: 24px;
  align-items: center;
  padding: 34px;
  border: 1px solid var(--line);
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  border-radius: 32px;
  box-shadow: var(--shadow);
}
.eyebrow { margin: 0 0 8px; color: var(--brand); font-weight: 900; letter-spacing: .08em; text-transform: uppercase; }
h1 { margin: 0; font-size: clamp(32px, 5vw, 54px); line-height: 1.04; letter-spacing: -0.045em; }
.sub { margin: 14px 0 0; color: var(--muted); }
.score {
  min-height: 160px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 26px;
  background: #fff;
}
.score span { font-size: 48px; font-weight: 950; color: var(--brand); }
.score small { color: var(--muted); }
.grid { display: grid; gap: 16px; }
.metrics { grid-template-columns: repeat(5, minmax(0, 1fr)); margin: 20px 0; }
.metric {
  padding: 20px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 22px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, .05);
}
.metric span, .metric small { color: var(--muted); display: block; }
.metric strong { display: block; margin: 8px 0; font-size: 32px; }
.metric.ok strong { color: var(--ok); }
.metric.bad strong { color: var(--bad); }
.metric.warn strong { color: var(--warn); }
.insight {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 18px;
  align-items: center;
  margin: 18px 0;
  padding: 22px 24px;
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--panel);
  box-shadow: 0 12px 32px rgba(15, 23, 42, .05);
}
.insight h2 { margin: 0 0 8px; }
.insight p { margin: 0; color: var(--muted); }
.insight.ok h2 { color: var(--ok); }
.insight.warn h2 { color: var(--warn); }
.insight.bad h2 { color: var(--bad); }
.insight-meta { display: grid; grid-template-columns: repeat(2, minmax(120px, 1fr)); gap: 10px; }
.insight-meta span {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 14px;
  color: var(--muted);
  background: #f8fafc;
}
.insight-meta b { display: block; color: var(--text); font-size: 18px; margin-top: 3px; }
.split { display: grid; grid-template-columns: 1.2fr .8fr; gap: 18px; margin: 18px 0; }
.panel {
  margin: 18px 0;
  padding: 24px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 26px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, .05);
}
.panel-head { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 18px; align-items: end; margin-bottom: 18px; }
h2 { margin: 0; font-size: 22px; }
.panel-head p { margin: 0; color: var(--muted); }
.bar-row { margin: 14px 0; }
.bar-label, .status-meta { display: flex; justify-content: space-between; gap: 12px; color: var(--muted); margin-bottom: 8px; }
.bar-label span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-track, .status-track { height: 10px; background: #eef2f7; border-radius: 99px; overflow: hidden; }
.bar-track i, .status-track i { display: block; height: 100%; background: linear-gradient(90deg, var(--brand), var(--brand2)); border-radius: inherit; }
.status-track i.ok { background: var(--ok); }
.status-track i.bad { background: var(--bad); }
.status-track i.warn { background: var(--warn); }
.live-progress { margin-top: 18px; }
.progress-large {
  height: 18px;
  background: #eef2f7;
  border-radius: 99px;
  overflow: hidden;
}
.progress-large i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--brand), var(--brand2));
  border-radius: inherit;
  transition: width .25s ease;
}
.live-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
.summary-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  border-radius: 99px;
  background: #f1f5f9;
  color: #475569;
  font-weight: 800;
}
.summary-chip b { color: var(--text); }
.summary-chip.passed { background: #dcfce7; color: #166534; }
.summary-chip.failed, .summary-chip.error { background: #fee2e2; color: #991b1b; }
.summary-chip.skipped { background: #fef3c7; color: #92400e; }
.summary-chip.running { background: #dbeafe; color: #1d4ed8; }
.failure-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.failure-card {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fff7f7;
}
.failure-card h3 { margin: 0 0 8px; color: var(--bad); }
.failure-card p { margin: 0 0 10px; color: var(--muted); }
.failure-card .suggestion {
  padding: 10px 12px;
  border-radius: 12px;
  background: #ffffff;
  color: #475569;
  border: 1px solid #fecaca;
}
.failure-card ul { margin: 0; padding-left: 18px; color: var(--text); }
.success-panel { background: #f6fff8; }
.group-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.group-card {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease;
}
.group-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 36px rgba(37, 99, 235, .10);
  border-color: #bfdbfe;
}
.group-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.group-head h3 { margin: 0; }
.group-head strong {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef6ff;
  color: #1d4ed8;
  white-space: nowrap;
}
.group-summary { margin: 10px 0 12px; color: var(--muted); }
.group-card ul { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
.group-card li { display: grid; grid-template-columns: auto 1fr; gap: 10px; align-items: start; }
.group-card li b { font-size: 13px; line-height: 1.5; color: var(--text); word-break: break-word; }
.summary-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.summary-list li {
  padding: 13px 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fbfdff;
  color: #334155;
  line-height: 1.7;
}
.links { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; }
.links a {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 8px 12px;
  background: #eef6ff;
  color: #1d4ed8;
  font-weight: 900;
  text-decoration: none;
  font-size: 13px;
}
code {
  padding: 3px 7px;
  border-radius: 8px;
  background: #eef2ff;
  color: #3730a3;
  font-weight: 800;
}
.table-wrap { overflow-x: auto; border: 1px solid var(--line); border-radius: 18px; background: #fff; }
table { width: 100%; border-collapse: collapse; min-width: 980px; }
th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--line); vertical-align: top; }
th { color: var(--muted); font-size: 13px; background: #f8fafc; }
td { color: #334155; font-size: 14px; }
tr:last-child td { border-bottom: 0; }
.name { color: var(--text); font-weight: 650; }
.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  padding: 4px 9px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 800;
  background: #e2e8f0;
  color: #475569;
}
.pill.passed { background: #dcfce7; color: #166534; }
.pill.failed, .pill.error { background: #fee2e2; color: #991b1b; }
.pill.skipped { background: #fef3c7; color: #92400e; }
.pill.running { background: #dbeafe; color: #1d4ed8; }
.muted { color: var(--muted); }
.footer {
  margin: 20px 0 0;
  padding: 18px 22px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: rgba(255, 255, 255, .72);
  color: var(--muted);
  text-align: center;
  font-weight: 700;
}
@media (max-width: 980px) {
  .page { width: min(100% - 24px, 1440px); padding-top: 18px; }
  .hero, .panel-head { grid-template-columns: 1fr; align-items: stretch; }
  .score { min-height: 120px; }
  .metrics, .split, .failure-grid, .group-grid, .insight { grid-template-columns: 1fr; }
  .insight-meta { grid-template-columns: 1fr; }
}
"""

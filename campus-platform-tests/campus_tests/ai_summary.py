from __future__ import annotations

import json
import os
from collections import Counter
from html import escape
from pathlib import Path
from typing import Any
from urllib.parse import urljoin

import requests
import urllib3
from dotenv import load_dotenv

ROOT = Path(__file__).resolve().parents[1]
load_dotenv(ROOT / ".env")


def build_summary(payload: dict[str, Any]) -> dict[str, Any]:
    local = _local_summary(payload)
    if payload.get("disable_ai"):
        local["source"] = "本地规则总结（占位页）"
        return local
    if not _env_bool("AI_SUMMARY_ENABLED", True):
        local["source"] = "本地规则总结"
        return local

    api_key = os.getenv("MIMO_API_KEY", "").strip()
    if not api_key:
        local["source"] = "本地规则总结（未配置小米 AI 密钥）"
        return local

    try:
        return _mimo_summary(payload, api_key)
    except Exception as exc:  # noqa: BLE001 - summary generation must not break test reports.
        local["source"] = f"本地规则总结（小米 AI 调用失败：{type(exc).__name__}）"
        return local


def render_summary_page(payload: dict[str, Any], summary: dict[str, Any], css: str) -> str:
    profile = str(payload.get("profile", "full") or "full")
    raw_summary = payload.get("summary", {})
    total = int(raw_summary.get("total", 0) or 0)
    passed = int(raw_summary.get("passed", 0) or 0)
    failed = int(raw_summary.get("failed", 0) or 0)
    errors = int(raw_summary.get("error", 0) or 0)
    skipped = int(raw_summary.get("skipped", 0) or 0)
    pass_rate = round((passed / total) * 100, 1) if total else 0

    return f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Campus Platform AI 测试总结</title>
  <style>{css}</style>
</head>
<body>
  <main class="page">
    <section class="hero">
      <div>
        <p class="eyebrow">AI Test Summary</p>
        <h1>{escape(str(summary.get("title", "AI 测试总结")))}</h1>
        <p class="sub">{escape(str(summary.get("verdict", "测试总结已生成。")))}</p>
        <p class="sub">目标：{escape(str(payload.get("target", "-")))} · API：{escape(str(payload.get("api", "-")))}</p>
      </div>
      <div class="score"><span>{pass_rate}%</span><small>通过率</small></div>
    </section>

    <section class="grid metrics">
      {_metric("总用例", total, "本轮测试覆盖数量")}
      {_metric("通过", passed, "符合预期")}
      {_metric("失败/错误", failed + errors, "需要解释或修复")}
      {_metric("跳过", skipped, "环境或严格模式原因")}
      {_metric("总结来源", summary.get("source", "-"), "AI 或本地规则")}
    </section>

    {_list_panel("分析依据", summary.get("data_basis", []))}

    <section class="split">
      {_list_panel("核心结论", summary.get("highlights", []))}
      {_list_panel("主要风险", summary.get("risks", []))}
    </section>

    <section class="split">
      {_list_panel("修复建议", summary.get("next_steps", []))}
      {_list_panel("验收说明建议", summary.get("speaker_notes", []))}
    </section>

    <section class="panel">
      <div class="panel-head">
        <h2>页面入口</h2>
        <p>可从 AI 总结快速跳转到实时页和最终看板，查看完整证据链。</p>
      </div>
      <div class="links">
        <a href="/campus-tests/{escape(profile)}/latest-live.html">实时进度</a>
        <a href="/campus-tests/{escape(profile)}/latest-dashboard.html">最终看板</a>
        <a href="/campus-tests/index.html">报告总入口</a>
      </div>
    </section>

    <footer class="footer">Campus Platform 自动化测试 AI 总结 · 面向交付验收与质量复盘</footer>
  </main>
</body>
</html>"""


def _mimo_summary(payload: dict[str, Any], api_key: str) -> dict[str, Any]:
    base_url = os.getenv("MIMO_BASE_URL", "https://token-plan-cn.xiaomimimo.com/v1").strip()
    model = os.getenv("MIMO_MODEL", "mimo-v2.5-pro").strip()
    timeout = int(os.getenv("AI_SUMMARY_TIMEOUT", "90") or "90")
    max_tokens = int(os.getenv("MIMO_MAX_TOKENS", "4000") or "4000")
    verify_ssl = _env_bool("MIMO_VERIFY_SSL", False)
    if not verify_ssl:
        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    url = urljoin(base_url.rstrip("/") + "/", "chat/completions")
    prompt = _prompt_payload(payload)
    request_body = {
        "model": model,
        "messages": [
            {
                "role": "system",
                "content": (
                    "你是校园平台自动化测试报告分析助手。"
                    "只输出一个 JSON 对象，不要输出 Markdown，不要解释推理过程。"
                ),
            },
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.2,
        "max_tokens": max_tokens,
    }
    session = requests.Session()
    session.trust_env = False
    response = None
    last_error: Exception | None = None
    for _ in range(2):
        try:
            response = session.post(
                url,
                headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
                json=request_body,
                timeout=timeout,
                verify=verify_ssl,
            )
            break
        except requests.RequestException as exc:
            last_error = exc
    if response is None:
        raise last_error or RuntimeError("MiMo AI request failed")
    response.raise_for_status()
    raw = response.json()
    content = raw["choices"][0]["message"]["content"]
    parsed = json.loads(_extract_json_object(_strip_code_fence(content)))
    parsed["source"] = f"小米 AI：{model}"
    return _normalize_summary(parsed)


def _local_summary(payload: dict[str, Any]) -> dict[str, Any]:
    summary = payload.get("summary", {})
    tests = payload.get("tests", [])
    total = int(summary.get("total", 0) or 0)
    passed = int(summary.get("passed", 0) or 0)
    failed = int(summary.get("failed", 0) or 0)
    errors = int(summary.get("error", 0) or 0)
    skipped = int(summary.get("skipped", 0) or 0)
    pass_rate = round((passed / total) * 100, 1) if total else 0
    failed_tests = [item for item in tests if item.get("outcome") in {"failed", "error"}]
    marker_counts: Counter[str] = Counter(
        marker for item in tests for marker in item.get("markers", [])
    )
    top_failed = [_short_name(item.get("nodeid", "")) for item in failed_tests[:6]]
    duration = float(summary.get("duration", 0) or 0)
    return _normalize_summary(
        {
            "title": f"{payload.get('profile', 'full')} 测试总结",
            "verdict": f"本轮共 {total} 条用例，通过率 {pass_rate}%，失败/错误 {failed + errors} 条，跳过 {skipped} 条。",
            "highlights": [
                f"已完成 {passed + failed + errors + skipped}/{total} 条用例。",
                f"覆盖类型：{', '.join(name for name, _ in marker_counts.most_common(6)) or '暂无'}。",
                "测试端已按独立目录生成实时页、最终看板和总结页。",
            ],
            "data_basis": [
                f"总用例 {total} 条，通过 {passed} 条，失败/错误 {failed + errors} 条，跳过 {skipped} 条。",
                f"通过率 {pass_rate}%，总耗时 {duration:.2f} 秒。",
                f"失败样例：{', '.join(top_failed) if top_failed else '无'}。",
            ],
            "risks": top_failed or ["当前没有失败用例，重点确认跳过项是否符合预期。"],
            "next_steps": [
                "优先处理失败/错误用例，再重新运行对应单项测试。",
                "生产验收可保持 STRICT_MODE=true，日常巡检可使用默认模式降低公网波动噪声。",
                "全量测试通过后，可使用本页作为质量验收总结。",
            ],
            "speaker_notes": [
                "先说明测试覆盖了前端、接口、权限、安全、性能等 12 类场景。",
                "再展示通过率和失败分析，说明自动化测试能定位部署和接口问题。",
                "最后打开独立测试目录，证明每类测试都有单独报告和可追溯明细。",
            ],
            "source": "本地规则总结",
        }
    )


def _prompt_payload(payload: dict[str, Any]) -> str:
    summary = payload.get("summary", {})
    tests = payload.get("tests", [])
    compact_tests = [
        {
            "name": _short_name(item.get("nodeid", "")),
            "outcome": item.get("outcome"),
            "markers": item.get("markers", []),
            "message": str(item.get("message") or item.get("longrepr") or "")[:220],
        }
        for item in tests
        if item.get("outcome") in {"failed", "error", "skipped"}
    ][:40]
    return json.dumps(
        {
            "task": "根据测试结果生成面向客户验收的中文质量总结。只输出紧凑 JSON。每个数组最多 3 条，每条不超过 28 个汉字。",
            "required_json_schema": {
                "title": "短标题",
                "verdict": "一段总体结论",
                "data_basis": ["必须引用输入数据中的总数、通过率、失败项或跳过项"],
                "highlights": ["3-5条亮点"],
                "risks": ["主要风险"],
                "next_steps": ["修复建议"],
                "speaker_notes": ["验收说明建议"],
            },
            "profile": payload.get("profile"),
            "target": payload.get("target"),
            "api": payload.get("api"),
            "summary": summary,
            "status_by_marker": _status_by_marker(tests),
            "failed_or_skipped_tests": compact_tests,
        },
        ensure_ascii=False,
    )


def _normalize_summary(value: dict[str, Any]) -> dict[str, Any]:
    return {
        "title": str(value.get("title") or "AI 测试总结"),
        "verdict": str(value.get("verdict") or "测试总结已生成。"),
        "data_basis": _list(value.get("data_basis")),
        "highlights": _list(value.get("highlights")),
        "risks": _list(value.get("risks")),
        "next_steps": _list(value.get("next_steps")),
        "speaker_notes": _list(value.get("speaker_notes")),
        "source": str(value.get("source") or "未知"),
    }


def _list(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(item) for item in value if str(item).strip()]
    if value:
        return [str(value)]
    return ["暂无"]


def _metric(title: str, value: object, note: str) -> str:
    return f"""
      <article class="metric">
        <span>{escape(title)}</span>
        <strong>{escape(str(value))}</strong>
        <small>{escape(note)}</small>
      </article>"""


def _list_panel(title: str, items: Any) -> str:
    rows = "".join(f"<li>{escape(item)}</li>" for item in _list(items))
    return f"""
      <article class="panel">
        <div class="panel-head"><h2>{escape(title)}</h2></div>
        <ul class="summary-list">{rows}</ul>
      </article>"""


def _status_by_marker(tests: list[dict[str, Any]]) -> dict[str, dict[str, int]]:
    result: dict[str, Counter[str]] = {}
    for item in tests:
        outcome = str(item.get("outcome") or "unknown")
        for marker in item.get("markers", []):
            result.setdefault(str(marker), Counter())[outcome] += 1
    return {marker: dict(counter) for marker, counter in result.items()}


def _short_name(nodeid: str) -> str:
    return nodeid.split("::")[-1] if nodeid else "-"


def _strip_code_fence(text: str) -> str:
    value = text.strip()
    if value.startswith("```"):
        value = value.split("\n", 1)[1] if "\n" in value else value
        value = value.rsplit("```", 1)[0]
    return value.strip()


def _extract_json_object(text: str) -> str:
    value = text.strip()
    if value.startswith("{") and value.endswith("}"):
        return value
    start = value.find("{")
    end = value.rfind("}")
    if start >= 0 and end > start:
        return value[start : end + 1]
    return value


def _env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "y", "on"}

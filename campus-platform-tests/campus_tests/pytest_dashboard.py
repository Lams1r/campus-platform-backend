from __future__ import annotations

import time
from pathlib import Path
from typing import Any

from .config import CONFIG
from .dashboard import write_final_dashboard, write_progress_dashboard, write_progress_json, write_reports_index


class DashboardPlugin:
    def __init__(self, report_dir: Path) -> None:
        self.report_dir = report_dir
        self.started_at = time.time()
        self.tests: dict[str, dict[str, Any]] = {}
        self.total = 0

    def pytest_collection_modifyitems(self, session, config, items) -> None:  # type: ignore[no-untyped-def]
        self.total = len(items)
        for item in items:
            self.tests[item.nodeid] = {
                "nodeid": item.nodeid,
                "markers": sorted(marker.name for marker in item.iter_markers()),
                "outcome": "running",
                "duration": 0,
                "message": "",
                "longrepr": "",
            }
        self._write_progress()

    def pytest_runtest_logreport(self, report) -> None:  # type: ignore[no-untyped-def]
        if report.when not in {"setup", "call", "teardown"}:
            return

        test = self.tests.setdefault(
            report.nodeid,
            {
                "nodeid": report.nodeid,
                "markers": [],
                "outcome": "running",
                "duration": 0,
                "message": "",
                "longrepr": "",
            },
        )

        test["duration"] = round(float(test.get("duration", 0) or 0) + float(report.duration or 0), 4)

        if report.when == "call" or report.failed or report.skipped:
            if report.failed and report.when != "call":
                test["outcome"] = "error"
            else:
                test["outcome"] = report.outcome
            if report.longrepr:
                test["longrepr"] = str(report.longrepr)
                test["message"] = str(report.longrepr).strip().splitlines()[0]

        self._write_progress()

    def pytest_sessionfinish(self, session, exitstatus) -> None:  # type: ignore[no-untyped-def]
        payload = self._payload(exitstatus=exitstatus, finished=True)
        write_progress_json(payload, self.report_dir)
        write_final_dashboard(payload, self.report_dir)

    def _write_progress(self) -> None:
        write_progress_json(self._payload(finished=False), self.report_dir)

    def _payload(self, *, exitstatus: int | None = None, finished: bool = False) -> dict[str, Any]:
        tests = list(self.tests.values())
        summary = {
            "total": self.total or len(tests),
            "passed": sum(1 for item in tests if item.get("outcome") == "passed"),
            "failed": sum(1 for item in tests if item.get("outcome") == "failed"),
            "skipped": sum(1 for item in tests if item.get("outcome") == "skipped"),
            "error": sum(1 for item in tests if item.get("outcome") == "error"),
            "running": sum(1 for item in tests if item.get("outcome") == "running"),
            "duration": round(time.time() - self.started_at, 3),
            "exitstatus": exitstatus,
            "finished": finished,
        }
        return {
            "profile": str(self.report_dir.name),
            "target": CONFIG.target_base_url,
            "api": CONFIG.api_base_url,
            "summary": summary,
            "tests": tests,
        }


def pytest_configure(config) -> None:  # type: ignore[no-untyped-def]
    reports_root = Path(str(config.rootpath)) / "reports"
    report_dir = Path(str(config.option.htmlpath)).parent if getattr(config.option, "htmlpath", None) else reports_root / "full"
    write_reports_index(reports_root)
    write_progress_dashboard(report_dir, profile=report_dir.name)
    plugin = DashboardPlugin(report_dir)
    config.pluginmanager.register(plugin, "campus-dashboard")

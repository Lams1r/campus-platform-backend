from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urljoin

from dotenv import load_dotenv


ROOT = Path(__file__).resolve().parents[1]
load_dotenv(ROOT / ".env")


def _clean_url(value: str) -> str:
    return value.strip().rstrip("/")


def _bool_env(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


def _int_env(name: str, default: int) -> int:
    raw = os.getenv(name)
    if not raw:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


@dataclass(frozen=True)
class TestConfig:
    target_base_url: str
    api_base_url: str
    request_timeout: int
    strict_mode: bool
    perf_rounds: int
    latency_avg_ms: int
    latency_max_ms: int
    auth_header_name: str
    admin_token: str
    teacher_token: str
    student_token: str

    @classmethod
    def from_env(cls) -> "TestConfig":
        target = _clean_url(os.getenv("TARGET_BASE_URL", "http://127.0.0.1:5173"))
        api = os.getenv("API_BASE_URL", "").strip()
        if api:
            api_base = _clean_url(api)
        else:
            api_base = _clean_url(urljoin(target + "/", "api"))

        return cls(
            target_base_url=target,
            api_base_url=api_base,
            request_timeout=_int_env("REQUEST_TIMEOUT", 15),
            strict_mode=_bool_env("STRICT_MODE", False),
            perf_rounds=max(1, _int_env("PERF_ROUNDS", 5)),
            latency_avg_ms=_int_env("LATENCY_AVG_MS", 1200),
            latency_max_ms=_int_env("LATENCY_MAX_MS", 2500),
            auth_header_name=os.getenv("AUTH_HEADER_NAME", "Authorization"),
            admin_token=os.getenv("ADMIN_TOKEN", "").strip(),
            teacher_token=os.getenv("TEACHER_TOKEN", "").strip(),
            student_token=os.getenv("STUDENT_TOKEN", "").strip(),
        )

    def token_for(self, role: str) -> str:
        return {
            "admin": self.admin_token,
            "teacher": self.teacher_token,
            "student": self.student_token,
        }.get(role, "")


CONFIG = TestConfig.from_env()

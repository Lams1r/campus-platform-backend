from __future__ import annotations

import statistics
import time

import pytest

from campus_tests.assertions import assert_api_contract, assert_auth_required, assert_http_ok
from campus_tests.client import CampusClient
from campus_tests.config import CONFIG


def _measure(callable_request):
    durations = []
    for _ in range(CONFIG.perf_rounds):
        start = time.perf_counter()
        result = callable_request()
        durations.append((time.perf_counter() - start) * 1000)
        yield result, durations[-1]
    return durations


def _assert_latency_budget(durations: list[float]) -> None:
    if not CONFIG.strict_mode:
        return
    assert statistics.mean(durations) <= CONFIG.latency_avg_ms
    assert max(durations) <= CONFIG.latency_max_ms


@pytest.mark.performance
def test_frontend_home_latency_baseline() -> None:
    durations = []
    client = CampusClient()
    for response, duration_ms in _measure(lambda: client.request_frontend("/")):
        assert_http_ok(response, label="frontend home")
        durations.append(duration_ms)

    _assert_latency_budget(durations)


@pytest.mark.performance
@pytest.mark.api
def test_captcha_api_latency_baseline() -> None:
    durations = []
    client = CampusClient()
    for result, duration_ms in _measure(lambda: client.get("/auth/captcha")):
        assert_api_contract(result)
        durations.append(duration_ms)

    _assert_latency_budget(durations)


@pytest.mark.performance
@pytest.mark.api
def test_notice_page_latency_baseline() -> None:
    durations = []
    client = CampusClient()
    for result, duration_ms in _measure(lambda: client.get("/svc/notice/page?pageNum=1&pageSize=5")):
        if result.code in {401, 403} or result.status_code in {401, 403}:
            assert_auth_required(result, label="notice page")
            durations.append(duration_ms)
            continue
        assert_api_contract(result)
        durations.append(duration_ms)

    _assert_latency_budget(durations)

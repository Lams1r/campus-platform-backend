from concurrent.futures import ThreadPoolExecutor, as_completed

import pytest

from campus_tests.assertions import assert_api_contract, assert_http_ok
from campus_tests.client import CampusClient


@pytest.mark.blackbox
@pytest.mark.performance
@pytest.mark.api
def test_public_api_small_concurrency_baseline() -> None:
    paths = [
        "/auth/captcha",
        "/svc/notice/page?pageNum=1&pageSize=3",
        "/svc/book/page?pageNum=1&pageSize=3",
        "/svc/dorm/building/list",
        "/svc/dorm/room/list",
    ]

    with ThreadPoolExecutor(max_workers=5) as pool:
        futures = [pool.submit(CampusClient().get, path) for path in paths for _ in range(2)]
        results = [future.result() for future in as_completed(futures)]

    for result in results:
        assert result.status_code < 500, result.response.text[:500]
        assert_api_contract(result, allow_error_code=True)


@pytest.mark.blackbox
@pytest.mark.performance
@pytest.mark.frontend
def test_frontend_routes_small_concurrency_baseline() -> None:
    paths = ["/", "/login", "/dashboard", "/campus/notice", "/education/timetable"]

    with ThreadPoolExecutor(max_workers=5) as pool:
        futures = [pool.submit(CampusClient().request_frontend, path) for path in paths for _ in range(2)]
        responses = [future.result() for future in as_completed(futures)]

    for response in responses:
        assert_http_ok(response, label="concurrent frontend route")
        assert "text/html" in response.headers.get("content-type", "").lower()

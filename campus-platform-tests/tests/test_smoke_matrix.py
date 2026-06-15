import pytest

from campus_tests.assertions import assert_api_contract_or_auth_required, assert_http_ok
from campus_tests.client import CampusClient


FRONTEND_SMOKE_MATRIX = [
    ("/", "app shell"),
    ("/login", "login route"),
    ("/dashboard", "dashboard route"),
    ("/campus/notice", "notice route"),
    ("/campus/book", "book route"),
    ("/education/timetable", "timetable route"),
]


API_SMOKE_MATRIX = [
    ("/auth/captcha", "captcha"),
    ("/svc/notice/page?pageNum=1&pageSize=1", "notice list"),
    ("/svc/book/page?pageNum=1&pageSize=1", "book list"),
    ("/svc/dorm/building/list", "dorm building list"),
    ("/svc/dorm/room/list", "dorm room list"),
]


@pytest.mark.blackbox
@pytest.mark.smoke
@pytest.mark.frontend
@pytest.mark.parametrize(("path", "label"), FRONTEND_SMOKE_MATRIX)
def test_frontend_smoke_matrix(path: str, label: str) -> None:
    response = CampusClient().request_frontend(path)

    assert_http_ok(response, label=label)
    assert "id=\"app\"" in response.text or "id='app'" in response.text


@pytest.mark.blackbox
@pytest.mark.smoke
@pytest.mark.api
@pytest.mark.parametrize(("path", "label"), API_SMOKE_MATRIX)
def test_api_smoke_matrix(path: str, label: str) -> None:
    result = CampusClient().get(path)

    assert_api_contract_or_auth_required(result, label=label)

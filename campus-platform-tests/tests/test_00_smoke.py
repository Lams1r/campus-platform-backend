import pytest

from campus_tests.assertions import assert_api_contract, assert_http_ok
from campus_tests.client import CampusClient


@pytest.mark.smoke
def test_frontend_home_is_reachable() -> None:
    client = CampusClient()
    response = client.request_frontend("/")

    assert_http_ok(response, label="frontend home")
    assert "text/html" in response.headers.get("content-type", "").lower()
    assert "<div id=\"app\"" in response.text or "<div id='app'" in response.text


@pytest.mark.smoke
@pytest.mark.api
def test_backend_captcha_is_reachable() -> None:
    client = CampusClient()
    result = client.get("/auth/captcha")

    assert_api_contract(result)
    assert isinstance(result.data, dict)
    assert result.data.get("captchaKey"), "captchaKey must be returned"
    assert result.data.get("captchaImage"), "captchaImage must be returned"

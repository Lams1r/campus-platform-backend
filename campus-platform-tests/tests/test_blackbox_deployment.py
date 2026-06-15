import pytest

from campus_tests.assertions import assert_http_ok
from campus_tests.client import CampusClient


@pytest.mark.blackbox
@pytest.mark.deployment
@pytest.mark.parametrize(
    "path",
    [
        "/",
        "/login",
        "/dashboard",
        "/campus/notice",
        "/education/course",
        "/system/user",
        "/this-route-does-not-exist",
    ],
)
def test_spa_fallback_supports_refresh_on_deep_links(path: str) -> None:
    response = CampusClient().request_frontend(path)

    assert_http_ok(response, label=f"SPA fallback {path}")
    assert "text/html" in response.headers.get("content-type", "").lower()
    assert "<html" in response.text.lower()
    assert "id=\"app\"" in response.text or "id='app'" in response.text


@pytest.mark.blackbox
@pytest.mark.deployment
def test_api_unknown_path_does_not_return_frontend_html() -> None:
    result = CampusClient().get("/blackbox-not-existing-api")
    content_type = result.response.headers.get("content-type", "").lower()
    body = result.response.text.lower()

    assert "text/html" not in content_type, "API errors should be JSON, not frontend index.html"
    assert "<div id=\"app\"" not in body and "<div id='app'" not in body


@pytest.mark.blackbox
@pytest.mark.deployment
def test_frontend_index_references_versioned_assets() -> None:
    response = CampusClient().request_frontend("/")
    assert_http_ok(response, label="frontend index")

    html = response.text
    assert "/assets/" in html, "production build should reference /assets/ files"
    assert ".js" in html, "production build should reference JavaScript bundle"
    assert ".css" in html, "production build should reference CSS bundle"


@pytest.mark.blackbox
@pytest.mark.deployment
def test_backend_api_prefix_is_available_under_target_domain() -> None:
    result = CampusClient().get("/auth/captcha")

    assert result.status_code < 500
    assert result.response.headers.get("content-type", "").lower().startswith("application/json")

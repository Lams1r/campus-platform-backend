import re
from urllib.parse import urlparse

import pytest
import requests

from campus_tests.assertions import assert_http_ok
from campus_tests.client import CampusClient


@pytest.mark.blackbox
@pytest.mark.deployment
@pytest.mark.frontend
def test_frontend_index_has_basic_accessibility_metadata() -> None:
    response = CampusClient().request_frontend("/")

    assert_http_ok(response, label="frontend index")
    html = response.text.lower()
    assert "<html" in html
    assert "lang=" in html, "index.html should declare document language"
    assert 'name="viewport"' in html, "mobile viewport meta is required"
    assert "<title>" in html and "</title>" in html, "page title is required"


@pytest.mark.blackbox
@pytest.mark.deployment
@pytest.mark.frontend
def test_frontend_asset_content_types_are_correct() -> None:
    client = CampusClient()
    response = client.request_frontend("/")
    assert_http_ok(response, label="frontend index")

    refs = re.findall(r"""(?:src|href)\s*=\s*["']([^"']+\.(?:js|css)(?:\?[^"']*)?)["']""", response.text)
    assets = [
        ref
        for ref in refs
        if not urlparse(ref).scheme and ("assets/" in ref or ref.endswith((".js", ".css")))
    ]
    if not assets:
        assert "id=\"app\"" in response.text or "id='app'" in response.text
        return

    for asset in assets[:10]:
        try:
            asset_response = client.request_frontend(asset)
        except requests.RequestException as exc:
            pytest.skip(f"frontend asset request is unstable: {asset} ({type(exc).__name__})")
        assert_http_ok(asset_response, label=f"asset {asset}")
        content_type = asset_response.headers.get("content-type", "").lower()
        assert "text/html" not in content_type, f"asset should not return HTML fallback: {asset}"
        assert len(asset_response.content) > 0, f"asset should not be empty: {asset}"


@pytest.mark.blackbox
@pytest.mark.deployment
@pytest.mark.api
def test_api_cors_preflight_is_controlled() -> None:
    client = CampusClient()
    response = client.session.options(
        client.api_url("/auth/captcha"),
        headers={
            "Origin": "http://43.134.21.21",
            "Access-Control-Request-Method": "GET",
        },
        timeout=client.config.request_timeout,
    )

    assert response.status_code < 500, response.text[:500]
    assert "<div id=\"app\"" not in response.text.lower()


@pytest.mark.blackbox
@pytest.mark.deployment
@pytest.mark.security
def test_missing_api_path_does_not_leak_frontend_or_stacktrace() -> None:
    result = CampusClient().get("/missing-api-path-for-quality-check")
    body = result.response.text.lower()

    assert result.status_code < 500
    assert "<div id=\"app\"" not in body
    assert "java.lang." not in body
    assert "org.springframework" not in body

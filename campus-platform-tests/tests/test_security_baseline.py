import pytest

from campus_tests.assertions import assert_api_contract, assert_http_ok
from campus_tests.client import CampusClient
from campus_tests.config import CONFIG


@pytest.mark.security
@pytest.mark.parametrize(
    "path",
    [
        "/actuator/env",
        "/actuator/heapdump",
        "/swagger-ui/index.html",
        "/v3/api-docs",
        "/../application.yml",
        "/uploads/../application.yml",
    ],
)
def test_sensitive_backend_paths_are_not_public(path: str) -> None:
    result = CampusClient().get(path)

    if path in {"/swagger-ui/index.html", "/v3/api-docs"} and not CONFIG.strict_mode:
        assert result.status_code < 500
        return

    if result.raw is not None:
        assert_api_contract(result, allow_error_code=True)
        assert result.code != 200, f"sensitive path should not be public: {path}"
    else:
        assert result.status_code in {401, 403, 404}, (
            f"sensitive path returned unexpected HTTP {result.status_code}: {path}"
        )


@pytest.mark.security
def test_error_response_does_not_expose_stacktrace() -> None:
    result = CampusClient().get("/not-exist-" + "x" * 32)
    body = result.response.text.lower()

    assert "java.lang." not in body
    assert "stacktrace" not in body
    assert "org.springframework" not in body


@pytest.mark.security
def test_frontend_security_headers_when_strict_mode_enabled() -> None:
    response = CampusClient().request_frontend("/")
    assert_http_ok(response, label="frontend home")

    headers = {key.lower(): value for key, value in response.headers.items()}
    if not CONFIG.strict_mode:
        assert "text/html" in response.headers.get("content-type", "").lower()
        return
    assert "x-content-type-options" in headers
    assert headers["x-content-type-options"].lower() == "nosniff"
    assert "x-frame-options" in headers or "content-security-policy" in headers
    assert "referrer-policy" in headers

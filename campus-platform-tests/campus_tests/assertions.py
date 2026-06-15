from __future__ import annotations

import re
from typing import Iterable

import requests

from .client import ApiResult
from .config import CONFIG


ISO_T_RE = re.compile(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}")


def assert_http_ok(response: requests.Response, *, label: str = "request") -> None:
    assert response.status_code < 400, (
        f"{label} returned HTTP {response.status_code}: "
        f"{response.text[:500]}"
    )


def assert_api_contract(result: ApiResult, *, allow_error_code: bool = False) -> None:
    assert result.status_code < 500, (
        f"server error HTTP {result.status_code}: {result.response.text[:500]}"
    )
    assert isinstance(result.raw, dict), (
        "API must return JSON object shaped as {code,msg,data}; "
        f"got {result.response.text[:500]}"
    )
    assert "code" in result.raw, f"API response missing code: {result.raw}"
    assert "msg" in result.raw, f"API response missing msg: {result.raw}"
    if not allow_error_code:
        assert result.code == 200, f"API code={result.code}, msg={result.msg}, raw={result.raw}"


def assert_auth_required(result: ApiResult, *, label: str = "API") -> None:
    assert_api_contract(result, allow_error_code=True)
    assert result.code in {401, 403} or result.status_code in {401, 403}, (
        f"{label} should require login; got HTTP {result.status_code}, code={result.code}, msg={result.msg}"
    )


def assert_api_contract_or_auth_required(result: ApiResult, *, label: str = "API") -> None:
    if result.code in {401, 403} or result.status_code in {401, 403}:
        assert_auth_required(result, label=label)
        return
    assert_api_contract(result)


def assert_no_iso_timestamps(value: object) -> None:
    if not CONFIG.strict_mode:
        return
    text = str(value)
    assert not ISO_T_RE.search(text), f"response contains unformatted ISO datetime with T: {text[:500]}"


def assert_any_text(html: str, candidates: Iterable[str]) -> None:
    assert any(candidate in html for candidate in candidates), (
        "frontend HTML did not contain any expected marker: "
        + ", ".join(candidates)
    )

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Any
from urllib.parse import urljoin

import requests

from .config import CONFIG, TestConfig


TOKEN_CACHE: dict[str, str] = {}
INVALID_TOKEN_ROLES: set[str] = set()


@dataclass
class ApiResult:
    status_code: int
    code: int | None
    msg: str | None
    data: Any
    raw: Any
    response: requests.Response

    @property
    def ok(self) -> bool:
        return self.status_code < 400 and self.code in {None, 200}


class CampusClient:
    def __init__(self, config: TestConfig = CONFIG, token: str = "") -> None:
        self.config = config
        self.session = requests.Session()
        self.session.headers.update({"User-Agent": "campus-platform-blackbox-tests/1.0"})
        if token:
            self.session.headers.update({self.config.auth_header_name: token})

    def frontend_url(self, path: str = "/") -> str:
        return _join(self.config.target_base_url, path)

    def api_url(self, path: str) -> str:
        return _join(self.config.api_base_url, path)

    def request_frontend(self, path: str = "/", **kwargs: Any) -> requests.Response:
        return self.session.get(
            self.frontend_url(path),
            timeout=kwargs.pop("timeout", self.config.request_timeout),
            allow_redirects=kwargs.pop("allow_redirects", True),
            **kwargs,
        )

    def request_api(self, method: str, path: str, **kwargs: Any) -> ApiResult:
        response = self.session.request(
            method.upper(),
            self.api_url(path),
            timeout=kwargs.pop("timeout", self.config.request_timeout),
            **kwargs,
        )
        raw: Any
        try:
            raw = response.json()
        except ValueError:
            raw = None

        if isinstance(raw, dict):
            code = raw.get("code")
            msg = raw.get("msg")
            data = raw.get("data")
        else:
            code = None
            msg = None
            data = raw

        return ApiResult(
            status_code=response.status_code,
            code=code,
            msg=msg,
            data=data,
            raw=raw,
            response=response,
        )

    def get(self, path: str, **kwargs: Any) -> ApiResult:
        return self.request_api("GET", path, **kwargs)

    def post(self, path: str, **kwargs: Any) -> ApiResult:
        return self.request_api("POST", path, **kwargs)

    def put(self, path: str, **kwargs: Any) -> ApiResult:
        return self.request_api("PUT", path, **kwargs)

    def delete(self, path: str, **kwargs: Any) -> ApiResult:
        return self.request_api("DELETE", path, **kwargs)


def client_for(role: str = "") -> CampusClient:
    token = token_for_role(role) if role else ""
    return CampusClient(CONFIG, token=token)


def token_for_role(role: str) -> str:
    if not role:
        return ""
    if role in INVALID_TOKEN_ROLES:
        return ""

    configured_token = CONFIG.token_for(role)
    if configured_token and token_is_valid(configured_token):
        return configured_token

    if role in TOKEN_CACHE:
        return TOKEN_CACHE[role]

    token = auto_login_token(role)
    if token and token_is_valid(token):
        TOKEN_CACHE[role] = token
    else:
        INVALID_TOKEN_ROLES.add(role)
        token = ""
    return token


def token_is_valid(token: str) -> bool:
    if not token:
        return False
    client = CampusClient(CONFIG, token=token)
    try:
        result = client.get("/auth/userInfo")
    except requests.RequestException:
        return False
    return result.status_code < 500 and result.code == 200 and isinstance(result.data, dict)


def auto_login_token(role: str) -> str:
    prefix = role.upper()
    username = os.getenv(f"{prefix}_USERNAME", "").strip()
    password = os.getenv(f"{prefix}_PASSWORD", "").strip()

    if not username or not password:
        return ""

    token = auto_login_with_test_bypass(username, password)
    if token:
        return token

    return ""


def auto_login_with_test_bypass(username: str, password: str) -> str:
    secret = os.getenv("TEST_LOGIN_SECRET", "").strip()
    if not secret:
        return ""
    captcha_key = os.getenv("TEST_LOGIN_CAPTCHA_KEY", "__campus_test_login__").strip()
    captcha_code = os.getenv("TEST_LOGIN_CAPTCHA_CODE", "__campus_test_login__").strip()
    session = requests.Session()
    session.headers.update({
        "User-Agent": "campus-platform-blackbox-tests/1.0",
        "X-Test-Login-Secret": secret,
    })
    return request_login_token(session, username, password, captcha_key, captcha_code)


def request_login_token(
    session: requests.Session,
    username: str,
    password: str,
    captcha_key: str,
    captcha_code: str,
) -> str:
    response = session.post(
        _join(CONFIG.api_base_url, "/auth/login"),
        json={
            "username": username,
            "password": password,
            "captchaCode": captcha_code,
            "captchaKey": captcha_key,
        },
        timeout=CONFIG.request_timeout,
    )

    try:
        raw = response.json()
    except ValueError:
        return ""

    if response.status_code >= 400 or not isinstance(raw, dict) or raw.get("code") != 200:
        return ""

    data = raw.get("data")
    if not isinstance(data, dict):
        return ""
    token = data.get("token", "")
    return token if isinstance(token, str) else ""


def _join(base: str, path: str) -> str:
    return urljoin(base.rstrip("/") + "/", path.lstrip("/"))

from __future__ import annotations

import argparse
import base64
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from campus_tests.client import CampusClient  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate a Campus Platform login token.")
    parser.add_argument("--role", choices=["admin", "teacher", "student"], required=True)
    parser.add_argument("--username", default="")
    parser.add_argument("--password", default="")
    parser.add_argument("--auto", action="store_true", help="Use test-login bypass instead of manual captcha.")
    args = parser.parse_args()

    role_prefix = args.role.upper()
    username = args.username or os.getenv(f"{role_prefix}_USERNAME", "")
    password = args.password or os.getenv(f"{role_prefix}_PASSWORD", "")

    if not username or not password:
        print(f"Missing account. Set {role_prefix}_USERNAME/{role_prefix}_PASSWORD or pass CLI args.")
        return 2

    client = CampusClient()
    headers = {}
    if args.auto:
        secret = os.getenv("TEST_LOGIN_SECRET", "")
        if not secret:
            print("Missing TEST_LOGIN_SECRET. Set it in .env or run without --auto.")
            return 2
        captcha_key = os.getenv("TEST_LOGIN_CAPTCHA_KEY", "__campus_test_login__")
        captcha_code = os.getenv("TEST_LOGIN_CAPTCHA_CODE", "__campus_test_login__")
        headers["X-Test-Login-Secret"] = secret
    else:
        captcha = client.get("/auth/captcha")
        if captcha.code != 200 or not isinstance(captcha.data, dict):
            print(f"Failed to fetch captcha: HTTP {captcha.status_code}, body={captcha.raw}")
            return 1

        captcha_key = captcha.data.get("captchaKey", "")
        captcha_image = captcha.data.get("captchaImage", "")
        if not captcha_key or not captcha_image:
            print(f"Invalid captcha payload: {captcha.raw}")
            return 1

        image_path = ROOT / f"captcha-{args.role}.png"
        save_captcha_image(captcha_image, image_path)
        print(f"Captcha image saved: {image_path}")
        captcha_code = input("Enter captcha code: ").strip()

    login = client.post(
        "/auth/login",
        json={
            "username": username,
            "password": password,
            "captchaCode": captcha_code,
            "captchaKey": captcha_key,
        },
        headers=headers,
    )
    if login.code != 200 or not isinstance(login.data, dict):
        print(f"Login failed: HTTP {login.status_code}, body={login.raw}")
        return 1

    token = login.data.get("token", "")
    if not token:
        print(f"Login response did not contain token: {login.raw}")
        return 1

    print()
    print(f"{role_prefix}_TOKEN={token}")
    return 0


def save_captcha_image(raw_image: str, image_path: Path) -> None:
    payload = raw_image
    if "," in raw_image and raw_image.lower().startswith("data:"):
        payload = raw_image.split(",", 1)[1]
    image_path.write_bytes(base64.b64decode(payload))


if __name__ == "__main__":
    raise SystemExit(main())

import pytest

from campus_tests.assertions import assert_api_contract, assert_auth_required, assert_no_iso_timestamps
from campus_tests.client import CampusClient


PAGE_ENDPOINTS = [
    "/svc/notice/page?pageNum=1&pageSize=5",
    "/svc/book/page?pageNum=1&pageSize=5",
]


@pytest.mark.blackbox
@pytest.mark.contract
@pytest.mark.api
@pytest.mark.parametrize("path", PAGE_ENDPOINTS)
def test_public_page_response_shape(path: str) -> None:
    result = CampusClient().get(path)

    if result.code in {401, 403} or result.status_code in {401, 403}:
        assert_auth_required(result, label=path)
        return
    assert_api_contract(result)
    assert_page_shape(result.data)
    assert_no_iso_timestamps(result.raw)


@pytest.mark.blackbox
@pytest.mark.contract
@pytest.mark.api
def test_captcha_contract_contains_base64_image() -> None:
    result = CampusClient().get("/auth/captcha")

    assert_api_contract(result)
    assert isinstance(result.data, dict)
    assert isinstance(result.data.get("captchaKey"), str)
    assert len(result.data["captchaKey"]) >= 8
    image = result.data.get("captchaImage", "")
    assert isinstance(image, str)
    assert len(image) > 100
    assert image.startswith("data:image") or all(ch.isalnum() or ch in "+/=" for ch in image[:100])


def assert_page_shape(data: object) -> None:
    assert isinstance(data, dict), f"page data should be object, got {type(data).__name__}"
    assert "records" in data, f"page data missing records: {data}"
    assert isinstance(data["records"], list), f"records should be list: {data}"
    assert any(key in data for key in ["total", "current", "pageNum", "pageSize"]), (
        f"page data missing pagination metadata: {data}"
    )

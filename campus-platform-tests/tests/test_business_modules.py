import pytest

from campus_tests.assertions import assert_api_contract, assert_auth_required, assert_no_iso_timestamps
from campus_tests.client import CampusClient, client_for, token_for_role


BUSINESS_PAGE_ENDPOINTS = [
    "/sys/user/page?pageNum=1&pageSize=5",
    "/edu/course/page?pageNum=1&pageSize=5",
    "/svc/repair/page?pageNum=1&pageSize=5",
]


@pytest.mark.business
@pytest.mark.parametrize("path", BUSINESS_PAGE_ENDPOINTS)
def test_business_page_response_shape(path: str) -> None:
    if not token_for_role("admin"):
        assert_auth_required(CampusClient().get(path), label=path)
        return
    result = client_for("admin").get(path)

    assert_api_contract(result)
    assert_page_shape(result.data)
    assert_no_iso_timestamps(result.raw)


def assert_page_shape(data: object) -> None:
    assert isinstance(data, dict), f"page data should be object, got {type(data).__name__}"
    assert "records" in data, f"page data missing records: {data}"
    assert isinstance(data["records"], list), f"records should be list: {data}"
    assert any(key in data for key in ["total", "current", "pageNum", "pageSize"]), (
        f"page data missing pagination metadata: {data}"
    )

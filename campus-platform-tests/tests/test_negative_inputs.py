import pytest

from campus_tests.assertions import assert_api_contract
from campus_tests.client import CampusClient


@pytest.mark.blackbox
@pytest.mark.negative
@pytest.mark.api
@pytest.mark.parametrize(
    "path",
    [
        "/svc/notice/page?pageNum=-1&pageSize=5",
        "/svc/notice/page?pageNum=1&pageSize=99999",
        "/svc/book/page?pageNum=abc&pageSize=5",
        "/svc/book/page?pageNum=1&pageSize=abc",
    ],
)
def test_invalid_pagination_parameters_are_controlled(path: str) -> None:
    result = CampusClient().get(path)

    assert result.status_code < 500, result.response.text[:500]
    if result.raw is not None:
        assert_api_contract(result, allow_error_code=True)


@pytest.mark.blackbox
@pytest.mark.negative
@pytest.mark.auth
@pytest.mark.parametrize("token", ["bad-token", "Bearer bad-token", "00000000-0000-0000-0000-000000000000"])
def test_invalid_tokens_are_rejected(token: str) -> None:
    result = CampusClient(token=token).get("/auth/userInfo")

    assert_api_contract(result, allow_error_code=True)
    assert result.code in {401, 403} or result.status_code in {401, 403}


@pytest.mark.blackbox
@pytest.mark.negative
@pytest.mark.security
@pytest.mark.parametrize(
    "keyword",
    [
        "' OR '1'='1",
        "<script>alert(1)</script>",
        "../../../application.yml",
        "%00",
    ],
)
def test_search_like_inputs_do_not_crash_or_reflect_dangerous_content(keyword: str) -> None:
    result = CampusClient().get("/svc/notice/page", params={"pageNum": 1, "pageSize": 5, "title": keyword})

    assert result.status_code < 500, result.response.text[:500]
    text = result.response.text.lower()
    assert "java.lang." not in text
    assert "org.springframework" not in text
    assert "<script>alert(1)</script>" not in text


@pytest.mark.blackbox
@pytest.mark.negative
@pytest.mark.api
def test_wrong_http_method_is_controlled() -> None:
    result = CampusClient().post("/auth/captcha")

    assert result.status_code in {400, 405} or result.code in {400, 405, 500}
    body = result.response.text.lower()
    assert "java.lang." not in body
    assert "stacktrace" not in body

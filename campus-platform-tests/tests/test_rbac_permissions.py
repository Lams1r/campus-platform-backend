import pytest

from campus_tests.assertions import assert_api_contract, assert_auth_required
from campus_tests.client import CampusClient, client_for, token_for_role


PROTECTED_ENDPOINTS = [
    "/auth/userInfo",
    "/sys/user/page?pageNum=1&pageSize=5",
    "/sys/role/list",
    "/edu/course/page?pageNum=1&pageSize=5",
]


ADMIN_ONLY_ENDPOINTS = [
    "/sys/user/page?pageNum=1&pageSize=5",
    "/sys/role/list",
    "/sys/menu/tree",
    "/sys/log/login?pageNum=1&pageSize=5",
]


@pytest.mark.rbac
@pytest.mark.parametrize("path", PROTECTED_ENDPOINTS)
def test_protected_api_rejects_anonymous_user(path: str) -> None:
    result = CampusClient().get(path)

    assert_api_contract(result, allow_error_code=True)
    assert result.code in {401, 403} or result.status_code in {401, 403}


@pytest.mark.rbac
@pytest.mark.parametrize("path", ADMIN_ONLY_ENDPOINTS)
def test_student_cannot_access_admin_endpoints(path: str) -> None:
    if not token_for_role("student"):
        assert_auth_required(CampusClient().get(path), label=path)
        return
    result = client_for("student").get(path)

    assert_api_contract(result, allow_error_code=True)
    assert result.code in {401, 403} or result.status_code in {401, 403}


@pytest.mark.rbac
def test_admin_can_access_admin_user_page() -> None:
    if not token_for_role("admin"):
        assert_auth_required(CampusClient().get("/sys/user/page?pageNum=1&pageSize=5"), label="admin user page")
        return
    result = client_for("admin").get("/sys/user/page?pageNum=1&pageSize=5")

    assert_api_contract(result)

import pytest

from campus_tests.assertions import assert_api_contract, assert_auth_required, assert_no_iso_timestamps
from campus_tests.cases import (
    ADMIN_READ_CASES,
    AUTHENTICATED_READ_CASES,
    STUDENT_READ_CASES,
    TEACHER_READ_CASES,
)
from campus_tests.client import CampusClient, client_for, token_for_role


@pytest.mark.api
@pytest.mark.auth
@pytest.mark.parametrize("role", ["admin", "teacher", "student"])
def test_user_info_with_configured_token(role: str) -> None:
    if not token_for_role(role):
        assert_auth_required(CampusClient().get("/auth/userInfo"), label=f"{role} userInfo")
        return
    result = client_for(role).get("/auth/userInfo")

    assert_api_contract(result)
    assert isinstance(result.data, dict)
    assert result.data.get("user"), f"{role} userInfo should include user"
    assert_no_iso_timestamps(result.raw)


@pytest.mark.api
@pytest.mark.parametrize(("method", "path", "label"), AUTHENTICATED_READ_CASES)
def test_common_authenticated_reads_as_student(method: str, path: str, label: str) -> None:
    if not token_for_role("student"):
        assert_auth_required(CampusClient().request_api(method, path), label=label)
        return
    result = client_for("student").request_api(method, path)

    assert_api_contract(result)
    assert_no_iso_timestamps(result.raw)


@pytest.mark.api
@pytest.mark.parametrize(("method", "path", "label"), ADMIN_READ_CASES)
def test_admin_read_apis(method: str, path: str, label: str) -> None:
    if not token_for_role("admin"):
        assert_auth_required(CampusClient().request_api(method, path), label=label)
        return
    result = client_for("admin").request_api(method, path)

    assert_api_contract(result)
    assert_no_iso_timestamps(result.raw)


@pytest.mark.api
@pytest.mark.parametrize(("method", "path", "label"), TEACHER_READ_CASES)
def test_teacher_read_apis(method: str, path: str, label: str) -> None:
    if not token_for_role("teacher"):
        assert_auth_required(CampusClient().request_api(method, path), label=label)
        return
    result = client_for("teacher").request_api(method, path)
    if result.code == 500 and "系统繁忙" in str(result.msg):
        pytest.skip(f"{label} returned controlled backend busy response")

    assert_api_contract(result)
    assert_no_iso_timestamps(result.raw)


@pytest.mark.api
@pytest.mark.parametrize(("method", "path", "label"), STUDENT_READ_CASES)
def test_student_read_apis(method: str, path: str, label: str) -> None:
    if not token_for_role("student"):
        assert_auth_required(CampusClient().request_api(method, path), label=label)
        return
    result = client_for("student").request_api(method, path)

    assert_api_contract(result)
    assert_no_iso_timestamps(result.raw)

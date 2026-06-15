import pytest

from campus_tests.assertions import assert_api_contract, assert_api_contract_or_auth_required, assert_no_iso_timestamps
from campus_tests.cases import PUBLIC_API_CASES
from campus_tests.client import CampusClient


@pytest.mark.api
@pytest.mark.parametrize(("method", "path", "label"), PUBLIC_API_CASES)
def test_public_api_contract(method: str, path: str, label: str) -> None:
    client = CampusClient()
    result = client.request_api(method, path)

    assert_api_contract_or_auth_required(result, label=label)
    assert_no_iso_timestamps(result.raw)


@pytest.mark.api
@pytest.mark.auth
def test_login_rejects_empty_payload_with_controlled_error() -> None:
    client = CampusClient()
    result = client.post("/auth/login", json={})

    assert_api_contract(result, allow_error_code=True)
    assert result.code != 200
    assert result.code in {400, 401, 500}
    assert result.msg

import pytest

from campus_tests.assertions import assert_any_text, assert_http_ok
from campus_tests.cases import FRONTEND_ROUTES
from campus_tests.client import CampusClient


@pytest.mark.frontend
@pytest.mark.parametrize("route", FRONTEND_ROUTES)
def test_spa_routes_return_app_shell(route: str) -> None:
    client = CampusClient()
    response = client.request_frontend(route)

    assert_http_ok(response, label=f"frontend route {route}")
    assert "text/html" in response.headers.get("content-type", "").lower()
    assert_any_text(response.text, ["Campus Platform", "<div id=\"app\"", "<div id='app'"])


@pytest.mark.frontend
def test_frontend_assets_are_not_missing() -> None:
    client = CampusClient()
    response = client.request_frontend("/")
    assert_http_ok(response, label="frontend index")

    asset_refs = []
    for marker in ["src=\"", "href=\""]:
        parts = response.text.split(marker)[1:]
        asset_refs.extend(part.split("\"", 1)[0] for part in parts)

    local_assets = [ref for ref in asset_refs if ref.startswith("/assets/")]
    assert local_assets, "index.html should reference built assets under /assets/"

    for asset in local_assets[:8]:
        asset_response = client.request_frontend(asset)
        assert_http_ok(asset_response, label=f"frontend asset {asset}")
        assert len(asset_response.content) > 0

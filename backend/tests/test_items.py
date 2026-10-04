"""BE-06..BE-21 item CRUD, persistence, OpenAPI and robustness checks."""

from collections.abc import Callable

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.config import Settings

ITEM_FIELDS = {"id", "list_id", "name", "quantity", "bought", "created_at", "updated_at"}


def _create_list(client: TestClient, name: str = "Weekend groceries") -> str:
    response = client.post("/lists", json={"name": name})
    assert response.status_code == 201
    return response.json()["id"]


def _create_item(client: TestClient, list_id: str, **overrides: object) -> dict:
    payload: dict[str, object] = {"name": "milk", "quantity": "2 bottles"}
    payload.update(overrides)
    response = client.post(f"/lists/{list_id}/items", json=payload)
    assert response.status_code == 201
    return response.json()


def test_get_one_list_with_items(client: TestClient) -> None:
    """BE-06 — GET /lists/{id} returns the list plus its items."""

    list_id = _create_list(client)
    _create_item(client, list_id, name="milk", quantity="2 bottles")

    response = client.get(f"/lists/{list_id}")

    assert response.status_code == 200
    body = response.json()
    assert body["id"] == list_id
    assert len(body["items"]) == 1
    assert body["items"][0]["name"] == "milk"
    assert body["items"][0]["quantity"] == "2 bottles"


def test_get_one_list_unknown_id(client: TestClient) -> None:
    """BE-07 — GET /lists/{id} returns 404 for an unknown list."""

    response = client.get("/lists/does-not-exist")

    assert response.status_code == 404


def test_add_item_returns_full_shape(client: TestClient) -> None:
    """BE-08 — POST /lists/{id}/items returns 201 with the full item shape."""

    list_id = _create_list(client)

    response = client.post(
        f"/lists/{list_id}/items",
        json={"name": "milk", "quantity": "2 bottles"},
    )

    assert response.status_code == 201
    body = response.json()
    assert set(body) == ITEM_FIELDS
    assert body["list_id"] == list_id
    assert body["name"] == "milk"
    assert body["quantity"] == "2 bottles"
    assert body["bought"] is False
    assert body["created_at"]
    assert body["updated_at"]


def test_add_item_without_quantity(client: TestClient) -> None:
    """BE-09 — quantity is optional and stored as null when omitted."""

    list_id = _create_list(client)

    response = client.post(f"/lists/{list_id}/items", json={"name": "bread"})

    assert response.status_code == 201
    assert response.json()["quantity"] is None


def test_add_item_unknown_list(client: TestClient) -> None:
    """BE-10 — POST to an unknown list returns 404 (checked before insert)."""

    response = client.post("/lists/nope/items", json={"name": "milk"})

    assert response.status_code == 404


def test_add_item_empty_name_is_rejected(client: TestClient) -> None:
    """BE-11 — empty item name returns the locked 422."""

    list_id = _create_list(client)

    response = client.post(f"/lists/{list_id}/items", json={"name": ""})

    assert response.status_code == 422


def test_add_item_whitespace_only_name_is_rejected(client: TestClient) -> None:
    """OQ-7 — whitespace-only item name returns 422."""

    list_id = _create_list(client)

    response = client.post(f"/lists/{list_id}/items", json={"name": "   "})

    assert response.status_code == 422


def test_add_item_name_max_length_boundary(client: TestClient) -> None:
    """OQ-8 — item name accepts 200 chars, rejects 201 chars (422)."""

    list_id = _create_list(client)

    accepted = client.post(f"/lists/{list_id}/items", json={"name": "a" * 200})
    rejected = client.post(f"/lists/{list_id}/items", json={"name": "a" * 201})

    assert accepted.status_code == 201
    assert rejected.status_code == 422


def test_toggle_bought(client: TestClient) -> None:
    """BE-12 — PATCH bought toggles true then false."""

    list_id = _create_list(client)
    item = _create_item(client, list_id)

    bought = client.patch(f"/items/{item['id']}", json={"bought": True})
    assert bought.status_code == 200
    assert bought.json()["bought"] is True

    not_bought = client.patch(f"/items/{item['id']}", json={"bought": False})
    assert not_bought.status_code == 200
    assert not_bought.json()["bought"] is False


def test_rename_item_keeps_other_fields(client: TestClient) -> None:
    """BE-13 — renaming an item leaves quantity/bought unchanged."""

    list_id = _create_list(client)
    item = _create_item(client, list_id, name="milk", quantity="2 bottles")

    response = client.patch(f"/items/{item['id']}", json={"name": "whole milk"})

    assert response.status_code == 200
    body = response.json()
    assert body["name"] == "whole milk"
    assert body["quantity"] == "2 bottles"
    assert body["bought"] is False


def test_update_quantity(client: TestClient) -> None:
    """BE-14 — PATCH updates the quantity."""

    list_id = _create_list(client)
    item = _create_item(client, list_id, quantity="2 bottles")

    response = client.patch(f"/items/{item['id']}", json={"quantity": "1 bottle"})

    assert response.status_code == 200
    assert response.json()["quantity"] == "1 bottle"


def test_patch_unknown_item(client: TestClient) -> None:
    """BE-15 — PATCH on an unknown item returns 404."""

    response = client.patch("/items/nope", json={"bought": True})

    assert response.status_code == 404


def test_patch_empty_body_is_a_noop(client: TestClient) -> None:
    """LP-1 — PATCH with an empty body changes nothing and returns 200."""

    list_id = _create_list(client)
    item = _create_item(client, list_id, name="milk", quantity="2 bottles")

    response = client.patch(f"/items/{item['id']}", json={})

    assert response.status_code == 200
    body = response.json()
    assert body["name"] == "milk"
    assert body["quantity"] == "2 bottles"
    assert body["bought"] is False


def test_patch_partial_update_only_changes_supplied_fields(client: TestClient) -> None:
    """BE-21 / LP-1 — each partial PATCH leaves the other fields intact."""

    list_id = _create_list(client)
    item = _create_item(client, list_id, name="milk", quantity="2 bottles")

    renamed = client.patch(f"/items/{item['id']}", json={"name": "whole milk"})
    assert renamed.status_code == 200
    renamed_body = renamed.json()
    assert renamed_body["name"] == "whole milk"
    assert renamed_body["quantity"] == "2 bottles"
    assert renamed_body["bought"] is False

    requantified = client.patch(f"/items/{item['id']}", json={"quantity": "q"})
    assert requantified.status_code == 200
    body = requantified.json()
    assert body["name"] == "whole milk"
    assert body["quantity"] == "q"
    assert body["bought"] is False


def test_delete_item(client: TestClient) -> None:
    """BE-16 — DELETE returns 204 and the item is gone afterwards."""

    list_id = _create_list(client)
    item = _create_item(client, list_id)

    response = client.delete(f"/items/{item['id']}")

    assert response.status_code == 204
    assert response.content == b""
    remaining = client.get(f"/lists/{list_id}").json()["items"]
    assert remaining == []


def test_delete_unknown_item(client: TestClient) -> None:
    """BE-17 — DELETE on an unknown item returns 404."""

    response = client.delete("/items/nope")

    assert response.status_code == 404


def test_persistence_survives_app_and_engine_recreation(
    make_app: Callable[[Settings | None], FastAPI],
    settings: Settings,
) -> None:
    """BE-18 — data written by one app is visible to a fresh app on the same file DB."""

    with TestClient(make_app()) as first_client:
        list_id = _create_list(first_client, name="Persisted")
        item = _create_item(first_client, list_id, name="milk")

    with TestClient(make_app(settings)) as second_client:
        response = second_client.get(f"/lists/{list_id}")

        assert response.status_code == 200
        body = response.json()
        assert body["name"] == "Persisted"
        assert [entry["id"] for entry in body["items"]] == [item["id"]]


def test_openapi_schema_is_available(client: TestClient) -> None:
    """BE-19 — GET /openapi.json returns 200 with the documented paths."""

    response = client.get("/openapi.json")

    assert response.status_code == 200
    paths = response.json()["paths"]
    assert "/health" in paths
    assert "/lists" in paths
    assert "/lists/{list_id}" in paths
    assert "/lists/{list_id}/items" in paths
    assert "/items/{item_id}" in paths


def test_unknown_route_returns_404_not_500(client: TestClient) -> None:
    """LP-4 — an unknown route yields a predictable 404, never a 500."""

    response = client.get("/totally-unknown-route")

    assert response.status_code == 404


def test_malformed_json_returns_422_not_500(client: TestClient) -> None:
    """LP-4 — malformed JSON yields a predictable 422, never a 500."""

    response = client.post(
        "/lists",
        content="{not valid json",
        headers={"content-type": "application/json"},
    )

    assert response.status_code == 422


def test_method_not_allowed_returns_405_not_500(client: TestClient) -> None:
    """LP-4 — an unsupported method yields 405, never a 500."""

    response = client.delete("/lists")

    assert response.status_code == 405

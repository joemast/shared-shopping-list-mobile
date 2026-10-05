"""BE-02..BE-05: shopping list creation and retrieval."""

from fastapi.testclient import TestClient

LIST_FIELDS = {"id", "name", "created_at", "updated_at", "items"}


def test_create_list_returns_created_shape(client: TestClient) -> None:
    """BE-02 — POST /lists returns 201 with the documented fields and empty items."""

    response = client.post("/lists", json={"name": "Weekend groceries"})

    assert response.status_code == 201
    body = response.json()
    assert set(body) == LIST_FIELDS
    assert body["name"] == "Weekend groceries"
    assert body["items"] == []
    assert body["id"]
    assert body["created_at"]
    assert body["updated_at"]


def test_create_list_empty_name_is_rejected(client: TestClient) -> None:
    """BE-03 — POST /lists with an empty name returns 422."""

    response = client.post("/lists", json={"name": ""})

    assert response.status_code == 422


def test_create_list_whitespace_only_name_is_rejected(client: TestClient) -> None:
    """OQ-7 — whitespace-only names are treated as empty and rejected with 422."""

    response = client.post("/lists", json={"name": "   "})

    assert response.status_code == 422


def test_create_list_missing_name_is_rejected(client: TestClient) -> None:
    """A missing name is an invalid body (422)."""

    response = client.post("/lists", json={})

    assert response.status_code == 422


def test_create_list_name_max_length_boundary(client: TestClient) -> None:
    """OQ-8 — 200-character names are accepted, 201-character names rejected (422)."""

    accepted = client.post("/lists", json={"name": "a" * 200})
    rejected = client.post("/lists", json={"name": "a" * 201})

    assert accepted.status_code == 201
    assert rejected.status_code == 422


def test_get_lists_returns_all_with_items(client: TestClient) -> None:
    """BE-04 — GET /lists returns a 200 array; each list includes an items array."""

    client.post("/lists", json={"name": "One"})
    client.post("/lists", json={"name": "Two"})

    response = client.get("/lists")

    assert response.status_code == 200
    body = response.json()
    assert isinstance(body, list)
    assert len(body) == 2
    names = {item["name"] for item in body}
    assert names == {"One", "Two"}
    for shopping_list in body:
        assert isinstance(shopping_list["items"], list)


def test_get_lists_when_empty_returns_empty_array(client: TestClient) -> None:
    """BE-05 — GET /lists on an empty DB returns ``[]`` (not ``null``)."""

    response = client.get("/lists")

    assert response.status_code == 200
    assert response.json() == []

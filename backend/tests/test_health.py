"""BE-01: health endpoint."""

from fastapi.testclient import TestClient


def test_health_returns_ok(client: TestClient) -> None:
    """BE-01 — ``GET /health`` returns 200 with the locked body."""

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}

"""Shared pytest fixtures.

Every test gets a **file-backed** temp SQLite database (not in-memory) so the
persistence-across-restart test (BE-18) is meaningful (OQ-3).
"""

from collections.abc import Callable, Iterator
from pathlib import Path

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app


@pytest.fixture
def db_path(tmp_path: Path) -> Path:
    """Path to a per-test SQLite file."""

    return tmp_path / "test.db"


@pytest.fixture
def settings(db_path: Path) -> Settings:
    """Settings pointing at the per-test database file."""

    return Settings(database_url=f"sqlite:///{db_path}")


@pytest.fixture
def make_app(settings: Settings) -> Callable[[Settings | None], FastAPI]:
    """Factory that builds a fresh app against the same test database file."""

    def _make(override: Settings | None = None) -> FastAPI:
        return create_app(override or settings)

    return _make


@pytest.fixture
def app(make_app: Callable[[Settings | None], FastAPI]) -> FastAPI:
    """A fresh application instance for the current test."""

    return make_app()


@pytest.fixture
def client(app: FastAPI) -> Iterator[TestClient]:
    """A ``TestClient`` that runs the app lifespan (creates tables on entry)."""

    with TestClient(app) as test_client:
        yield test_client

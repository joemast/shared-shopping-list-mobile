"""Database engine, table bootstrap and session dependency.

An engine is created per application instance (no module-global engine) so
that each ``create_app`` call owns an isolated engine. ``get_session`` reads
the engine from ``app.state`` and can therefore be overridden per-app in tests
via ``app.dependency_overrides``.
"""

from collections.abc import Iterator
from pathlib import Path
from typing import Annotated

from fastapi import Depends, Request
from sqlalchemy.engine import Engine
from sqlmodel import Session, SQLModel, create_engine

_SQLITE_PREFIX = "sqlite:///"


def make_engine(database_url: str) -> Engine:
    """Create a SQLAlchemy engine for ``database_url``.

    For file-backed SQLite the parent directory is created on demand so the
    app can start even when the mounted ``data`` volume is empty.
    """

    connect_args: dict[str, object] = {}
    if database_url.startswith(_SQLITE_PREFIX):
        connect_args["check_same_thread"] = False
        db_path = database_url[len(_SQLITE_PREFIX) :]
        if db_path and db_path != ":memory:":
            Path(db_path).parent.mkdir(parents=True, exist_ok=True)
    return create_engine(database_url, connect_args=connect_args)


def init_db(engine: Engine) -> None:
    """Register the models and create the schema for a fresh engine."""

    import app.models  # noqa: F401  (import side effect registers tables)

    SQLModel.metadata.create_all(engine)


def get_session(request: Request) -> Iterator[Session]:
    """Yield a session bound to the engine stored on the application state."""

    engine: Engine = request.app.state.engine
    with Session(engine) as session:
        yield session


# Annotated dependency used by routers (avoids the B008 call-in-default lint).
SessionDep = Annotated[Session, Depends(get_session)]

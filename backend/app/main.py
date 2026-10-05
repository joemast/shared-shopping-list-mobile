"""FastAPI application factory and ASGI entrypoint.

``create_app(settings)`` builds a fully isolated application instance with its
own engine, which makes the persistence tests (BE-18) and per-test temp
database fixtures straightforward.
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import Settings, get_settings
from app.database import init_db, make_engine
from app.routers import health, items, lists


def create_app(settings: Settings | None = None) -> FastAPI:
    """Create and configure a FastAPI application instance."""

    settings = settings or get_settings()
    engine = make_engine(settings.database_url)

    @asynccontextmanager
    async def lifespan(application: FastAPI) -> AsyncIterator[None]:
        init_db(engine)
        try:
            yield
        finally:
            engine.dispose()

    application = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        lifespan=lifespan,
    )
    application.state.engine = engine
    application.state.settings = settings

    application.include_router(health.router)
    application.include_router(lists.router)
    application.include_router(items.router)
    return application


app = create_app()

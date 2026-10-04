"""Runtime configuration for the backend.

Kept intentionally tiny: the only knob required by the MVP is the SQLite
database URL, which is read from the environment so the same code works for
local runs, tests (file-backed temp DB) and the Docker/Compose deployment.
"""

import os

from pydantic import BaseModel

DEFAULT_DATABASE_URL = "sqlite:///./data/app.db"


class Settings(BaseModel):
    """Configuration values for a single application instance."""

    app_name: str = "Shared Shopping List API"
    database_url: str = DEFAULT_DATABASE_URL


def get_settings() -> Settings:
    """Build settings from the environment with sensible defaults."""

    return Settings(database_url=os.getenv("DATABASE_URL", DEFAULT_DATABASE_URL))

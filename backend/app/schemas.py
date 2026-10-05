"""Pydantic/SQLModel request and response schemas.

Contract decisions applied here (see the Stage 1 report):

* ``max_length=200`` on list/item names (OQ-8).
* Blank or whitespace-only names are rejected with ``422`` (OQ-7).
* ``PATCH`` is a partial update driven by ``exclude_unset`` (OQ-6).
"""

from datetime import datetime

from pydantic import ConfigDict, field_validator, model_validator
from sqlmodel import Field, SQLModel

NAME_MAX_LENGTH = 200


def _reject_blank_name(value: str) -> str:
    """Raise when a name is empty or only whitespace."""

    if not value or not value.strip():
        raise ValueError("name must not be empty")
    return value


class HealthStatus(SQLModel):
    """Response body for ``GET /health``."""

    status: str


class ShoppingItemRead(SQLModel):
    """Item as returned by the API."""

    model_config = ConfigDict(from_attributes=True)

    id: str
    list_id: str
    name: str
    quantity: str | None = None
    bought: bool
    created_at: datetime
    updated_at: datetime


class ShoppingListRead(SQLModel):
    """List as returned by the API, always including its items."""

    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    created_at: datetime
    updated_at: datetime
    items: list[ShoppingItemRead] = Field(default_factory=list)


class ShoppingListCreate(SQLModel):
    """Request body for ``POST /lists``."""

    name: str = Field(min_length=1, max_length=NAME_MAX_LENGTH)

    @field_validator("name")
    @classmethod
    def _validate_name(cls, value: str) -> str:
        return _reject_blank_name(value)


class ShoppingItemCreate(SQLModel):
    """Request body for ``POST /lists/{list_id}/items``."""

    name: str = Field(min_length=1, max_length=NAME_MAX_LENGTH)
    quantity: str | None = None

    @field_validator("name")
    @classmethod
    def _validate_name(cls, value: str) -> str:
        return _reject_blank_name(value)


class ShoppingItemUpdate(SQLModel):
    """Partial request body for ``PATCH /items/{item_id}``."""

    name: str | None = Field(default=None, max_length=NAME_MAX_LENGTH)
    quantity: str | None = None
    bought: bool | None = None

    @field_validator("name")
    @classmethod
    def _validate_name(cls, value: str | None) -> str | None:
        if value is None:
            return value
        return _reject_blank_name(value)

    @model_validator(mode="after")
    def _reject_explicit_null_name(self) -> "ShoppingItemUpdate":
        if "name" in self.model_fields_set and self.name is None:
            raise ValueError("name must not be null")
        return self

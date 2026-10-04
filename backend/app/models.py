"""SQLModel table models for the Shared Shopping List MVP."""

from datetime import UTC, datetime
from uuid import uuid4

from sqlmodel import Field, Relationship, SQLModel


def utcnow() -> datetime:
    """Timezone-aware UTC timestamp used for created/updated fields."""

    return datetime.now(UTC)


class ShoppingList(SQLModel, table=True):
    """A named shopping list that owns a collection of items."""

    __tablename__ = "shopping_lists"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    name: str = Field(max_length=200)
    created_at: datetime = Field(default_factory=utcnow)
    updated_at: datetime = Field(default_factory=utcnow)

    items: list["ShoppingItem"] = Relationship(
        back_populates="list",
        cascade_delete=True,
    )


class ShoppingItem(SQLModel, table=True):
    """An item belonging to a single shopping list."""

    __tablename__ = "shopping_items"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    list_id: str = Field(foreign_key="shopping_lists.id", index=True)
    name: str = Field(max_length=200)
    quantity: str | None = Field(default=None)
    bought: bool = Field(default=False)
    created_at: datetime = Field(default_factory=utcnow)
    updated_at: datetime = Field(default_factory=utcnow)

    list: ShoppingList | None = Relationship(back_populates="items")

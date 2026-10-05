"""Shopping list routes: create, list and fetch a single list with items.

These routes are registered *before* the item routes in ``app.main`` so the
``/lists/{list_id}`` path does not shadow ``/lists/{list_id}/items`` (R14).
"""

from fastapi import APIRouter, HTTPException, status
from sqlalchemy.orm import selectinload
from sqlmodel import select

from app.database import SessionDep
from app.models import ShoppingList
from app.schemas import ShoppingListCreate, ShoppingListRead

router = APIRouter(prefix="/lists", tags=["lists"])


def _to_read(shopping_list: ShoppingList) -> ShoppingListRead:
    """Convert a table row to its API representation (loads items eagerly)."""

    return ShoppingListRead.model_validate(shopping_list)


@router.post("", response_model=ShoppingListRead, status_code=status.HTTP_201_CREATED)
def create_list(payload: ShoppingListCreate, session: SessionDep) -> ShoppingListRead:
    """Create a list and return it with an empty ``items`` array (AC2)."""

    shopping_list = ShoppingList(name=payload.name)
    session.add(shopping_list)
    session.commit()
    session.refresh(shopping_list)
    return _to_read(shopping_list)


@router.get("", response_model=list[ShoppingListRead])
def get_lists(session: SessionDep) -> list[ShoppingListRead]:
    """Return all lists, each including its ``items`` array (AC3, OQ-13)."""

    statement = (
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .order_by(ShoppingList.created_at)
    )
    lists = session.exec(statement).all()
    return [_to_read(shopping_list) for shopping_list in lists]


@router.get("/{list_id}", response_model=ShoppingListRead)
def get_list(list_id: str, session: SessionDep) -> ShoppingListRead:
    """Return a single list with its items, or ``404`` when unknown (AC4)."""

    shopping_list = session.get(ShoppingList, list_id)
    if shopping_list is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Shopping list not found")
    session.refresh(shopping_list, attribute_names=["items"])
    return _to_read(shopping_list)

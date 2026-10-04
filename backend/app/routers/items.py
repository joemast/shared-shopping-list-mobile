"""Shopping item routes: add, partial-update and delete (AC5-AC9)."""

from fastapi import APIRouter, HTTPException, status

from app.database import SessionDep
from app.models import ShoppingItem, ShoppingList, utcnow
from app.schemas import ShoppingItemCreate, ShoppingItemRead, ShoppingItemUpdate

router = APIRouter(tags=["items"])


def _to_read(item: ShoppingItem) -> ShoppingItemRead:
    """Convert a table row to its API representation."""

    return ShoppingItemRead.model_validate(item)


@router.post(
    "/lists/{list_id}/items",
    response_model=ShoppingItemRead,
    status_code=status.HTTP_201_CREATED,
)
def create_item(
    list_id: str,
    payload: ShoppingItemCreate,
    session: SessionDep,
) -> ShoppingItemRead:
    """Add an item to an existing list (AC5).

    The list is checked *before* the insert so an unknown list yields ``404``
    rather than a foreign-key failure (R14). A blank name is rejected with
    ``422`` by the request schema (AC8).
    """

    if session.get(ShoppingList, list_id) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Shopping list not found")

    item = ShoppingItem(list_id=list_id, name=payload.name, quantity=payload.quantity)
    session.add(item)
    session.commit()
    session.refresh(item)
    return _to_read(item)


@router.patch("/items/{item_id}", response_model=ShoppingItemRead)
def update_item(
    item_id: str,
    payload: ShoppingItemUpdate,
    session: SessionDep,
) -> ShoppingItemRead:
    """Partially update name/quantity/bought using ``exclude_unset`` (AC6, OQ-6)."""

    item = session.get(ShoppingItem, item_id)
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Item not found")

    updates = payload.model_dump(exclude_unset=True)
    for field, value in updates.items():
        setattr(item, field, value)
    if updates:
        item.updated_at = utcnow()

    session.add(item)
    session.commit()
    session.refresh(item)
    return _to_read(item)


@router.delete("/items/{item_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_item(item_id: str, session: SessionDep) -> None:
    """Delete an item, returning ``204`` (or ``404`` when unknown) (AC7, OQ-2)."""

    item = session.get(ShoppingItem, item_id)
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Item not found")

    session.delete(item)
    session.commit()

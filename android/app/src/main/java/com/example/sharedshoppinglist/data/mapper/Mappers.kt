package com.example.sharedshoppinglist.data.mapper

import com.example.sharedshoppinglist.data.dto.ShoppingItemDto
import com.example.sharedshoppinglist.data.dto.ShoppingListDto
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.ShoppingList

/** Maps a wire [ShoppingListDto] (and its nested items) to the domain model. */
fun ShoppingListDto.toDomain(): ShoppingList = ShoppingList(
    id = id,
    name = name,
    items = items.map { it.toDomain() },
)

/** Maps a wire [ShoppingItemDto] to the domain model. */
fun ShoppingItemDto.toDomain(): ShoppingItem = ShoppingItem(
    id = id,
    listId = listId,
    name = name,
    quantity = quantity,
    bought = bought,
)

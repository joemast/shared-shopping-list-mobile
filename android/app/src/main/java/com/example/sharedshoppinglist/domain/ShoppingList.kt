package com.example.sharedshoppinglist.domain

/** A named shopping list with its (possibly empty) items. */
data class ShoppingList(
    val id: String,
    val name: String,
    val items: List<ShoppingItem> = emptyList(),
)

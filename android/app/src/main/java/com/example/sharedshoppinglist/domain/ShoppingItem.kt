package com.example.sharedshoppinglist.domain

/** An item belonging to a shopping list. [quantity] is optional free text. */
data class ShoppingItem(
    val id: String,
    val listId: String,
    val name: String,
    val quantity: String?,
    val bought: Boolean,
)

package com.example.sharedshoppinglist.data

import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.ShoppingList

/**
 * Data-access boundary between the app and the backend.
 *
 * It is an interface (not a concrete class) so both the JVM unit tests (fake
 * implementation) and the instrumented UI suite (MockWebServer-backed
 * implementation) can substitute a deterministic implementation without a DI
 * framework.
 */
interface ShoppingRepository {

    suspend fun health(): String

    suspend fun getLists(): List<ShoppingList>

    suspend fun createList(name: String): ShoppingList

    suspend fun getList(listId: String): ShoppingList

    suspend fun addItem(listId: String, name: String, quantity: String?): ShoppingItem

    /** Partial update: only non-null arguments are sent (rename / quantity / toggle). */
    suspend fun updateItem(
        itemId: String,
        name: String? = null,
        quantity: String? = null,
        bought: Boolean? = null,
    ): ShoppingItem

    suspend fun deleteItem(itemId: String)
}

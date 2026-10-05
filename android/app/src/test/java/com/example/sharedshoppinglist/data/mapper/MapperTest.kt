package com.example.sharedshoppinglist.data.mapper

import com.example.sharedshoppinglist.data.ApiClient
import com.example.sharedshoppinglist.data.dto.ShoppingItemDto
import com.example.sharedshoppinglist.data.dto.ShoppingListDto
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.ShoppingList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the DTO→domain mapping — AND-07..AND-10 (HP-12, R6).
 * Guards the backend↔Android contract: snake_case wire fields, nullable
 * `quantity`, and nested items.
 */
class MapperTest {

    @Test
    fun `AND-07 HP-12 item DTO maps every field with a non-null quantity`() {
        val dto = ShoppingItemDto(
            id = "i1",
            listId = "l1",
            name = "milk",
            quantity = "2 bottles",
            bought = false,
        )

        val domain = dto.toDomain()

        assertEquals(
            ShoppingItem(
                id = "i1",
                listId = "l1",
                name = "milk",
                quantity = "2 bottles",
                bought = false,
            ),
            domain,
        )
        assertEquals("2 bottles", domain.quantity)
    }

    @Test
    fun `AND-08 HP-12 item DTO with a null quantity maps to null`() {
        val dto = ShoppingItemDto(
            id = "i2",
            listId = "l1",
            name = "bread",
            quantity = null,
            bought = true,
        )

        val domain = dto.toDomain()

        assertNull(domain.quantity)
        assertTrue(domain.bought)
        assertEquals(
            ShoppingItem(id = "i2", listId = "l1", name = "bread", quantity = null, bought = true),
            domain,
        )
    }

    @Test
    fun `AND-09 list DTO maps nested items with the correct listId`() {
        val dto = ShoppingListDto(
            id = "l1",
            name = "Groceries",
            items = listOf(
                ShoppingItemDto(id = "i1", listId = "l1", name = "milk", quantity = "2 bottles"),
                ShoppingItemDto(id = "i2", listId = "l1", name = "bread", quantity = null, bought = true),
            ),
        )

        val domain = dto.toDomain()

        assertEquals("l1", domain.id)
        assertEquals("Groceries", domain.name)
        assertEquals(2, domain.items.size)
        assertTrue(domain.items.all { it.listId == "l1" })
        assertEquals("milk", domain.items[0].name)
        assertEquals("2 bottles", domain.items[0].quantity)
        assertNull(domain.items[1].quantity)
        assertTrue(domain.items[1].bought)
    }

    @Test
    fun `AND-10 snake_case JSON binds to DTO fields and unknown keys are ignored`() {
        // Fixture mirrors the backend wire contract exactly: snake_case keys
        // (`list_id`, `created_at`, `updated_at`) plus one key the DTO does not
        // model, to prove the production decoder tolerates it.
        val json = """
            {
              "id": "l1",
              "name": "Groceries",
              "created_at": "2024-06-01T10:00:00Z",
              "updated_at": "2024-06-02T11:00:00Z",
              "unexpected_field": 123,
              "items": [
                {
                  "id": "i1",
                  "list_id": "l1",
                  "name": "milk",
                  "quantity": "2 bottles",
                  "bought": false,
                  "created_at": "2024-06-01T10:05:00Z",
                  "updated_at": "2024-06-01T10:05:00Z"
                }
              ]
            }
        """.trimIndent()

        val dto = ApiClient.json.decodeFromString<ShoppingListDto>(json)

        assertEquals("l1", dto.id)
        assertEquals("Groceries", dto.name)
        assertEquals("2024-06-01T10:00:00Z", dto.createdAt)
        assertEquals("2024-06-02T11:00:00Z", dto.updatedAt)
        val item = dto.items.single()
        assertEquals("i1", item.id)
        assertEquals("l1", item.listId)
        assertEquals("2 bottles", item.quantity)
        assertEquals("2024-06-01T10:05:00Z", item.createdAt)
        assertEquals("2024-06-01T10:05:00Z", item.updatedAt)
    }
}

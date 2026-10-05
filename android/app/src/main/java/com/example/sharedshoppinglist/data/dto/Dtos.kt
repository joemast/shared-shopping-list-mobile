package com.example.sharedshoppinglist.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `GET /health`. */
@Serializable
data class HealthDto(
    val status: String,
)

/**
 * Wire model for a shopping list. JSON is `snake_case` on the backend, so the
 * KDoc-documented fields carry explicit [SerialName] mappings.
 */
@Serializable
data class ShoppingListDto(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val items: List<ShoppingItemDto> = emptyList(),
)

/** Wire model for a shopping item. [quantity] is nullable free text. */
@Serializable
data class ShoppingItemDto(
    val id: String,
    @SerialName("list_id") val listId: String,
    val name: String,
    val quantity: String? = null,
    val bought: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** Request body for `POST /lists`. */
@Serializable
data class CreateListRequest(
    val name: String,
)

/** Request body for `POST /lists/{id}/items`. */
@Serializable
data class CreateItemRequest(
    val name: String,
    val quantity: String? = null,
)

/**
 * Partial request body for `PATCH /items/{id}`. Null fields are omitted from
 * the JSON payload (see [com.example.sharedshoppinglist.data.ApiClient]) so a
 * single body can perform rename, quantity change, or toggle independently.
 */
@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val quantity: String? = null,
    val bought: Boolean? = null,
)

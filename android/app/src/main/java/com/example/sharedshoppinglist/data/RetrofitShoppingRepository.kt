package com.example.sharedshoppinglist.data

import com.example.sharedshoppinglist.data.dto.CreateItemRequest
import com.example.sharedshoppinglist.data.dto.CreateListRequest
import com.example.sharedshoppinglist.data.dto.UpdateItemRequest
import com.example.sharedshoppinglist.data.mapper.toDomain
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.ShoppingList
import java.io.IOException
import retrofit2.HttpException

/**
 * [ShoppingRepository] implementation backed by a Retrofit [ApiService].
 *
 * Non-2xx responses are surfaced through Retrofit/OkHttp (`HttpException`); the
 * `DELETE` call is inspected explicitly because it returns a bodyless response.
 */
class RetrofitShoppingRepository(
    baseUrl: String,
    private val api: ApiService = ApiClient.create(baseUrl),
) : ShoppingRepository {

    override suspend fun health(): String = api.health().status

    override suspend fun getLists(): List<ShoppingList> =
        api.getLists().map { it.toDomain() }

    override suspend fun createList(name: String): ShoppingList =
        api.createList(CreateListRequest(name)).toDomain()

    override suspend fun getList(listId: String): ShoppingList =
        api.getList(listId).toDomain()

    override suspend fun addItem(
        listId: String,
        name: String,
        quantity: String?,
    ): ShoppingItem = api.addItem(listId, CreateItemRequest(name, quantity)).toDomain()

    override suspend fun updateItem(
        itemId: String,
        name: String?,
        quantity: String?,
        bought: Boolean?,
    ): ShoppingItem = api.updateItem(
        itemId,
        UpdateItemRequest(name = name, quantity = quantity, bought = bought),
    ).toDomain()

    override suspend fun deleteItem(itemId: String) {
        val response = api.deleteItem(itemId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}

/** Narrow helper used by the ViewModels to build user-facing error messages. */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> "Server error (${code()})"
    is IOException -> "Network error: could not reach the backend"
    else -> message ?: "Unexpected error"
}

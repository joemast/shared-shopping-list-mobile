package com.example.sharedshoppinglist.data

import com.example.sharedshoppinglist.data.dto.CreateItemRequest
import com.example.sharedshoppinglist.data.dto.CreateListRequest
import com.example.sharedshoppinglist.data.dto.HealthDto
import com.example.sharedshoppinglist.data.dto.ShoppingItemDto
import com.example.sharedshoppinglist.data.dto.ShoppingListDto
import com.example.sharedshoppinglist.data.dto.UpdateItemRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Retrofit description of the backend API contract. */
interface ApiService {

    @GET("health")
    suspend fun health(): HealthDto

    @GET("lists")
    suspend fun getLists(): List<ShoppingListDto>

    @POST("lists")
    suspend fun createList(@Body body: CreateListRequest): ShoppingListDto

    @GET("lists/{id}")
    suspend fun getList(@Path("id") id: String): ShoppingListDto

    @POST("lists/{id}/items")
    suspend fun addItem(
        @Path("id") id: String,
        @Body body: CreateItemRequest,
    ): ShoppingItemDto

    @PATCH("items/{id}")
    suspend fun updateItem(
        @Path("id") id: String,
        @Body body: UpdateItemRequest,
    ): ShoppingItemDto

    @DELETE("items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Response<Unit>
}

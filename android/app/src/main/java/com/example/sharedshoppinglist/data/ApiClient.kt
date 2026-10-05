package com.example.sharedshoppinglist.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Builds the Retrofit [ApiService]. Kept as a small factory (no DI framework) so
 * both the production wiring in [com.example.sharedshoppinglist.AppContainer]
 * and the MockWebServer-backed instrumented test seam can create a client against
 * an arbitrary base URL.
 */
object ApiClient {

    @OptIn(ExperimentalSerializationApi::class)
    val json: Json = Json {
        ignoreUnknownKeys = true
        // Omit null fields so PATCH bodies stay partial (rename OR quantity OR toggle).
        explicitNulls = false
    }

    fun create(baseUrl: String): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}

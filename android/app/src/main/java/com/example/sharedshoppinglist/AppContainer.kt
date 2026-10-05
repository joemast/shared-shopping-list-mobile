package com.example.sharedshoppinglist

import com.example.sharedshoppinglist.data.RetrofitShoppingRepository
import com.example.sharedshoppinglist.data.ShoppingRepository

/**
 * Framework-free service locator (explicitly NOT Hilt/Koin/Dagger).
 *
 * It exposes the app's wiring — the base URL and the [ShoppingRepository]. The
 * instrumented UI suite (Stage 6) installs a MockWebServer-backed container via
 * [installTestOverride] so the suite never needs a live backend; nothing else in
 * the app knows about tests.
 */
open class AppContainer(
    val baseUrl: String = BuildConfig.API_BASE_URL,
    val repository: ShoppingRepository = RetrofitShoppingRepository(baseUrl),
) {
    companion object {
        @Volatile
        private var testOverride: AppContainer? = null

        /** Installs a test container; call before any Activity is created. */
        fun installTestOverride(container: AppContainer) {
            testOverride = container
        }

        fun clearTestOverride() {
            testOverride = null
        }

        /** Production default, unless a test override is installed. */
        fun create(): AppContainer = testOverride ?: AppContainer()
    }
}

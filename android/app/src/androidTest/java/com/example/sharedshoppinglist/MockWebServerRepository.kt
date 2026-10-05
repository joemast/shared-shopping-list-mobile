package com.example.sharedshoppinglist

import com.example.sharedshoppinglist.data.RetrofitShoppingRepository
import com.example.sharedshoppinglist.data.ShoppingRepository
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.ShoppingList
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher

/**
 * MockWebServer-backed [ShoppingRepository] used as the instrumented UI test seam
 * (chosen approach, OQ-15).
 *
 * Why this shape:
 *  - It implements the production [ShoppingRepository] interface, so the
 *    framework-free [AppContainer] seam can inject it with **zero** production
 *    changes (`AppContainer.installTestOverride(...)`).
 *  - Its methods delegate to the *real* [RetrofitShoppingRepository] pointed at
 *    this server's loopback URL. That means every UI call still traverses the
 *    production Retrofit + kotlinx.serialization path; only the HTTP peer is a
 *    deterministic stub. No live backend is involved.
 *  - Responses are canned and served FIFO: a test calls [enqueueJson] /
 *    [enqueueEmpty] to script the exact sequence of list/item responses the UI
 *    under test will consume. Because [QueueDispatcher] blocks until a response
 *    is enqueued, the Activity's launch-time `GET /lists` is naturally
 *    synchronized with the test's script (no `Thread.sleep()`).
 *
 * The production `http://10.0.2.2:<port>/` wiring is deliberately kept out of the
 * seam: the suite installs an [AppContainer] whose `baseUrl` is [baseUrl] here.
 */
class MockWebServerRepository : ShoppingRepository {

    private val server = MockWebServer().apply { start() }

    /** The real data path: Retrofit + kotlinx.serialization against the stub. */
    private val delegate: ShoppingRepository =
        RetrofitShoppingRepository(server.url("/").toString())

    /** Loopback base URL of the stub server; used by the `AppContainer` override. */
    val baseUrl: String = server.url("/").toString()

    /** Enqueues a single canned response (FIFO — the server replays them in order). */
    fun enqueue(response: MockResponse) {
        server.enqueue(response)
    }

    /** Enqueues a canned JSON response. */
    fun enqueueJson(code: Int = 200, body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    /** Enqueues a bodyless response (e.g. `DELETE /items/{id}` → `204`). */
    fun enqueueEmpty(code: Int) {
        server.enqueue(MockResponse().setResponseCode(code))
    }

    /**
     * Drops the queued script by installing a fresh [QueueDispatcher]. Called after
     * each test so a partially consumed scenario cannot leak into the next one.
     */
    fun resetQueue() {
        server.dispatcher = QueueDispatcher()
    }

    /** Stops the stub server. Called once for the whole suite. */
    fun shutdown() {
        server.shutdown()
    }

    override suspend fun health(): String = delegate.health()

    override suspend fun getLists(): List<ShoppingList> = delegate.getLists()

    override suspend fun createList(name: String): ShoppingList = delegate.createList(name)

    override suspend fun getList(listId: String): ShoppingList = delegate.getList(listId)

    override suspend fun addItem(
        listId: String,
        name: String,
        quantity: String?,
    ): ShoppingItem = delegate.addItem(listId, name, quantity)

    override suspend fun updateItem(
        itemId: String,
        name: String?,
        quantity: String?,
        bought: Boolean?,
    ): ShoppingItem = delegate.updateItem(itemId, name, quantity, bought)

    override suspend fun deleteItem(itemId: String) = delegate.deleteItem(itemId)
}

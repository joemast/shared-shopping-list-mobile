package com.example.sharedshoppinglist.fake

import com.example.sharedshoppinglist.data.ShoppingRepository
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.ShoppingList

/**
 * Hand-written in-memory [ShoppingRepository] for JVM unit tests.
 *
 * Deliberately not a mocking framework and not wired through DI: the ViewModels
 * take a [ShoppingRepository] directly, so a plain fake is enough. Lists/items
 * are stored in maps so mutations are observable across the ViewModel's reload
 * cycle (e.g. toggling bought, then re-reading via [getList]).
 *
 * Set [failWith] to force every call to throw, which exercises the error states.
 */
class FakeShoppingRepository(
    initialLists: List<ShoppingList> = emptyList(),
) : ShoppingRepository {

    private val lists: MutableMap<String, ShoppingList> =
        initialLists.associateBy { it.id }.toMutableMap()

    private var nextItemId: Int = 0

    /** When non-null, every public call throws this before doing any work. */
    var failWith: Throwable? = null

    /** Value returned by [health]. */
    var healthResult: String = "ok"

    /** Id assigned to the next list created via [createList]. */
    var nextListId: String = "generated-list"

    var createListCalls: Int = 0
        private set
    var addItemCalls: Int = 0
        private set
    var updateItemCalls: Int = 0
        private set
    var deleteItemCalls: Int = 0
        private set

    override suspend fun health(): String {
        throwIfFailing()
        return healthResult
    }

    override suspend fun getLists(): List<ShoppingList> {
        throwIfFailing()
        return lists.values.toList()
    }

    override suspend fun createList(name: String): ShoppingList {
        throwIfFailing()
        createListCalls++
        val list = ShoppingList(id = nextListId, name = name)
        lists[list.id] = list
        return list
    }

    override suspend fun getList(listId: String): ShoppingList {
        throwIfFailing()
        return lists[listId] ?: throw NoSuchElementException("List $listId not found")
    }

    override suspend fun addItem(listId: String, name: String, quantity: String?): ShoppingItem {
        throwIfFailing()
        addItemCalls++
        val list = lists[listId] ?: throw NoSuchElementException("List $listId not found")
        val item = ShoppingItem(
            id = "item-${nextItemId++}",
            listId = listId,
            name = name,
            quantity = quantity,
            bought = false,
        )
        lists[listId] = list.copy(items = list.items + item)
        return item
    }

    override suspend fun updateItem(
        itemId: String,
        name: String?,
        quantity: String?,
        bought: Boolean?,
    ): ShoppingItem {
        throwIfFailing()
        updateItemCalls++
        val list = lists.values.firstOrNull { candidate -> candidate.items.any { it.id == itemId } }
            ?: throw NoSuchElementException("Item $itemId not found")
        val current = list.items.first { it.id == itemId }
        // Null arguments mean "not provided" (partial update contract) — preserve.
        val updated = current.copy(
            name = name ?: current.name,
            quantity = quantity ?: current.quantity,
            bought = bought ?: current.bought,
        )
        lists[list.id] = list.copy(items = list.items.map { if (it.id == itemId) updated else it })
        return updated
    }

    override suspend fun deleteItem(itemId: String) {
        throwIfFailing()
        deleteItemCalls++
        val list = lists.values.firstOrNull { candidate -> candidate.items.any { it.id == itemId } }
            ?: return
        lists[list.id] = list.copy(items = list.items.filterNot { it.id == itemId })
    }

    private fun throwIfFailing() {
        failWith?.let { throw it }
    }
}

package com.example.sharedshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedshoppinglist.data.ShoppingRepository
import com.example.sharedshoppinglist.data.toUserMessage
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the List details screen: loads one list's items and supports add,
 * rename/quantity update, bought toggle, and delete. Each mutation reloads so
 * the UI always reflects server state.
 */
class ListDetailsViewModel(
    private val listId: String,
    private val repository: ShoppingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ShoppingItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ShoppingItem>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val items = repository.getList(listId).items
                if (items.isEmpty()) UiState.Empty else UiState.Content(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    fun addItem(name: String, quantity: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val normalizedQuantity = quantity?.trim()?.takeIf { it.isNotEmpty() }
        mutate { repository.addItem(listId, trimmed, normalizedQuantity) }
    }

    fun renameItem(itemId: String, name: String, quantity: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val normalizedQuantity = quantity?.trim()?.takeIf { it.isNotEmpty() }
        mutate { repository.updateItem(itemId = itemId, name = trimmed, quantity = normalizedQuantity) }
    }

    fun toggleBought(item: ShoppingItem) {
        mutate { repository.updateItem(itemId = item.id, bought = !item.bought) }
    }

    fun deleteItem(itemId: String) {
        mutate { repository.deleteItem(itemId) }
    }

    /** Runs a suspending mutation, then reloads; any failure surfaces as Error. */
    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                reload()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UiState.Error(e.toUserMessage())
            }
        }
    }

    private suspend fun reload() {
        val items = repository.getList(listId).items
        _state.value = if (items.isEmpty()) UiState.Empty else UiState.Content(items)
    }
}

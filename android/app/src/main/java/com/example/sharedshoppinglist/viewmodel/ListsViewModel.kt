package com.example.sharedshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedshoppinglist.data.ShoppingRepository
import com.example.sharedshoppinglist.data.toUserMessage
import com.example.sharedshoppinglist.domain.ShoppingList
import com.example.sharedshoppinglist.domain.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Drives the Lists screen: loads all lists and creates new ones. */
class ListsViewModel(
    private val repository: ShoppingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ShoppingList>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ShoppingList>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val lists = repository.getLists()
                if (lists.isEmpty()) UiState.Empty else UiState.Content(lists)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    /** Creates a list then reloads; blank names are rejected client-side. */
    fun createList(name: String, onCreated: (ShoppingList) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val created = repository.createList(trimmed)
                onCreated(created)
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UiState.Error(e.toUserMessage())
            }
        }
    }
}

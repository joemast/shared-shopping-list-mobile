package com.example.sharedshoppinglist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sharedshoppinglist.data.ShoppingRepository

/**
 * Minimal hand-written factories (no DI framework) that wire the repository into
 * the ViewModels.
 */

class ListsViewModelFactory(
    private val repository: ShoppingRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ListsViewModel(repository) as T
}

class ListDetailsViewModelFactory(
    private val listId: String,
    private val repository: ShoppingRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ListDetailsViewModel(listId, repository) as T
}

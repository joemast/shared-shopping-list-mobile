package com.example.sharedshoppinglist.domain

/**
 * UI state for a screen backed by an asynchronous source.
 *
 * [Empty] is distinct from [Content] with an empty payload so screens can render
 * a dedicated empty-state affordance.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}

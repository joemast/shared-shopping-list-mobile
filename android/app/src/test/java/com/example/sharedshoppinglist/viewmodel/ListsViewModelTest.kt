package com.example.sharedshoppinglist.viewmodel

import com.example.sharedshoppinglist.MainDispatcherRule
import com.example.sharedshoppinglist.domain.ShoppingList
import com.example.sharedshoppinglist.domain.UiState
import com.example.sharedshoppinglist.fake.FakeShoppingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * JVM unit tests for [ListsViewModel] — AND-01..AND-03 (HP-10, MP-1, HP-11).
 * No network, no emulator, no mocking framework: the [FakeShoppingRepository]
 * supplies deterministic data/failures.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleLists = listOf(
        ShoppingList(id = "l1", name = "Groceries"),
        ShoppingList(id = "l2", name = "Hardware"),
    )

    @Test
    fun `AND-01 loading then content when the repository returns lists`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = ListsViewModel(FakeShoppingRepository(sampleLists))

            // StandardTestDispatcher keeps the init coroutine queued, so Loading
            // is the observable initial state (HP-10).
            assertEquals(UiState.Loading, viewModel.state.value)

            advanceUntilIdle()

            assertEquals(UiState.Content(sampleLists), viewModel.state.value)
        }

    @Test
    fun `AND-02 empty repository maps to Empty not Content`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = ListsViewModel(FakeShoppingRepository(emptyList()))

            advanceUntilIdle()

            assertEquals(UiState.Empty, viewModel.state.value)
        }

    @Test
    fun `AND-03 repository failure maps to Error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeShoppingRepository(sampleLists).apply {
                failWith = IOException("connection refused")
            }
            val viewModel = ListsViewModel(repository)

            advanceUntilIdle()

            assertEquals(
                UiState.Error("Network error: could not reach the backend"),
                viewModel.state.value,
            )
        }
}

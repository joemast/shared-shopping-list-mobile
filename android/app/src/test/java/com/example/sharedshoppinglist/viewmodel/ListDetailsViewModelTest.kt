package com.example.sharedshoppinglist.viewmodel

import com.example.sharedshoppinglist.MainDispatcherRule
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.ShoppingList
import com.example.sharedshoppinglist.domain.UiState
import com.example.sharedshoppinglist.fake.FakeShoppingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * JVM unit tests for [ListDetailsViewModel] — AND-04..AND-06 (HP-10/HP-11,
 * MP-2). The fake stores mutations, so a toggle round-trips through a reload.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val milk = ShoppingItem(
        id = "i1",
        listId = "l1",
        name = "milk",
        quantity = "2 bottles",
        bought = false,
    )
    private val list = ShoppingList(id = "l1", name = "Groceries", items = listOf(milk))

    @Test
    fun `AND-04 loading then content with the list items`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = ListDetailsViewModel("l1", FakeShoppingRepository(listOf(list)))

            assertEquals(UiState.Loading, viewModel.state.value)

            advanceUntilIdle()

            assertEquals(UiState.Content(listOf(milk)), viewModel.state.value)
        }

    @Test
    fun `AND-05 repository failure maps to Error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeShoppingRepository(listOf(list)).apply {
                failWith = IOException("connection refused")
            }
            val viewModel = ListDetailsViewModel("l1", repository)

            advanceUntilIdle()

            assertEquals(
                UiState.Error("Network error: could not reach the backend"),
                viewModel.state.value,
            )
        }

    @Test
    fun `AND-06 MP-2 toggle bought replaces state with the updated item`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeShoppingRepository(listOf(list))
            val viewModel = ListDetailsViewModel("l1", repository)
            advanceUntilIdle()

            val before = viewModel.state.value as UiState.Content
            val itemBefore = before.data.single()
            assertFalse(itemBefore.bought)

            viewModel.toggleBought(itemBefore)
            advanceUntilIdle()

            val after = viewModel.state.value as UiState.Content
            assertTrue(after.data.single().bought)
            assertEquals(1, repository.updateItemCalls)
            // The prior snapshot is untouched: the state was replaced, not mutated
            // in place (AND-06).
            assertFalse(before.data.single().bought)
        }
}

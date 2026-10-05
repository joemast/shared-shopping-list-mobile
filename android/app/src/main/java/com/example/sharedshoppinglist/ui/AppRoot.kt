package com.example.sharedshoppinglist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sharedshoppinglist.AppContainer

/**
 * Root navigation for the MVP: a small in-memory toggle between the Lists screen
 * and the List details screen. Intentionally avoids a navigation library — the
 * scope guardrail rules out heavy architecture for a two-screen app.
 */
@Composable
fun AppRoot(container: AppContainer) {
    var selectedList by remember { mutableStateOf<ListSelection?>(null) }

    val current = selectedList
    if (current == null) {
        ListsScreen(
            container = container,
            onOpenList = { list -> selectedList = ListSelection(list.id, list.name) },
        )
    } else {
        ListDetailsScreen(
            listId = current.id,
            listName = current.name,
            container = container,
            onBack = { selectedList = null },
        )
    }
}

private data class ListSelection(val id: String, val name: String)

package com.example.sharedshoppinglist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedshoppinglist.AppContainer
import com.example.sharedshoppinglist.domain.ShoppingList
import com.example.sharedshoppinglist.domain.UiState
import com.example.sharedshoppinglist.ui.components.EmptyView
import com.example.sharedshoppinglist.ui.components.ErrorView
import com.example.sharedshoppinglist.ui.components.LoadingView
import com.example.sharedshoppinglist.viewmodel.ListsViewModel
import com.example.sharedshoppinglist.viewmodel.ListsViewModelFactory

/** Lists screen: shows all shopping lists and allows creating a new one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    container: AppContainer,
    onOpenList: (ShoppingList) -> Unit,
    viewModel: ListsViewModel = viewModel(factory = ListsViewModelFactory(container.repository)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping Lists") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh lists")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add list")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Empty -> EmptyView(
                    title = "No lists yet",
                    message = "Tap + to create your first shopping list.",
                )
                is UiState.Error -> ErrorView(
                    message = current.message,
                    onRetry = viewModel::load,
                )
                is UiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(current.data, key = { it.id }) { list ->
                        ListRow(list = list, onClick = { onOpenList(list) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NameInputDialog(
            title = "New list",
            nameLabel = "List name",
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                showAddDialog = false
                viewModel.createList(name)
            },
        )
    }
}

@Composable
private fun ListRow(list: ShoppingList, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(text = list.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = itemCountLabel(list.items.size),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun itemCountLabel(count: Int): String =
    if (count == 1) "1 item" else "$count items"

/** Reusable single-field name dialog (also used by the details screen). */
@Composable
fun NameInputDialog(
    title: String,
    nameLabel: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(nameLabel) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

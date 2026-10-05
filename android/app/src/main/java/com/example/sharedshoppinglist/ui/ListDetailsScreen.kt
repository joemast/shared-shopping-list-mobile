package com.example.sharedshoppinglist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sharedshoppinglist.AppContainer
import com.example.sharedshoppinglist.domain.ShoppingItem
import com.example.sharedshoppinglist.domain.UiState
import com.example.sharedshoppinglist.ui.components.EmptyView
import com.example.sharedshoppinglist.ui.components.ErrorView
import com.example.sharedshoppinglist.ui.components.LoadingView
import com.example.sharedshoppinglist.viewmodel.ListDetailsViewModel
import com.example.sharedshoppinglist.viewmodel.ListDetailsViewModelFactory

/** List details screen: shows a list's items and supports add/rename/toggle/delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailsScreen(
    listId: String,
    listName: String,
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: ListDetailsViewModel = viewModel(
        key = "details-$listId",
        factory = ListDetailsViewModelFactory(listId, container.repository),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh items")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Empty -> EmptyView(
                    title = "No items yet",
                    message = "Tap + to add an item to $listName.",
                )
                is UiState.Error -> ErrorView(
                    message = current.message,
                    onRetry = viewModel::load,
                )
                is UiState.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(current.data, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            onToggle = { viewModel.toggleBought(item) },
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.deleteItem(item.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ItemInputDialog(
            title = "Add item",
            initialName = "",
            initialQuantity = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, quantity ->
                showAddDialog = false
                viewModel.addItem(name, quantity)
            },
        )
    }

    editingItem?.let { item ->
        ItemInputDialog(
            title = "Edit item",
            initialName = item.name,
            initialQuantity = item.quantity.orEmpty(),
            onDismiss = { editingItem = null },
            onConfirm = { name, quantity ->
                editingItem = null
                viewModel.renameItem(item.id, name, quantity)
            },
        )
    }
}

@Composable
private fun ItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.bought,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag("itemBoughtToggle"),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.bought) TextDecoration.LineThrough else null,
            )
            item.quantity?.let { quantity ->
                Text(
                    text = quantity,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("itemQuantityText"),
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit ${item.name}")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete ${item.name}")
        }
    }
}

/** Two-field dialog for item name + optional quantity. */
@Composable
private fun ItemInputDialog(
    title: String,
    initialName: String,
    initialQuantity: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, quantity: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var quantity by remember { mutableStateOf(initialQuantity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.testTag("itemNameField"),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (optional)") },
                    singleLine = true,
                    modifier = Modifier.testTag("itemQuantityField"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, quantity.ifBlank { null }) },
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

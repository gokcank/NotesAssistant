package com.gokcank.notesassistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gokcank.notesassistant.R

/** Çöp kutusu: silinen notlar 30 gün burada bekler, geri alınabilir veya kalıcı silinebilir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
) {
    val trash by viewModel.trash.collectAsState()
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var deleteForeverId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (trash.isNotEmpty()) {
                        TextButton(onClick = { showEmptyConfirm = true }) {
                            Text(stringResource(R.string.empty_trash))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (trash.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_trash_list), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Text(
                    stringResource(R.string.trash_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(trash, key = { it.note.id }) { noteWithItems ->
                        val note = noteWithItems.note
                        ListItem(
                            headlineContent = {
                                Text(
                                    note.title.ifBlank { stringResource(R.string.untitled) },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Column {
                                    val preview = when {
                                        note.isLocked -> stringResource(R.string.locked_note)
                                        note.isChecklist ->
                                            noteWithItems.items.take(2).joinToString(", ") { it.text }
                                        else -> note.body.take(80)
                                    }
                                    if (preview.isNotBlank()) {
                                        Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    note.deletedAt?.let {
                                        Text(
                                            stringResource(R.string.deleted_on, formatDateTime(it)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { viewModel.restoreFromTrash(note.id) }) {
                                        Icon(
                                            Icons.Filled.RestoreFromTrash,
                                            contentDescription = stringResource(R.string.restore_note),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    IconButton(onClick = { deleteForeverId = note.id }) {
                                        Icon(
                                            Icons.Filled.DeleteForever,
                                            contentDescription = stringResource(R.string.delete_forever),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text(stringResource(R.string.empty_trash_title)) },
            text = { Text(stringResource(R.string.empty_trash_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyConfirm = false
                    viewModel.emptyTrash()
                }) { Text(stringResource(R.string.empty_trash)) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    deleteForeverId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteForeverId = null },
            title = { Text(stringResource(R.string.delete_forever_title)) },
            text = { Text(stringResource(R.string.delete_forever_message)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteForeverId = null
                    viewModel.deleteForever(id)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteForeverId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

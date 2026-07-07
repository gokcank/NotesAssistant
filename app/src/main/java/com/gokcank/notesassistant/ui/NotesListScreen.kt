package com.gokcank.notesassistant.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gokcank.notesassistant.R
import com.gokcank.notesassistant.data.NoteWithItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onOpenNote: (Long) -> Unit,
    onNewNote: (Boolean) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val notes by viewModel.notes.collectAsState()
    val gridView by viewModel.gridView.collectAsState()
    val message by viewModel.message.collectAsState()
    val undoMessage by viewModel.undoMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(undoMessage) {
        undoMessage?.let { text ->
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            viewModel.consumeUndoMessage()
        }
    }

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val filteredNotes = remember(notes, query) {
        val q = query.trim()
        if (q.isEmpty()) notes
        else notes.filter { noteWithItems ->
            noteWithItems.note.title.contains(q, ignoreCase = true) ||
                noteWithItems.note.body.contains(q, ignoreCase = true) ||
                noteWithItems.items.any { it.text.contains(q, ignoreCase = true) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (searching) {
                SearchTopBar(
                    query = query,
                    onQueryChange = { query = it },
                    onClose = { searching = false; query = "" },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.notes_title)) },
                    actions = {
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                        IconButton(onClick = { viewModel.toggleGridView() }) {
                            if (gridView) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ViewList,
                                    contentDescription = stringResource(R.string.view_list),
                                )
                            } else {
                                Icon(
                                    Icons.Filled.GridView,
                                    contentDescription = stringResource(R.string.view_grid),
                                )
                            }
                        }
                        IconButton(onClick = onOpenReminders) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = stringResource(R.string.reminders_title),
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { onNewNote(true) },
                    icon = { Icon(Icons.Filled.Checklist, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_checklist)) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp),
                )
                ExtendedFloatingActionButton(
                    onClick = { onNewNote(false) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_note)) },
                )
            }
        },
        bottomBar = { AdBanner() },
    ) { padding ->
        when {
            filteredNotes.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (query.isBlank()) R.string.empty_notes else R.string.no_results),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            gridView -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredNotes, key = { it.note.id }) { noteWithItems ->
                        NoteCard(
                            noteWithItems = noteWithItems,
                            onClick = { onOpenNote(noteWithItems.note.id) },
                            onTogglePin = { viewModel.togglePin(noteWithItems.note) },
                            onDelete = { viewModel.deleteNoteWithUndo(noteWithItems.note.id) },
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredNotes, key = { it.note.id }) { noteWithItems ->
                        NoteCard(
                            noteWithItems = noteWithItems,
                            onClick = { onOpenNote(noteWithItems.note.id) },
                            onTogglePin = { viewModel.togglePin(noteWithItems.note) },
                            onDelete = { viewModel.deleteNoteWithUndo(noteWithItems.note.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    noteWithItems: NoteWithItems,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val note = noteWithItems.note
    val customColor = noteContainerColor(note.color)
    var menuOpen by remember { mutableStateOf(false) }
    ElevatedCard(
        colors = if (customColor != null) {
            CardDefaults.elevatedCardColors(containerColor = customColor)
        } else {
            CardDefaults.elevatedCardColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.elevatedShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true },
            ),
    ) {
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (note.isPinned) R.string.unpin_note else R.string.pin_note))
                },
                leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onTogglePin()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title.ifBlank { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (note.isPinned) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.pin_note),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            val preview = if (note.isChecklist) {
                noteWithItems.items.sortedBy { it.position }.take(3)
                    .joinToString("\n") { (if (it.isDone) "☑ " else "☐ ") + it.text }
            } else {
                note.body.take(150)
            }
            if (preview.isNotBlank()) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                formatDateTime(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

package com.gokcank.notesassistant.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gokcank.notesassistant.R
import com.gokcank.notesassistant.calendar.CalendarHelper
import com.gokcank.notesassistant.data.ChecklistItem
import com.gokcank.notesassistant.data.Note
import com.gokcank.notesassistant.smart.DateTimeExtractor
import java.time.LocalDateTime

data class EditItem(val text: String = "", val isDone: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: NotesViewModel,
    noteId: Long,
    checklistDefault: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var baseNote by remember { mutableStateOf(Note(isChecklist = checklistDefault)) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<EditItem>() }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            viewModel.getNote(noteId)?.let { noteWithItems ->
                baseNote = noteWithItems.note
                title = noteWithItems.note.title
                body = noteWithItems.note.body
                items.clear()
                items.addAll(
                    noteWithItems.items.sortedBy { it.position }.map { EditItem(it.text, it.isDone) }
                )
            }
        }
    }

    val combinedText = buildString {
        appendLine(title)
        appendLine(body)
        items.forEach { appendLine(it.text) }
    }
    val detection = remember(combinedText) { DateTimeExtractor.extract(combinedText) }

    fun hasContent() = title.isNotBlank() || body.isNotBlank() || items.any { it.text.isNotBlank() }
    fun buildNote() = baseNote.copy(title = title.trim(), body = body)
    fun buildItems() =
        if (baseNote.isChecklist) {
            items.filter { it.text.isNotBlank() }.mapIndexed { index, item ->
                ChecklistItem(noteId = baseNote.id, text = item.text.trim(), isDone = item.isDone, position = index)
            }
        } else {
            emptyList()
        }

    fun reminderMessage() =
        if (baseNote.isChecklist) {
            items.filter { it.text.isNotBlank() && !it.isDone }.joinToString("\n") { "• ${it.text}" }
        } else {
            body.take(200)
        }

    fun persist(onDone: (Long) -> Unit = {}) {
        viewModel.saveNote(buildNote(), buildItems()) { id ->
            baseNote = baseNote.copy(id = id)
            onDone(id)
        }
    }

    fun saveAndExit() {
        if (hasContent()) persist { onBack() } else onBack()
    }

    BackHandler { saveAndExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (baseNote.isChecklist) R.string.new_checklist else R.string.new_note
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { baseNote = baseNote.copy(isPinned = !baseNote.isPinned) }) {
                        Icon(
                            if (baseNote.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(
                                if (baseNote.isPinned) R.string.unpin_note else R.string.pin_note
                            ),
                        )
                    }
                    IconButton(onClick = { showReminderDialog = true }, enabled = hasContent()) {
                        Icon(Icons.Filled.Alarm, contentDescription = stringResource(R.string.set_reminder))
                    }
                    IconButton(
                        onClick = {
                            val start = detection?.dateTime?.toEpochMillis()
                                ?: (System.currentTimeMillis() + 3_600_000)
                            runCatching {
                                context.startActivity(
                                    CalendarHelper.insertEventIntent(
                                        title.ifBlank { context.getString(R.string.app_name) },
                                        reminderMessage(),
                                        start,
                                    )
                                )
                            }
                        },
                        enabled = hasContent(),
                    ) {
                        Icon(Icons.Filled.Event, contentDescription = stringResource(R.string.add_to_calendar))
                    }
                    if (baseNote.id > 0) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { saveAndExit() },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text(stringResource(R.string.save)) },
            )
        },
        containerColor = noteContainerColor(baseNote.color) ?: MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.note_title_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )
            ColorPickerRow(
                selected = baseNote.color,
                onSelect = { baseNote = baseNote.copy(color = it) },
            )
            detection?.let { detected ->
                AssistChip(
                    onClick = { showReminderDialog = true },
                    label = {
                        Text(stringResource(R.string.reminder_suggestion, formatDateTime(detected.dateTime)))
                    },
                    leadingIcon = { Icon(Icons.Filled.Alarm, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            if (baseNote.isChecklist) {
                ChecklistEditor(items)
            } else {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text(stringResource(R.string.note_body_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteNoteWithUndo(baseNote.id)
                    onBack()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            initial = detection?.dateTime ?: LocalDateTime.now().plusHours(1),
            onDismiss = { showReminderDialog = false },
            onConfirm = { dateTime ->
                showReminderDialog = false
                persist { id ->
                    viewModel.addReminder(
                        noteId = id,
                        title = title.ifBlank { context.getString(R.string.app_name) },
                        message = reminderMessage(),
                        triggerAt = dateTime.toEpochMillis(),
                    )
                }
            },
        )
    }
}

@Composable
private fun ColorPickerRow(selected: Int, onSelect: (Int) -> Unit) {
    val colorDescription = stringResource(R.string.note_color)
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(NOTE_COLOR_COUNT) { index ->
            val color = noteContainerColor(index) ?: MaterialTheme.colorScheme.surfaceVariant
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(index) }
                    .semantics { contentDescription = colorDescription },
            )
        }
    }
}

@Composable
private fun ChecklistEditor(items: SnapshotStateList<EditItem>) {
    var newItemText by remember { mutableStateOf("") }
    fun addItem() {
        if (newItemText.isNotBlank()) {
            items.add(EditItem(newItemText.trim()))
            newItemText = ""
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
    ) {
        itemsIndexed(items) { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = { checked -> items[index] = item.copy(isDone = checked) },
                )
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { items[index] = item.copy(text = it) },
                    textStyle = LocalTextStyle.current.copy(
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { items.removeAt(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text(stringResource(R.string.checklist_item_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addItem() }),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = { addItem() },
                    enabled = newItemText.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.checklist_item_hint))
                }
            }
        }
    }
}

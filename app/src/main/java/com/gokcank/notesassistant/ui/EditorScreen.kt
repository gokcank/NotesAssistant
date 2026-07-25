package com.gokcank.notesassistant.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import com.gokcank.notesassistant.R
import com.gokcank.notesassistant.calendar.CalendarHelper
import com.gokcank.notesassistant.data.ChecklistItem
import com.gokcank.notesassistant.data.Note
import com.gokcank.notesassistant.smart.DateTimeExtractor

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
    var body by remember { mutableStateOf(TextFieldValue("")) }
    val items = remember { mutableStateListOf<EditItem>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEmptyDeleteConfirm by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }

    val allNotes by viewModel.notes.collectAsState()
    val existingLabels = remember(allNotes) {
        allNotes.mapNotNull { it.note.label }.distinct().sorted()
    }

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            viewModel.getNote(noteId)?.let { noteWithItems ->
                baseNote = noteWithItems.note
                title = noteWithItems.note.title
                body = TextFieldValue(noteWithItems.note.body)
                items.clear()
                items.addAll(
                    // Tamamlanmamışlar önce, ardından tamamlananlar; kendi içlerinde kayıtlı sırayla
                    noteWithItems.items
                        .sortedWith(compareBy<ChecklistItem> { it.isDone }.thenBy { it.position })
                        .map { EditItem(it.text, it.isDone) }
                )
            }
        }
    }

    val combinedText = buildString {
        appendLine(title)
        appendLine(body.text)
        items.forEach { appendLine(it.text) }
    }
    val detection = remember(combinedText) { DateTimeExtractor.extract(combinedText) }

    fun hasContent() = title.isNotBlank() || body.text.isNotBlank() || items.any { it.text.isNotBlank() }
    fun buildNote() = baseNote.copy(title = title.trim(), body = body.text)
    fun buildItems() =
        if (baseNote.isChecklist) {
            items.filter { it.text.isNotBlank() }.mapIndexed { index, item ->
                ChecklistItem(noteId = baseNote.id, text = item.text.trim(), isDone = item.isDone, position = index)
            }
        } else {
            emptyList()
        }

    fun eventDescription() =
        if (baseNote.isChecklist) {
            items.filter { it.text.isNotBlank() && !it.isDone }.joinToString("\n") { "• ${it.text}" }
        } else {
            body.text.take(200)
        }

    /** Seçili metni verilen işaretle sarar (kalın/italik). */
    fun wrapSelection(marker: String) {
        val start = body.selection.min
        val end = body.selection.max
        val newText = buildString {
            append(body.text, 0, start)
            append(marker)
            append(body.text, start, end)
            append(marker)
            append(body.text, end, body.text.length)
        }
        body = body.copy(
            text = newText,
            selection = TextRange(start + marker.length, end + marker.length),
        )
    }

    /** İmlecin bulunduğu satırı başlık yapar / başlığı kaldırır. */
    fun toggleHeading() {
        val text = body.text
        val lineStart = text.lastIndexOf('\n', body.selection.min - 1) + 1
        if (text.startsWith("# ", lineStart)) {
            body = body.copy(
                text = text.removeRange(lineStart, lineStart + 2),
                selection = TextRange((body.selection.min - 2).coerceAtLeast(lineStart)),
            )
        } else {
            body = body.copy(
                text = text.substring(0, lineStart) + "# " + text.substring(lineStart),
                selection = TextRange(body.selection.min + 2),
            )
        }
    }

    fun shareNote() {
        val bodyText = if (baseNote.isChecklist) {
            items.filter { it.text.isNotBlank() }
                .joinToString("\n") { (if (it.isDone) "☑ " else "☐ ") + it.text }
        } else {
            body.text
        }
        val text = listOf(title, bodyText).filter { it.isNotBlank() }.joinToString("\n\n")
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            if (title.isNotBlank()) putExtra(android.content.Intent.EXTRA_SUBJECT, title)
        }
        runCatching {
            context.startActivity(android.content.Intent.createChooser(intent, null))
        }
    }

    fun openCalendar(startAt: Long) {
        runCatching {
            context.startActivity(
                CalendarHelper.insertEventIntent(
                    title.ifBlank { context.getString(R.string.app_name) },
                    eventDescription(),
                    startAt,
                )
            )
        }
    }

    fun persist(onDone: (Long) -> Unit = {}) {
        viewModel.saveNote(buildNote(), buildItems()) { id ->
            baseNote = baseNote.copy(id = id)
            onDone(id)
        }
    }

    fun saveAndExit() {
        when {
            hasContent() -> persist { onBack() }
            // Mevcut notun tüm içeriği silinmişse sessizce eski haline dönmek yerine silmeyi öner
            baseNote.id > 0 -> showEmptyDeleteConfirm = true
            else -> onBack()
        }
    }

    BackHandler { saveAndExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when {
                                noteId > 0 && baseNote.isChecklist -> R.string.edit_checklist
                                noteId > 0 -> R.string.edit_note
                                baseNote.isChecklist -> R.string.new_checklist
                                else -> R.string.new_note
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (baseNote.isLocked) {
                            (context as? FragmentActivity)?.let { activity ->
                                BiometricLock.authenticate(
                                    activity,
                                    title = context.getString(R.string.auth_title),
                                    subtitle = context.getString(R.string.auth_subtitle),
                                ) { baseNote = baseNote.copy(isLocked = false) }
                            }
                        } else if (BiometricLock.isAvailable(context)) {
                            baseNote = baseNote.copy(isLocked = true)
                        } else {
                            Toast.makeText(context, R.string.no_lockscreen_warning, Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(
                            if (baseNote.isLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = stringResource(
                                if (baseNote.isLocked) R.string.unlock_note else R.string.lock_note
                            ),
                        )
                    }
                    IconButton(onClick = { baseNote = baseNote.copy(isPinned = !baseNote.isPinned) }) {
                        Icon(
                            if (baseNote.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(
                                if (baseNote.isPinned) R.string.unpin_note else R.string.pin_note
                            ),
                        )
                    }
                    IconButton(onClick = { shareNote() }, enabled = hasContent()) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                    }
                    IconButton(
                        onClick = {
                            openCalendar(
                                detection?.dateTime?.toEpochMillis()
                                    ?: (System.currentTimeMillis() + 3_600_000)
                            )
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
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        stringResource(R.string.note_title_hint),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = editorFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
            ColorPickerRow(
                selected = baseNote.color,
                onSelect = { baseNote = baseNote.copy(color = it) },
            )
            AssistChip(
                onClick = { showLabelDialog = true },
                label = { Text(baseNote.label ?: stringResource(R.string.add_label)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            detection?.let { detected ->
                AssistChip(
                    onClick = { openCalendar(detected.dateTime.toEpochMillis()) },
                    label = {
                        Text(stringResource(R.string.detected_date, formatDateTime(detected.dateTime)))
                    },
                    leadingIcon = { Icon(Icons.Filled.Event, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            if (baseNote.isChecklist) {
                ChecklistEditor(items)
            } else {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    IconButton(onClick = { wrapSelection("**") }) {
                        Icon(Icons.Filled.FormatBold, contentDescription = stringResource(R.string.format_bold))
                    }
                    IconButton(onClick = { wrapSelection("*") }) {
                        Icon(Icons.Filled.FormatItalic, contentDescription = stringResource(R.string.format_italic))
                    }
                    IconButton(onClick = { toggleHeading() }) {
                        Icon(Icons.Filled.Title, contentDescription = stringResource(R.string.format_heading))
                    }
                }
                val markerColor = MaterialTheme.colorScheme.outline
                val markdownTransformation = remember(markerColor) { MarkdownLiteTransformation(markerColor) }
                TextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text(stringResource(R.string.note_body_hint)) },
                    colors = editorFieldColors(),
                    visualTransformation = markdownTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 4.dp),
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

    if (showLabelDialog) {
        LabelDialog(
            current = baseNote.label,
            existingLabels = existingLabels,
            onDismiss = { showLabelDialog = false },
            onSelect = { selected ->
                baseNote = baseNote.copy(label = selected)
                showLabelDialog = false
            },
        )
    }

    if (showEmptyDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_empty_note_title)) },
            text = { Text(stringResource(R.string.delete_empty_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyDeleteConfirm = false
                    viewModel.deleteNoteWithUndo(baseNote.id)
                    onBack()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

}

/** Etiket seçme/yazma penceresi: mevcut etiketlerden seçilir veya yenisi yazılır. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelDialog(
    current: String?,
    existingLabels: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    var text by remember { mutableStateOf(current.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_title)) },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existingLabels.isNotEmpty()) {
                    Text(
                        stringResource(R.string.existing_labels),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        existingLabels.forEach { label ->
                            AssistChip(onClick = { text = label }, label = { Text(label) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(text.trim().ifBlank { null }) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text(stringResource(R.string.remove_label))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Google Keep tarzı çerçevesiz yazı alanı: kutu ve alt çizgi görünmez, yalnızca metin. */
@Composable
private fun editorFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
)

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
    var showClearDoneConfirm by remember { mutableStateOf(false) }

    // Sürükleme durumu: yalnızca tamamlanmamış maddeler sıralanabilir
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<Int, Int>() }

    // Değişmez kural: listede önce tamamlanmamışlar, sonra tamamlananlar durur
    fun firstDoneIndex() = items.indexOfFirst { it.isDone }.takeIf { it >= 0 } ?: items.size

    fun addItem() {
        if (newItemText.isNotBlank()) {
            items.add(firstDoneIndex(), EditItem(newItemText.trim()))
            newItemText = ""
        }
    }

    fun toggleItem(index: Int, checked: Boolean) {
        val item = items.removeAt(index).copy(isDone = checked)
        if (checked) items.add(item) else items.add(firstDoneIndex(), item)
    }

    val activeCount = items.count { !it.isDone }
    val doneCount = items.size - activeCount

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        items.forEachIndexed { index, item ->
            if (item.isDone) return@forEachIndexed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowHeights[index] = it.height }
                    .zIndex(if (index == draggedIndex) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (index == draggedIndex) dragOffset else 0f
                    },
            ) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = stringResource(R.string.reorder),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.pointerInput(activeCount) {
                        detectDragGestures(
                            onDragStart = { draggedIndex = index; dragOffset = 0f },
                            onDragCancel = { draggedIndex = -1; dragOffset = 0f },
                            onDragEnd = { draggedIndex = -1; dragOffset = 0f },
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                            val step = rowHeights[draggedIndex] ?: return@detectDragGestures
                            while (dragOffset > step * 0.6f && draggedIndex < activeCount - 1) {
                                items.add(draggedIndex + 1, items.removeAt(draggedIndex))
                                draggedIndex++
                                dragOffset -= step
                            }
                            while (dragOffset < -step * 0.6f && draggedIndex > 0) {
                                items.add(draggedIndex - 1, items.removeAt(draggedIndex))
                                draggedIndex--
                                dragOffset += step
                            }
                        }
                    },
                )
                Checkbox(
                    checked = false,
                    onCheckedChange = { toggleItem(index, true) },
                )
                TextField(
                    value = item.text,
                    onValueChange = { items[index] = item.copy(text = it) },
                    colors = editorFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { items.removeAt(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                placeholder = { Text(stringResource(R.string.checklist_item_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addItem() }),
                colors = editorFieldColors(),
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

        if (doneCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    stringResource(R.string.completed_section, doneCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showClearDoneConfirm = true }) {
                    Text(stringResource(R.string.clear_completed))
                }
            }
            items.forEachIndexed { index, item ->
                if (!item.isDone) return@forEachIndexed
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { toggleItem(index, false) },
                    )
                    TextField(
                        value = item.text,
                        onValueChange = { items[index] = item.copy(text = it) },
                        textStyle = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough),
                        colors = editorFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { items.removeAt(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete))
                    }
                }
            }
        }
    }

    if (showClearDoneConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDoneConfirm = false },
            title = { Text(stringResource(R.string.clear_completed_title)) },
            text = { Text(stringResource(R.string.clear_completed_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDoneConfirm = false
                    items.removeAll { it.isDone }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDoneConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

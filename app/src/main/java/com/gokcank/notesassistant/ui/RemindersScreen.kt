package com.gokcank.notesassistant.ui

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gokcank.notesassistant.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = context.getSystemService(NotificationManager::class.java)
            ?.areNotificationsEnabled() != false
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!notificationsEnabled) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.reminders_permission_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            if (reminders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_reminders), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(reminders, key = { it.id }) { reminder ->
                        val isPast = reminder.triggerAt < System.currentTimeMillis()
                        val pastSuffix = if (isPast) " " + stringResource(R.string.past_suffix) else ""
                        ListItem(
                            headlineContent = { Text(reminder.title) },
                            supportingContent = {
                                Column {
                                    Text(formatDateTime(reminder.triggerAt) + pastSuffix)
                                    if (reminder.message.isNotBlank()) {
                                        Text(
                                            reminder.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            },
                            leadingContent = { Icon(Icons.Filled.Alarm, contentDescription = null) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            },
                            modifier = Modifier.clickable(enabled = reminder.noteId != null) {
                                reminder.noteId?.let(onOpenNote)
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

package com.gokcank.notesassistant.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.gokcank.notesassistant.R
import com.gokcank.notesassistant.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importBackup) }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importDocument) }

    val themeMode by viewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val currentLanguageLabel = when {
        currentLanguageTag.startsWith("tr") -> stringResource(R.string.language_turkish)
        currentLanguageTag.startsWith("en") -> stringResource(R.string.language_english)
        else -> stringResource(R.string.language_system)
    }
    val themeLabel = stringResource(
        when (themeMode) {
            ThemeMode.SYSTEM -> R.string.theme_system
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.DARK -> R.string.theme_dark
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                SettingsSection(stringResource(R.string.settings_appearance_section)) {
                    SettingsItem(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_theme_title),
                        description = themeLabel,
                        onClick = { showThemeDialog = true },
                    )
                    SettingsItem(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.settings_language_title),
                        description = currentLanguageLabel,
                        onClick = { showLanguageDialog = true },
                    )
                }
            }
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_backup_section),
                    banner = { InfoBanner(stringResource(R.string.settings_auto_backup_info)) },
                ) {
                    SettingsItem(
                        icon = Icons.Filled.FileUpload,
                        title = stringResource(R.string.settings_export_title),
                        description = stringResource(R.string.settings_export_desc),
                        onClick = { exportLauncher.launch("NotesAssistant-backup.json") },
                    )
                    SettingsItem(
                        icon = Icons.Filled.FileDownload,
                        title = stringResource(R.string.settings_import_title),
                        description = stringResource(R.string.settings_import_desc),
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream")
                            )
                        },
                    )
                }
            }
            item {
                SettingsSection(stringResource(R.string.settings_notes_section)) {
                    SettingsItem(
                        icon = Icons.Filled.Description,
                        title = stringResource(R.string.settings_import_doc_title),
                        description = stringResource(R.string.settings_import_doc_desc),
                        onClick = { documentLauncher.launch(arrayOf("text/*")) },
                    )
                }
            }
            item {
                SettingsSection(stringResource(R.string.settings_notifications_section)) {
                    SettingsItem(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.settings_notification_title),
                        description = stringResource(R.string.settings_notification_desc),
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                        },
                    )
                    if (Build.VERSION.SDK_INT >= 31) {
                        SettingsItem(
                            icon = Icons.Filled.Alarm,
                            title = stringResource(R.string.settings_exact_alarm_title),
                            description = stringResource(R.string.settings_exact_alarm_desc),
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                            .setData("package:${context.packageName}".toUri())
                                    )
                                }
                            },
                        )
                    }
                }
            }
            item {
                SettingsSection(stringResource(R.string.settings_about_section)) {
                    SettingsItem(
                        icon = Icons.Filled.Info,
                        title = "NotesAssistant",
                        description = stringResource(R.string.settings_version_desc),
                    )
                    SettingsItem(
                        icon = Icons.Filled.Person,
                        title = stringResource(R.string.settings_developer_title),
                        description = stringResource(R.string.settings_developer_name),
                    )
                }
            }
            item {
                FilledTonalButton(
                    onClick = {
                        runCatching {
                            val deviceInfo = "\n\n—\nNotesAssistant v1.0.0 • Android ${Build.VERSION.RELEASE} • " +
                                "${Build.MANUFACTURER} ${Build.MODEL}"
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = "mailto:".toUri()
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.support_email)))
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.bug_report_subject))
                                    putExtra(Intent.EXTRA_TEXT, deviceInfo)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.BugReport, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_bug_report))
                }
            }
        }
    }

    if (showThemeDialog) {
        RadioDialog(
            title = stringResource(R.string.settings_theme_title),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark),
            ),
            selected = themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        val selectedTag = when {
            currentLanguageTag.startsWith("tr") -> "tr"
            currentLanguageTag.startsWith("en") -> "en"
            else -> ""
        }
        RadioDialog(
            title = stringResource(R.string.settings_language_title),
            options = listOf(
                "" to stringResource(R.string.language_system),
                "en" to stringResource(R.string.language_english),
                "tr" to stringResource(R.string.language_turkish),
            ),
            selected = selectedTag,
            onSelect = { tag ->
                showLanguageDialog = false
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(tag)
                )
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    banner: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        if (banner != null) {
            banner()
            Spacer(Modifier.padding(4.dp))
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

/** Tıklanabilir seçenek gibi değil, bilgi notu gibi görünen satır. */
@Composable
private fun InfoBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(description, style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
private fun <T> RadioDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) },
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

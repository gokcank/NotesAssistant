package com.gokcank.notesassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gokcank.notesassistant.data.ThemeMode
import com.gokcank.notesassistant.ui.EditorScreen
import com.gokcank.notesassistant.ui.NotesListScreen
import com.gokcank.notesassistant.ui.NotesViewModel
import com.gokcank.notesassistant.ui.SettingsScreen
import com.gokcank.notesassistant.ui.TrashScreen
import com.gokcank.notesassistant.ui.theme.NotesAssistantTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText =
            if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) else null
        val sharedTitle = intent?.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        // Simge kısayolundan gelindiyse doğrudan boş editör açılır
        val shortcutChecklist = when (intent?.action) {
            ACTION_NEW_NOTE -> false
            ACTION_NEW_CHECKLIST -> true
            else -> null
        }

        setContent {
            val viewModel: NotesViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            NotesAssistantTheme(darkTheme = darkTheme) {
                AppNav(viewModel, sharedTitle, sharedText, shortcutChecklist)
            }
        }
    }

    companion object {
        const val ACTION_NEW_NOTE = "com.gokcank.notesassistant.NEW_NOTE"
        const val ACTION_NEW_CHECKLIST = "com.gokcank.notesassistant.NEW_CHECKLIST"
    }
}

@Composable
fun AppNav(
    viewModel: NotesViewModel,
    sharedTitle: String,
    sharedText: String?,
    shortcutChecklist: Boolean? = null,
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        if (shortcutChecklist != null) {
            navController.navigate("editor/-1?checklist=$shortcutChecklist")
        } else if (!sharedText.isNullOrBlank()) {
            viewModel.createNoteFromText(sharedTitle, sharedText) { id ->
                navController.navigate("editor/$id?checklist=false")
            }
        }
    }

    NavHost(navController, startDestination = "notes") {
        composable("notes") {
            NotesListScreen(
                viewModel = viewModel,
                onOpenNote = { navController.navigate("editor/$it?checklist=false") },
                onNewNote = { checklist -> navController.navigate("editor/-1?checklist=$checklist") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenTrash = { navController.navigate("trash") },
            )
        }
        composable("trash") {
            TrashScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "editor/{noteId}?checklist={checklist}",
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType },
                navArgument("checklist") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            EditorScreen(
                viewModel = viewModel,
                noteId = entry.arguments?.getLong("noteId") ?: -1L,
                checklistDefault = entry.arguments?.getBoolean("checklist") ?: false,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

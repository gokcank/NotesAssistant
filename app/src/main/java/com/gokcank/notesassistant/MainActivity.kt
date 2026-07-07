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
import com.gokcank.notesassistant.ui.OnboardingDialog
import com.gokcank.notesassistant.ui.RemindersScreen
import com.gokcank.notesassistant.ui.SettingsScreen
import com.gokcank.notesassistant.ui.theme.NotesAssistantTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText =
            if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) else null
        val sharedTitle = intent?.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val openNoteId = intent?.getLongExtra(EXTRA_OPEN_NOTE_ID, -1L) ?: -1L

        setContent {
            val viewModel: NotesViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            NotesAssistantTheme(darkTheme = darkTheme) {
                AppNav(viewModel, sharedTitle, sharedText, openNoteId)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
    }
}

@Composable
fun AppNav(
    viewModel: NotesViewModel,
    sharedTitle: String,
    sharedText: String?,
    openNoteId: Long,
) {
    val navController = rememberNavController()

    val onboardingDone by viewModel.onboardingDone.collectAsState()
    if (!onboardingDone) {
        OnboardingDialog(onFinished = { viewModel.completeOnboarding() })
    }

    LaunchedEffect(Unit) {
        if (openNoteId > 0) {
            navController.navigate("editor/$openNoteId?checklist=false")
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
                onOpenReminders = { navController.navigate("reminders") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(
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
        composable("reminders") {
            RemindersScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenNote = { navController.navigate("editor/$it?checklist=false") },
            )
        }
    }
}

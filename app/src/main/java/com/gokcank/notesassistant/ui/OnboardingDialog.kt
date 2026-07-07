package com.gokcank.notesassistant.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.gokcank.notesassistant.R

/** Android 12+ cihazlarda tam zamanlı alarm izni verilmemişse sistem ekranına yönlendirir. */
fun requestExactAlarmIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT >= 31) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData("package:${context.packageName}".toUri())
                )
            }
        }
    }
}

/**
 * İlk açılışta bildirim izninin neden gerektiğini anlatan tanıtım diyaloğu.
 * "İzin ver" bildirim iznini ister; ardından gerekiyorsa tam zamanlı alarm
 * izni için sistem ekranına yönlendirir.
 */
@Composable
fun OnboardingDialog(onFinished: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        requestExactAlarmIfNeeded(context)
        onFinished()
    }

    AlertDialog(
        onDismissRequest = onFinished,
        icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null) },
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = { Text(stringResource(R.string.onboarding_message)) },
        confirmButton = {
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    requestExactAlarmIfNeeded(context)
                    onFinished()
                }
            }) { Text(stringResource(R.string.onboarding_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onFinished) { Text(stringResource(R.string.onboarding_later)) }
        },
    )
}

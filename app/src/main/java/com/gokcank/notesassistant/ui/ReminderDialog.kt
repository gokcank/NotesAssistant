package com.gokcank.notesassistant.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.gokcank.notesassistant.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

/** Önce tarih, sonra saat seçtiren iki adımlı hatırlatıcı zamanı seçici. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    initial: LocalDateTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initial.toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { step = 1 },
                    enabled = dateState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.set_reminder)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute)))
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

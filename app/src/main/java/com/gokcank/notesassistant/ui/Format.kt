package com.gokcank.notesassistant.ui

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun formatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.getDefault())

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter())

fun formatDateTime(dateTime: LocalDateTime): String = dateTime.format(formatter())

fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

package com.gokcank.notesassistant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Not kartı renk paleti; dizin 0 = varsayılan (tema yüzeyi). */
private val lightNoteColors = listOf(
    null,
    Color(0xFFFFF59D), // sarı
    Color(0xFFFFCC80), // turuncu
    Color(0xFFEF9A9A), // kırmızı
    Color(0xFFF48FB1), // pembe
    Color(0xFFCE93D8), // mor
    Color(0xFF90CAF9), // mavi
    Color(0xFFA5D6A7), // yeşil
    Color(0xFFB0BEC5), // gri
)

private val darkNoteColors = listOf(
    null,
    Color(0xFF5C5427), // sarı
    Color(0xFF5F4527), // turuncu
    Color(0xFF5C3434), // kırmızı
    Color(0xFF5C3446), // pembe
    Color(0xFF4A3A55), // mor
    Color(0xFF2E4258), // mavi
    Color(0xFF33513A), // yeşil
    Color(0xFF37474F), // gri
)

const val NOTE_COLOR_COUNT = 9

/** Seçili temaya (açık/koyu) uygun kart rengini döndürür; 0 veya geçersiz dizin için null. */
@Composable
fun noteContainerColor(index: Int): Color? {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return (if (isDark) darkNoteColors else lightNoteColors).getOrNull(index)
}

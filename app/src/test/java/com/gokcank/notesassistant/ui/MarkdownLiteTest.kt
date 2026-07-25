package com.gokcank.notesassistant.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLiteTest {

    // --- Önizleme (render): işaretler atılır ---

    @Test
    fun `kalin isaretleri atilir ve stil uygulanir`() {
        val result = MarkdownLite.render("**kalın** yazı")
        assertEquals("kalın yazı", result.text)
        assertTrue(
            result.spanStyles.any {
                it.item.fontWeight == FontWeight.Bold && it.start == 0 && it.end == 5
            }
        )
    }

    @Test
    fun `italik isaretleri atilir ve stil uygulanir`() {
        val result = MarkdownLite.render("bu *eğik* olur")
        assertEquals("bu eğik olur", result.text)
        assertTrue(
            result.spanStyles.any {
                it.item.fontStyle == FontStyle.Italic && it.start == 3 && it.end == 7
            }
        )
    }

    @Test
    fun `baslik isareti atilir ve satir stillenir`() {
        val result = MarkdownLite.render("# Başlık\ngövde metni")
        assertEquals("Başlık\ngövde metni", result.text)
        assertTrue(
            result.spanStyles.any {
                it.item.fontWeight == FontWeight.Bold && it.start == 0 && it.end == 6
            }
        )
    }

    @Test
    fun `karisik bicimlendirme`() {
        val result = MarkdownLite.render("# Liste\n**önemli** ve *notlar*")
        assertEquals("Liste\nönemli ve notlar", result.text)
    }

    @Test
    fun `isaretsiz metin aynen kalir`() {
        val result = MarkdownLite.render("düz metin 5*3 çarpımı")
        assertEquals("düz metin 5*3 çarpımı", result.text)
    }

    @Test
    fun `satir ortasindaki diyez baslik sayilmaz`() {
        val result = MarkdownLite.render("bugün # işareti")
        assertEquals("bugün # işareti", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // --- Editör görünümü (styleKeepingMarkers): metin değişmez ---

    @Test
    fun `editor gorunumunde metin aynen korunur`() {
        val input = "# Başlık\n**kalın** ve *eğik*"
        val result = MarkdownLite.styleKeepingMarkers(input, Color.Gray)
        assertEquals(input, result.text)
    }

    @Test
    fun `editor gorunumunde kalin bolge stillenir`() {
        val result = MarkdownLite.styleKeepingMarkers("**ab**", Color.Gray)
        assertTrue(
            result.spanStyles.any {
                it.item.fontWeight == FontWeight.Bold && it.start == 0 && it.end == 6
            }
        )
    }

    @Test
    fun `cift yildiz italik sayilmaz`() {
        val result = MarkdownLite.styleKeepingMarkers("**ab**", Color.Gray)
        assertTrue(result.spanStyles.none { it.item.fontStyle == FontStyle.Italic })
    }
}

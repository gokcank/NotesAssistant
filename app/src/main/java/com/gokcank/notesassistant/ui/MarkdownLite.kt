package com.gokcank.notesassistant.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp

/**
 * Hafif biçimlendirme: `**kalın**`, `*italik*`, satır başında `# ` başlık.
 * Editörde işaretler korunarak canlı stillenir; kart önizlemesinde işaretler atılır.
 */
object MarkdownLite {

    private val boldRegex = Regex("""\*\*([^\n]+?)\*\*""")
    private val italicRegex = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)""")
    private val inlineRegex = Regex("""\*\*([^\n]+?)\*\*|(?<!\*)\*([^*\n]+)\*(?!\*)""")

    private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
    private val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
    private val headingStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)
    private val previewHeadingStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)

    /** Editör görünümü: metin aynen kalır, stiller eklenir, işaret karakterleri soluklaşır. */
    fun styleKeepingMarkers(text: String, markerColor: Color): AnnotatedString = buildAnnotatedString {
        append(text)
        val marker = SpanStyle(color = markerColor)
        var lineStart = 0
        text.split('\n').forEach { line ->
            if (line.startsWith("# ")) {
                addStyle(headingStyle, lineStart, lineStart + line.length)
                addStyle(marker, lineStart, lineStart + 2)
            }
            lineStart += line.length + 1
        }
        boldRegex.findAll(text).forEach { m ->
            addStyle(boldStyle, m.range.first, m.range.last + 1)
            addStyle(marker, m.range.first, m.range.first + 2)
            addStyle(marker, m.range.last - 1, m.range.last + 1)
        }
        italicRegex.findAll(text).forEach { m ->
            addStyle(italicStyle, m.range.first, m.range.last + 1)
            addStyle(marker, m.range.first, m.range.first + 1)
            addStyle(marker, m.range.last, m.range.last + 1)
        }
    }

    /** Kart önizlemesi: işaretler atılır, stiller uygulanır. */
    fun render(text: String): AnnotatedString = buildAnnotatedString {
        text.split('\n').forEachIndexed { i, rawLine ->
            if (i > 0) append('\n')
            val heading = rawLine.startsWith("# ")
            val line = if (heading) rawLine.removePrefix("# ") else rawLine
            val start = length
            appendInline(line)
            if (heading) addStyle(previewHeadingStyle, start, length)
        }
    }

    private fun AnnotatedString.Builder.appendInline(line: String) {
        var last = 0
        inlineRegex.findAll(line).forEach { m ->
            append(line.substring(last, m.range.first))
            val boldText = m.groupValues[1]
            val start = length
            if (boldText.isNotEmpty()) {
                append(boldText)
                addStyle(boldStyle, start, length)
            } else {
                append(m.groupValues[2])
                addStyle(italicStyle, start, length)
            }
            last = m.range.last + 1
        }
        append(line.substring(last))
    }
}

/** Editör metin alanına canlı biçimlendirme uygular; metnin kendisi değişmez. */
class MarkdownLiteTransformation(private val markerColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            MarkdownLite.styleKeepingMarkers(text.text, markerColor),
            OffsetMapping.Identity,
        )
}

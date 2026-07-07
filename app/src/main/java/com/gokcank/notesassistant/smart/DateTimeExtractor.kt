package com.gokcank.notesassistant.smart

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

data class DetectedDateTime(val dateTime: LocalDateTime, val matchedText: String)

/**
 * Not metnindeki doğal dil tarih/saat ifadelerini çıkarır ve hatırlatıcı önerisi üretir.
 * Türkçe ("yarın 15:00", "pazartesi", "3 gün sonra") ve İngilizce
 * ("tomorrow 3pm", "monday", "in 3 days") aynı anda desteklenir.
 */
object DateTimeExtractor {

    private val relativeTrRegex =
        Regex("""(\d+)\s*(dakika|dk|saat|gün|gun|hafta|ay)\s+sonra""")
    private val relativeEnRegex =
        Regex("""\bin\s+(\d+)\s+(minutes?|mins?|hours?|days?|weeks?|months?)\b""")
    private val amPmRegex = Regex("""\b(1[0-2]|0?[1-9])(?:[:.]([0-5]\d))?\s*(am|pm)\b""")
    private val bareTimeRegex = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val hourWordRegex = Regex("""\b(?:saat|at)\s+([01]?\d|2[0-3])(?:[:.]([0-5]\d))?\b""")
    private val numericDateRegex = Regex("""\b(\d{1,2})[./](\d{1,2})(?:[./](\d{4}))?\b""")

    private val months = mapOf(
        // Türkçe
        "ocak" to 1, "şubat" to 2, "subat" to 2, "mart" to 3, "nisan" to 4,
        "mayıs" to 5, "mayis" to 5, "haziran" to 6, "temmuz" to 7,
        "ağustos" to 8, "agustos" to 8, "eylül" to 9, "eylul" to 9,
        "ekim" to 10, "kasım" to 11, "kasim" to 11, "aralık" to 12, "aralik" to 12,
        // İngilizce
        "january" to 1, "february" to 2, "march" to 3, "april" to 4, "may" to 5,
        "june" to 6, "july" to 7, "august" to 8, "september" to 9,
        "october" to 10, "november" to 11, "december" to 12,
    )
    private val monthAlternation = months.keys.joinToString("|")
    private val dayMonthRegex =
        Regex("""(\d{1,2})(?:st|nd|rd|th)?\s+($monthAlternation)(?![a-z])""")
    private val monthDayRegex =
        Regex("""($monthAlternation)\s+(\d{1,2})(?:st|nd|rd|th)?\b""")

    private val weekdays = listOf(
        // "pazar", "pazartesi"nin parçası olduğundan pazartesi önce denenmeli
        "pazartesi" to DayOfWeek.MONDAY,
        "salı" to DayOfWeek.TUESDAY, "sali" to DayOfWeek.TUESDAY,
        "çarşamba" to DayOfWeek.WEDNESDAY, "carsamba" to DayOfWeek.WEDNESDAY,
        "perşembe" to DayOfWeek.THURSDAY, "persembe" to DayOfWeek.THURSDAY,
        "cumartesi" to DayOfWeek.SATURDAY, // "cuma"dan önce denenmeli
        "cuma" to DayOfWeek.FRIDAY,
        "pazar" to DayOfWeek.SUNDAY,
        "monday" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,
    )

    fun extract(text: String, now: LocalDateTime = LocalDateTime.now()): DetectedDateTime? {
        if (text.isBlank()) return null
        // ROOT locale + U+0307 temizliği: "İ" → "i", Türkçe "I" → ASCII eşdeğerine düşer
        val lower = text.lowercase(Locale.ROOT).replace("̇", "")

        relativeMatch(lower, now)?.let { return it }

        var date: LocalDate? = null
        var dateMatch: String? = null

        when {
            "öbür gün" in lower || "obur gun" in lower || "day after tomorrow" in lower -> {
                date = now.toLocalDate().plusDays(2); dateMatch = "öbür gün / day after tomorrow"
            }
            "yarın" in lower || "yarin" in lower || "tomorrow" in lower -> {
                date = now.toLocalDate().plusDays(1); dateMatch = "yarın / tomorrow"
            }
            "bugün" in lower || "bugun" in lower || "today" in lower || "tonight" in lower -> {
                date = now.toLocalDate(); dateMatch = "bugün / today"
            }
        }

        if (date == null) {
            for ((word, dayOfWeek) in weekdays) {
                if (word in lower) {
                    var d = now.toLocalDate().plusDays(1)
                    while (d.dayOfWeek != dayOfWeek) d = d.plusDays(1)
                    date = d
                    dateMatch = word
                    break
                }
            }
        }

        if (date == null) {
            (dayMonthRegex.find(lower)?.let { Triple(it.groupValues[1], it.groupValues[2], it.value) }
                ?: monthDayRegex.find(lower)?.let { Triple(it.groupValues[2], it.groupValues[1], it.value) }
                )?.let { (dayText, monthText, matched) ->
                    val day = dayText.toIntOrNull()
                    val month = months[monthText]
                    if (day != null && day in 1..31 && month != null) {
                        var d = runCatching { LocalDate.of(now.year, month, day) }.getOrNull()
                        if (d != null) {
                            if (d.isBefore(now.toLocalDate())) d = d.plusYears(1)
                            date = d
                            dateMatch = matched
                        }
                    }
                }
        }

        if (date == null) {
            numericDateRegex.find(lower)?.let { m ->
                val day = m.groupValues[1].toIntOrNull() ?: return@let
                val month = m.groupValues[2].toIntOrNull() ?: return@let
                if (day !in 1..31 || month !in 1..12) return@let
                val year = m.groupValues[3].toIntOrNull()
                var d = runCatching {
                    LocalDate.of(year ?: now.year, month, day)
                }.getOrNull() ?: return@let
                if (year == null && d.isBefore(now.toLocalDate())) d = d.plusYears(1)
                date = d
                dateMatch = m.value
            }
        }

        var time: LocalTime? = null
        var timeMatch: String? = null

        amPmRegex.find(lower)?.let { m ->
            var hour = m.groupValues[1].toIntOrNull() ?: return@let
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            if (m.groupValues[3] == "pm" && hour != 12) hour += 12
            if (m.groupValues[3] == "am" && hour == 12) hour = 0
            time = LocalTime.of(hour, minute)
            timeMatch = m.value
        }
        if (time == null) {
            hourWordRegex.find(lower)?.let { m ->
                val hour = m.groupValues[1].toIntOrNull() ?: return@let
                val minute = m.groupValues[2].toIntOrNull() ?: 0
                time = LocalTime.of(hour, minute)
                timeMatch = m.value
            }
        }
        if (time == null) {
            bareTimeRegex.find(lower)?.let { m ->
                time = LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
                timeMatch = m.value
            }
        }
        if (time == null) {
            when {
                "akşam" in lower || "aksam" in lower || "evening" in lower || "tonight" in lower -> {
                    time = LocalTime.of(20, 0); timeMatch = "akşam / evening"
                }
                "öğle" in lower || "ogle" in lower || "noon" in lower -> {
                    time = LocalTime.of(12, 0); timeMatch = "öğle / noon"
                }
                "sabah" in lower || "morning" in lower -> {
                    time = LocalTime.of(9, 0); timeMatch = "sabah / morning"
                }
            }
        }

        val resolvedDate = date
        val resolvedTime = time
        return when {
            resolvedDate != null && resolvedTime != null ->
                DetectedDateTime(
                    LocalDateTime.of(resolvedDate, resolvedTime),
                    listOfNotNull(dateMatch, timeMatch).joinToString(" "),
                )
            resolvedDate != null ->
                DetectedDateTime(LocalDateTime.of(resolvedDate, LocalTime.of(9, 0)), dateMatch.orEmpty())
            resolvedTime != null -> {
                var dt = LocalDateTime.of(now.toLocalDate(), resolvedTime)
                if (!dt.isAfter(now)) dt = dt.plusDays(1)
                DetectedDateTime(dt, timeMatch.orEmpty())
            }
            else -> null
        }
    }

    private fun relativeMatch(lower: String, now: LocalDateTime): DetectedDateTime? {
        relativeTrRegex.find(lower)?.let { m ->
            val amount = m.groupValues[1].toLongOrNull() ?: return@let
            val dt = when (m.groupValues[2]) {
                "dakika", "dk" -> now.plusMinutes(amount)
                "saat" -> now.plusHours(amount)
                "hafta" -> now.plusWeeks(amount)
                "ay" -> now.plusMonths(amount)
                else -> now.plusDays(amount)
            }
            return DetectedDateTime(dt, m.value)
        }
        relativeEnRegex.find(lower)?.let { m ->
            val amount = m.groupValues[1].toLongOrNull() ?: return@let
            val unit = m.groupValues[2]
            val dt = when {
                unit.startsWith("min") -> now.plusMinutes(amount)
                unit.startsWith("hour") -> now.plusHours(amount)
                unit.startsWith("week") -> now.plusWeeks(amount)
                unit.startsWith("month") -> now.plusMonths(amount)
                else -> now.plusDays(amount)
            }
            return DetectedDateTime(dt, m.value)
        }
        return null
    }
}

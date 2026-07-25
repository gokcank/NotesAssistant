package com.gokcank.notesassistant.smart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class DateTimeExtractorTest {

    /** Sabit referans an: 15 Temmuz 2026 Çarşamba, 10:30 */
    private val now = LocalDateTime.of(2026, 7, 15, 10, 30)

    private fun extract(text: String): LocalDateTime? =
        DateTimeExtractor.extract(text, now)?.dateTime

    // --- Göreli ifadeler ---

    @Test
    fun `uc gun sonra`() {
        assertEquals(LocalDateTime.of(2026, 7, 18, 10, 30), extract("fatura öde 3 gün sonra"))
    }

    @Test
    fun `kirkbes dakika sonra`() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 11, 15), extract("çamaşırı as 45 dakika sonra"))
    }

    @Test
    fun `iki hafta sonra`() {
        assertEquals(LocalDateTime.of(2026, 7, 29, 10, 30), extract("2 hafta sonra kontrol"))
    }

    @Test
    fun `in two hours`() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 12, 30), extract("call mom in 2 hours"))
    }

    // --- Yarın / bugün / öbür gün ---

    @Test
    fun `yarin saatle birlikte`() {
        assertEquals(LocalDateTime.of(2026, 7, 16, 15, 0), extract("yarın 15:00 toplantı"))
    }

    @Test
    fun `buyuk harfli YARIN da algilanir`() {
        assertEquals(LocalDateTime.of(2026, 7, 16, 15, 0), extract("YARIN 15:00"))
    }

    @Test
    fun `tomorrow 3pm`() {
        assertEquals(LocalDateTime.of(2026, 7, 16, 15, 0), extract("dentist tomorrow 3pm"))
    }

    @Test
    fun `yarin saatsiz varsayilan sabah dokuz`() {
        assertEquals(LocalDateTime.of(2026, 7, 16, 9, 0), extract("yarın kargoyu al"))
    }

    @Test
    fun `obur gun aksam`() {
        assertEquals(LocalDateTime.of(2026, 7, 17, 20, 0), extract("öbür gün akşam misafir var"))
    }

    @Test
    fun `bugun ogle`() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 12, 0), extract("bugün öğle yemeği"))
    }

    // --- Hafta günleri ---

    @Test
    fun `pazartesi gelecek haftaya denk gelir`() {
        assertEquals(LocalDateTime.of(2026, 7, 20, 9, 0), extract("pazartesi spor salonu"))
    }

    @Test
    fun `cumartesi cuma ile karismaz`() {
        // 18 Temmuz 2026 = Cumartesi; "cuma" (17'si) olarak algılanmamalı
        assertEquals(LocalDateTime.of(2026, 7, 18, 9, 0), extract("cumartesi kahvaltı"))
    }

    @Test
    fun `friday in english`() {
        assertEquals(LocalDateTime.of(2026, 7, 17, 9, 0), extract("friday movie night"))
    }

    @Test
    fun `pazartesi saat dokuz`() {
        assertEquals(LocalDateTime.of(2026, 7, 20, 9, 0), extract("Pazartesi saat 9 servis"))
    }

    // --- Sayısal ve ay adlı tarihler ---

    @Test
    fun `tam tarih ve saat kelimesi`() {
        assertEquals(LocalDateTime.of(2026, 8, 12, 10, 0), extract("12.08.2026 saat 10 randevu"))
    }

    @Test
    fun `yilsiz gecmis tarih gelecek yila kayar`() {
        // 3 Mayıs bu yıl geçti (referans: 15 Temmuz) → 2027'ye kaymalı
        assertEquals(LocalDateTime.of(2027, 5, 3, 9, 0), extract("3.05 yıldönümü"))
    }

    @Test
    fun `turkce ay adi`() {
        assertEquals(LocalDateTime.of(2026, 8, 12, 9, 0), extract("12 ağustos tatil başlangıcı"))
    }

    @Test
    fun `ingilizce ay adi`() {
        assertEquals(LocalDateTime.of(2026, 8, 12, 9, 0), extract("august 12 vacation"))
    }

    // --- Yalnız saat ---

    @Test
    fun `gelecekteki saat bugune kurulur`() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 45), extract("14:45 ilaç"))
    }

    @Test
    fun `gecmis saat ertesi gune kayar`() {
        // Referans 10:30; 09:00 geçti → yarın 09:00 olmalı
        assertEquals(LocalDateTime.of(2026, 7, 16, 9, 0), extract("09:00 ilaç"))
    }

    // --- Algılanmaması gerekenler ---

    @Test
    fun `tarihsiz metin null doner`() {
        assertNull(extract("süt ekmek yumurta al"))
    }

    @Test
    fun `bos metin null doner`() {
        assertNull(extract(""))
    }
}

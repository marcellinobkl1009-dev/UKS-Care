package com.sekolah.ukscare.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    private val jakarta = ZoneId.of("Asia/Jakarta")

    @Test
    fun `format tanggal dan jam memakai locale id dan zona yang diberikan`() {
        val waktu = jakarta(2026, 10, 19, 9, 30)
        assertEquals("Senin, 19 Okt 2026 • 09.30", DateUtils.formatTanggalJam(waktu, jakarta))
    }

    @Test
    fun `format jam selalu dua digit`() {
        val waktu = jakarta(2026, 10, 19, 0, 5)
        assertEquals("Senin, 19 Okt 2026 • 00.05", DateUtils.formatTanggalJam(waktu, jakarta))
    }

    @Test
    fun `startOfDay mengembalikan tengah malam lokal`() {
        val siang = jakarta(2026, 10, 19, 15, 45)
        assertEquals(jakarta(2026, 10, 19, 0, 0), DateUtils.startOfDay(siang, jakarta))
    }

    @Test
    fun `startOfNextDay mengembalikan tengah malam hari berikutnya`() {
        val malam = jakarta(2026, 10, 19, 23, 59)
        assertEquals(jakarta(2026, 10, 20, 0, 0), DateUtils.startOfNextDay(malam, jakarta))
    }

    @Test
    fun `startOfDay memakai zona yang diberikan, bukan zona default`() {
        // 2026-10-18T20.00Z sudah tanggal 19 di Jakarta tetapi masih tanggal 18 di UTC.
        val waktu = Instant.parse("2026-10-18T20:00:00Z").toEpochMilli()
        assertEquals(
            Instant.parse("2026-10-18T17:00:00Z").toEpochMilli(),
            DateUtils.startOfDay(waktu, jakarta),
        )
        assertEquals(
            Instant.parse("2026-10-18T00:00:00Z").toEpochMilli(),
            DateUtils.startOfDay(waktu, ZoneOffset.UTC),
        )
    }

    @Test
    fun `DatePicker UTC diubah menjadi awal hari lokal`() {
        val dariPicker = LocalDate.of(2026, 10, 19).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(jakarta(2026, 10, 19, 0, 0), DateUtils.dariDatePickerKeLokal(dariPicker, jakarta))
        // Zona barat: tengah malam lokal jatuh setelah tengah malam UTC.
        assertEquals(
            Instant.parse("2026-10-19T04:00:00Z").toEpochMilli(),
            DateUtils.dariDatePickerKeLokal(dariPicker, ZoneId.of("America/New_York")),
        )
    }

    private fun jakarta(tahun: Int, bulan: Int, hari: Int, jam: Int, menit: Int): Long =
        ZonedDateTime.of(tahun, bulan, hari, jam, menit, 0, 0, jakarta).toInstant().toEpochMilli()
}

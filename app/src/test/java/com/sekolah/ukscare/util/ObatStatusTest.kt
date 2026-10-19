package com.sekolah.ukscare.util

import com.sekolah.ukscare.data.model.Obat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Batas-batas nilai status obat (AGENTS.md 6.4 dan 9). */
class ObatStatusTest {

    private val hariIni = LocalDate.of(2026, 10, 19)

    @Test
    fun `stok empat dianggap kritis`() {
        assertEquals(StatusObat.STOK_KRITIS, ObatStatus.klasifikasi(4, hariIni.plusDays(200), hariIni))
    }

    @Test
    fun `stok lima aman bila kadaluwarsa masih jauh`() {
        assertEquals(StatusObat.AMAN, ObatStatus.klasifikasi(5, hariIni.plusDays(200), hariIni))
    }

    @Test
    fun `kadaluwarsa kemarin berstatus kadaluwarsa`() {
        assertEquals(StatusObat.KADALUWARSA, ObatStatus.klasifikasi(10, hariIni.minusDays(1), hariIni))
    }

    @Test
    fun `kadaluwarsa hari ini belum kadaluwarsa dan masih bisa diberikan`() {
        assertEquals(StatusObat.HAMPIR_KADALUWARSA, ObatStatus.klasifikasi(10, hariIni, hariIni))
    }

    @Test
    fun `kadaluwarsa tepat tiga puluh hari berstatus hampir kadaluwarsa`() {
        assertEquals(StatusObat.HAMPIR_KADALUWARSA, ObatStatus.klasifikasi(10, hariIni.plusDays(30), hariIni))
    }

    @Test
    fun `kadaluwarsa tiga puluh satu hari berstatus aman`() {
        assertEquals(StatusObat.AMAN, ObatStatus.klasifikasi(10, hariIni.plusDays(31), hariIni))
    }

    @Test
    fun `kadaluwarsa diutamakan daripada stok kritis`() {
        assertEquals(StatusObat.KADALUWARSA, ObatStatus.klasifikasi(2, hariIni.minusDays(3), hariIni))
    }

    @Test
    fun `stok kritis diutamakan daripada hampir kadaluwarsa`() {
        assertEquals(StatusObat.STOK_KRITIS, ObatStatus.klasifikasi(4, hariIni.plusDays(10), hariIni))
    }

    @Test
    fun `penilaian memakai granularitas hari pada zona yang diberikan`() {
        val jakarta = ZoneId.of("Asia/Jakarta")
        val sekarang = waktu(2026, 10, 19, 23, 30, jakarta)
        val kadaluwarsaAwalHariIni = waktu(2026, 10, 19, 0, 0, jakarta)

        // Di Jakarta masih tanggal 19 -> belum kadaluwarsa.
        assertEquals(
            StatusObat.HAMPIR_KADALUWARSA,
            ObatStatus.klasifikasi(10, kadaluwarsaAwalHariIni, sekarang, jakarta),
        )
        // Zona yang sama instannya bisa berbeda tanggal: di New York instan itu sudah tanggal 18.
        assertEquals(
            StatusObat.KADALUWARSA,
            ObatStatus.klasifikasi(10, kadaluwarsaAwalHariIni, sekarang, ZoneId.of("America/New_York")),
        )
    }

    @Test
    fun `obat bermasalah diurutkan lebih dulu`() {
        val zone = ZoneId.of("Asia/Jakarta")
        val sekarang = waktu(2026, 10, 19, 9, 0, zone)
        fun awalHari(hari: LocalDate): Long = hari.atStartOfDay(zone).toInstant().toEpochMilli()

        val daftar = listOf(
            Obat(1, "Aman", stok = 20, expiredDate = awalHari(hariIni.plusDays(100))),
            Obat(2, "Hampir", stok = 20, expiredDate = awalHari(hariIni.plusDays(10))),
            Obat(3, "Kritis", stok = 3, expiredDate = awalHari(hariIni.plusDays(100))),
            Obat(4, "Lampau", stok = 20, expiredDate = awalHari(hariIni.minusDays(1))),
        )

        assertEquals(
            listOf("Lampau", "Kritis", "Hampir", "Aman"),
            ObatStatus.urutkan(daftar, sekarang, zone).map { it.namaObat },
        )
    }

    private fun waktu(tahun: Int, bulan: Int, hari: Int, jam: Int, menit: Int, zone: ZoneId): Long =
        ZonedDateTime.of(tahun, bulan, hari, jam, menit, 0, 0, zone).toInstant().toEpochMilli()
}

package com.sekolah.ukscare.util

import com.sekolah.ukscare.data.model.Obat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Status kesehatan stok sebuah obat (lihat AGENTS.md 6.4). */
enum class StatusObat {
    KADALUWARSA,
    STOK_KRITIS,
    HAMPIR_KADALUWARSA,
    AMAN,
}

/**
 * Penilaian status obat berbasis hari pada zona waktu tertentu.
 * Semua masukan waktu dikirim pemanggil agar hasilnya deterministik.
 */
object ObatStatus {

    /** Stok di bawah nilai ini dianggap kritis: stok 4 kritis, stok 5 tidak. */
    const val BATAS_STOK_KRITIS = 5

    /** Batas "hampir kadaluwarsa", inklusif 0..30 hari. */
    const val BATAS_HARI_HAMPIR_KADALUWARSA = 30L

    fun dari(obat: Obat, sekarang: Long, zone: ZoneId): StatusObat =
        klasifikasi(obat.stok, obat.expiredDate, sekarang, zone)

    fun klasifikasi(stok: Int, expiredDate: Long, sekarang: Long, zone: ZoneId): StatusObat {
        val hariIni = Instant.ofEpochMilli(sekarang).atZone(zone).toLocalDate()
        val hariKadaluwarsa = Instant.ofEpochMilli(expiredDate).atZone(zone).toLocalDate()
        return klasifikasi(stok, hariKadaluwarsa, hariIni)
    }

    /**
     * Urutan penilaian: Kadaluwarsa, lalu Stok Kritis, lalu Hampir Kadaluwarsa (0..30 hari), sisanya Aman.
     * Obat yang kadaluwarsa pada hari ini masih boleh diberikan (baru dianggap kadaluwarsa setelah harinya lewat).
     */
    fun klasifikasi(stok: Int, hariKadaluwarsa: LocalDate, hariIni: LocalDate): StatusObat = when {
        hariKadaluwarsa.isBefore(hariIni) -> StatusObat.KADALUWARSA
        stok < BATAS_STOK_KRITIS -> StatusObat.STOK_KRITIS
        ChronoUnit.DAYS.between(hariIni, hariKadaluwarsa) <= BATAS_HARI_HAMPIR_KADALUWARSA ->
            StatusObat.HAMPIR_KADALUWARSA
        else -> StatusObat.AMAN
    }

    /** Bobot urutan tampilan: makin kecil makin mendesak. */
    fun bobotUrut(status: StatusObat): Int = when (status) {
        StatusObat.KADALUWARSA -> 0
        StatusObat.STOK_KRITIS -> 1
        StatusObat.HAMPIR_KADALUWARSA -> 2
        StatusObat.AMAN -> 3
    }

    /** Obat bermasalah (merah) muncul lebih dulu, lalu berdasarkan nama. */
    fun urutkan(daftar: List<Obat>, sekarang: Long, zone: ZoneId): List<Obat> = daftar.sortedWith(
        compareBy({ bobotUrut(dari(it, sekarang, zone)) }, { it.namaObat }),
    )
}

package com.sekolah.ukscare.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Semua fungsi di sini menerima [ZoneId] secara eksplisit supaya perilakunya
 * dapat diuji dan tidak bergantung pada default perangkat.
 */
object DateUtils {

    // Locale("id", "ID") sesuai AGENTS.md 7.4; Locale.of() belum tersedia di minSdk 24.
    @Suppress("DEPRECATION")
    private val LOCALE_ID = Locale("id", "ID")

    private val FORMAT_TANGGAL = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", LOCALE_ID)

    private val FORMAT_JAM = DateTimeFormatter.ofPattern("HH.mm", LOCALE_ID)

    private const val PEMISAH_TANGGAL_JAM = " • "

    /** Awal hari lokal (00.00) dari [millis] pada zona [zone]. */
    fun startOfDay(millis: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

    /** Awal hari berikutnya, dipakai untuk membatasi query "hari ini". */
    fun startOfNextDay(millis: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millis)
        .atZone(zone)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

    /** Contoh keluaran: `Senin, 19 Okt 2026 • 09.30`. */
    fun formatTanggalJam(millis: Long, zone: ZoneId): String {
        val waktu = Instant.ofEpochMilli(millis).atZone(zone)
        return waktu.format(FORMAT_TANGGAL) + PEMISAH_TANGGAL_JAM + waktu.format(FORMAT_JAM)
    }

    /**
     * DatePicker Material 3 mengembalikan millis tengah malam **UTC**.
     * Nilai itu diubah menjadi awal hari di zona lokal, karena `expired_date`
     * disimpan sebagai awal hari lokal dan dibandingkan oleh query penjaga stok.
     */
    fun dariDatePickerKeLokal(millisUtc: Long, zone: ZoneId): Long = Instant.ofEpochMilli(millisUtc)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}

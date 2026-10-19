package com.sekolah.ukscare.data

import android.content.Context
import androidx.room.Room
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.util.DateUtils
import java.time.ZoneId
import java.time.ZonedDateTime

/** Waktu tetap untuk pengujian agar hasilnya deterministik. */
object WaktuUji {
    val ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    const val SATU_HARI_MILLIS = 24L * 60 * 60 * 1000

    /** Senin, 19 Okt 2026 09.30 waktu Jakarta. */
    val SEKARANG: Long = ZonedDateTime.of(2026, 10, 19, 9, 30, 0, 0, ZONE).toInstant().toEpochMilli()
    val AWAL_HARI_INI: Long = DateUtils.startOfDay(SEKARANG, ZONE)
    val AWAL_HARI_BESOK: Long = DateUtils.startOfNextDay(SEKARANG, ZONE)
}

/** Database in-memory dengan callback data awal yang sama seperti di produksi. */
fun buatDatabaseInMemory(context: Context, sekarang: Long = WaktuUji.SEKARANG): AppDatabase =
    Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .addCallback(AppDatabase.SeedCallback { sekarang })
        .build()

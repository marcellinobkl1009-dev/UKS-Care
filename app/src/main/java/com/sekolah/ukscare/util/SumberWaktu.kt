package com.sekolah.ukscare.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Sumber waktu yang dapat di-inject. ViewModel tidak pernah memanggil
 * `System.currentTimeMillis()` langsung supaya dapat diuji secara deterministik.
 */
fun interface SumberWaktu {
    fun sekarang(): Long
}

/** Implementasi produksi: jam perangkat. */
object SumberWaktuSistem : SumberWaktu {
    override fun sekarang(): Long = System.currentTimeMillis()
}

/**
 * Detak berkala untuk memperbarui durasi istirahat berjalan.
 * Dipisahkan dari [SumberWaktu] agar pengujian dapat mengirim detak secara manual.
 */
object Ticker {

    /** 30 detik: cukup responsif untuk durasi istirahat, tanpa membebani baterai. */
    const val INTERVAL_DETAK_MS = 30_000L

    fun detak(intervalMillis: Long = INTERVAL_DETAK_MS): Flow<Unit> = flow {
        while (true) {
            delay(intervalMillis)
            emit(Unit)
        }
    }
}

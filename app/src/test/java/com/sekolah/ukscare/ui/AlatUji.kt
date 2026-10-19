package com.sekolah.ukscare.ui

import com.sekolah.ukscare.util.SumberWaktu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** Jam palsu yang dapat digeser dari pengujian. */
class JamUji(var waktu: Long) : SumberWaktu {
    override fun sekarang(): Long = waktu

    fun maju(millis: Long) {
        waktu += millis
    }
}

/**
 * Detak yang dikendalikan pengujian. `replay = 1` membuat nilai detak tidak hilang
 * dan `emit` tidak pernah menggantung walaupun belum ada kolektor.
 */
fun detakUji(): MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)

/** Menunggu state memenuhi [predikat] dengan batas waktu nyata, agar pengujian tidak menggantung. */
suspend fun <T> Flow<T>.tunggu(predikat: (T) -> Boolean): T = withTimeout(BATAS_TUNGGU_MS) { first(predikat) }

private const val BATAS_TUNGGU_MS = 5_000L

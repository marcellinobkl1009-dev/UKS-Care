package com.sekolah.ukscare

import android.app.Application
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.data.repository.ObatRepository
import com.sekolah.ukscare.data.repository.PasienRepository

/**
 * Application class: tempat dependency dibuat manual (tanpa Hilt/Dagger).
 * Semua dibuat malas (lazy) agar tidak ada pekerjaan berat saat startup.
 */
class UksCareApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }

    val obatRepository: ObatRepository by lazy { ObatRepository(database.obatDao()) }

    val pasienRepository: PasienRepository by lazy {
        PasienRepository(db = database, obatRepository = obatRepository)
    }
}

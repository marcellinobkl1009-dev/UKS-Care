package com.sekolah.ukscare.data.local

import androidx.test.core.app.ApplicationProvider
import com.sekolah.ukscare.data.WaktuUji
import com.sekolah.ukscare.data.buatDatabaseInMemory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Callback pembuatan database harus mengisi 5 obat bawaan. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseSeedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = buatDatabaseInMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `database baru berisi lima obat`() = runBlocking {
        val daftar = db.obatDao().getAll().first()

        assertEquals(5, daftar.size)
        assertEquals(
            setOf("Paracetamol", "Antasida", "Betadine", "Plester", "Minyak Kayu Putih"),
            daftar.map { it.namaObat }.toSet(),
        )
    }

    @Test
    fun `data awal memakai stok sepuluh dan kadaluwarsa setahun kemudian`() = runBlocking {
        val daftar = db.obatDao().getAll().first()
        val setahun = WaktuUji.SEKARANG + 365L * WaktuUji.SATU_HARI_MILLIS

        assertTrue(daftar.all { it.stok == 10 })
        assertTrue(daftar.all { it.expiredDate == setahun })
    }
}

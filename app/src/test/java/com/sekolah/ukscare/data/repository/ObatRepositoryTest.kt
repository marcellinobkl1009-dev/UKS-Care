package com.sekolah.ukscare.data.repository

import androidx.test.core.app.ApplicationProvider
import com.sekolah.ukscare.data.WaktuUji
import com.sekolah.ukscare.data.buatDatabaseInMemory
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.data.model.Obat
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

/** Stok tidak boleh pernah negatif, baik lewat edit manual maupun restock cepat. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ObatRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ObatRepository

    @Before
    fun setUp() {
        db = buatDatabaseInMemory(ApplicationProvider.getApplicationContext())
        repository = ObatRepository(db.obatDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `atur stok dengan nilai negatif ditolak dan stok tidak berubah`() = runBlocking {
        val paracetamol = obat("Paracetamol")

        val hasil = repository.aturStok(paracetamol.id, -1)

        assertTrue(hasil.exceptionOrNull() is StokTidakValidException)
        assertEquals(10, stok(paracetamol.id))
    }

    @Test
    fun `restock yang membuat stok negatif ditolak`() = runBlocking {
        val betadine = obat("Betadine")

        val hasil = repository.tambahStok(betadine.id, -11)

        assertTrue(hasil.exceptionOrNull() is StokTidakValidException)
        assertEquals(10, stok(betadine.id))
    }

    @Test
    fun `restock sampai stok nol masih diterima`() = runBlocking {
        val plester = obat("Plester")

        assertTrue(repository.tambahStok(plester.id, -10).isSuccess)
        assertEquals(0, stok(plester.id))
    }

    @Test
    fun `restock positif menambah stok`() = runBlocking {
        val plester = obat("Plester")

        assertTrue(repository.tambahStok(plester.id, 5).isSuccess)
        assertEquals(15, stok(plester.id))
    }

    @Test
    fun `edit obat dengan stok negatif ditolak dan stok tidak berubah`() = runBlocking {
        val antasida = obat("Antasida")

        val hasil = repository.perbaruiObat(antasida.id, "Antasida", -5, antasida.expiredDate)

        assertTrue(hasil.exceptionOrNull() is StokTidakValidException)
        assertEquals(10, stok(antasida.id))
    }

    @Test
    fun `tambah obat dengan stok negatif ditolak`() = runBlocking {
        val hasil = repository.tambahObat("Obat Baru", stok = -3, expiredDate = WaktuUji.AWAL_HARI_INI)

        assertTrue(hasil.exceptionOrNull() is StokTidakValidException)
        assertEquals(5, db.obatDao().getAll().first().size)
    }

    @Test
    fun `hapus obat yang tidak ada dilaporkan gagal`() = runBlocking {
        assertTrue(repository.hapusObat(999).exceptionOrNull() is ObatTidakDitemukanException)
    }

    @Test
    fun `daftar obat yang bisa diberikan mengecualikan obat kadaluwarsa dan stok nol`() = runBlocking {
        val lampau = obatBaru("Obat Lampau", stok = 5, expiredDate = WaktuUji.AWAL_HARI_INI - WaktuUji.SATU_HARI_MILLIS)
        val habis = obatBaru("Obat Habis", stok = 0, expiredDate = WaktuUji.AWAL_HARI_INI + 30L * WaktuUji.SATU_HARI_MILLIS)
        val hariIni = obatBaru("Obat Hari Ini", stok = 2, expiredDate = WaktuUji.AWAL_HARI_INI)

        val daftar = repository.getObatDapatDiberikan(WaktuUji.AWAL_HARI_INI).first()
        val idTerpilih = daftar.map { it.id }

        // 5 obat bawaan + obat yang kadaluwarsa hari ini; obat lampau dan stok nol tidak ikut.
        assertEquals(6, daftar.size)
        assertTrue(idTerpilih.contains(hariIni.id))
        assertTrue(!idTerpilih.contains(lampau.id))
        assertTrue(!idTerpilih.contains(habis.id))
    }

    private suspend fun obat(nama: String): Obat = repository.getSemuaObat().first().first { it.namaObat == nama }

    private suspend fun obatBaru(nama: String, stok: Int, expiredDate: Long): Obat =
        db.obatDao().getById(repository.tambahObat(nama, stok, expiredDate).getOrThrow().toInt())!!

    private suspend fun stok(id: Int): Int = db.obatDao().getById(id)!!.stok
}

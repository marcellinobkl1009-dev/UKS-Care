package com.sekolah.ukscare.data.repository

import androidx.test.core.app.ApplicationProvider
import com.sekolah.ukscare.data.WaktuUji
import com.sekolah.ukscare.data.WaktuUji.SATU_HARI_MILLIS
import com.sekolah.ukscare.data.buatDatabaseInMemory
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.data.model.Pasien
import com.sekolah.ukscare.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Penyimpanan pasien + pengurangan stok harus atomik:
 * bila obat tidak bisa diberikan, tidak boleh ada baris pasien yang tersisa.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PasienRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var obatRepository: ObatRepository
    private lateinit var repository: PasienRepository

    @Before
    fun setUp() {
        db = buatDatabaseInMemory(ApplicationProvider.getApplicationContext())
        obatRepository = ObatRepository(db.obatDao())
        repository = PasienRepository(db, obatRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `simpan pasien dengan obat mengurangi stok tepat satu`() = runBlocking {
        val paracetamol = obat("Paracetamol")

        val hasil = repository.simpanPasien(masukan(obat = paracetamol), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)

        assertTrue(hasil is HasilSimpanPasien.Berhasil)
        assertEquals(9, stok(paracetamol.id))

        val daftar = repository.getPasienAktif().first()
        assertEquals(1, daftar.size)
        assertEquals("Andi Saputra", daftar.first().namaSiswa)
        assertEquals("Paracetamol", daftar.first().obatDiberikan)
        assertEquals(Pasien.STATUS_AKTIF, daftar.first().status)
        assertEquals(WaktuUji.SEKARANG, daftar.first().waktuMasuk)
        assertNull(daftar.first().waktuKeluar)
    }

    @Test
    fun `simpan pasien tanpa obat tidak mengubah stok`() = runBlocking {
        val sebelum = db.obatDao().getAll().first().associate { it.id to it.stok }

        val hasil = repository.simpanPasien(masukan(obat = null), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)

        assertTrue(hasil is HasilSimpanPasien.Berhasil)
        assertEquals(1, repository.getPasienAktif().first().size)
        assertEquals(sebelum, db.obatDao().getAll().first().associate { it.id to it.stok })
    }

    @Test
    fun `stok nol membuat penyimpanan gagal tanpa menyisakan baris pasien`() = runBlocking {
        val habis = obatBaru("Obat Habis", stok = 0, expiredDate = WaktuUji.AWAL_HARI_INI + 100L * SATU_HARI_MILLIS)

        val hasil = repository.simpanPasien(masukan(obat = habis), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)

        assertEquals(HasilSimpanPasien.GagalObat(AlasanStokGagal.STOK_HABIS), hasil)
        assertEquals(0, stok(habis.id))
        assertTrue(repository.getPasienAktif().first().isEmpty())
        assertTrue(repository.getRiwayat().first().isEmpty())
    }

    @Test
    fun `obat kadaluwarsa membuat penyimpanan gagal dan stok tidak berubah`() = runBlocking {
        val lampau = obatBaru("Obat Lampau", stok = 5, expiredDate = WaktuUji.AWAL_HARI_INI - SATU_HARI_MILLIS)

        val hasil = repository.simpanPasien(masukan(obat = lampau), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)

        assertEquals(HasilSimpanPasien.GagalObat(AlasanStokGagal.OBAT_KADALUWARSA), hasil)
        assertEquals(5, stok(lampau.id))
        assertTrue(repository.getPasienAktif().first().isEmpty())
        assertTrue(repository.getRiwayat().first().isEmpty())
    }

    @Test
    fun `obat yang kadaluwarsa hari ini masih bisa diberikan`() = runBlocking {
        val hariIni = obatBaru("Obat Hari Ini", stok = 2, expiredDate = WaktuUji.AWAL_HARI_INI)

        val hasil = repository.simpanPasien(masukan(obat = hariIni), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)

        assertTrue(hasil is HasilSimpanPasien.Berhasil)
        assertEquals(1, stok(hariIni.id))
    }

    @Test
    fun `selesai istirahat memindahkan pasien ke riwayat`() = runBlocking {
        val hasil = repository.simpanPasien(masukan(), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)
        assertTrue(hasil is HasilSimpanPasien.Berhasil)
        val idPasien = (hasil as HasilSimpanPasien.Berhasil).idPasien
        val waktuKeluar = WaktuUji.SEKARANG + 30 * 60 * 1000

        assertTrue(repository.selesaikanIstirahat(idPasien, waktuKeluar))

        assertTrue(repository.getPasienAktif().first().isEmpty())
        val riwayat = repository.getRiwayat().first()
        assertEquals(1, riwayat.size)
        assertEquals(Pasien.STATUS_SELESAI, riwayat.first().status)
        assertEquals(waktuKeluar, riwayat.first().waktuKeluar)
    }

    @Test
    fun `hitung pasien aktif dan kunjungan hari ini`() = runBlocking {
        repository.simpanPasien(masukan(nama = "Andi Saputra"), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)
        repository.simpanPasien(masukan(nama = "Budi"), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)

        assertEquals(2, repository.countPasienAktif().first())
        assertEquals(2, repository.countKunjunganHariIni(WaktuUji.AWAL_HARI_INI, WaktuUji.AWAL_HARI_BESOK).first())

        // Besok, kunjungan "hari ini" kembali nol.
        assertEquals(
            0,
            repository.countKunjunganHariIni(
                WaktuUji.AWAL_HARI_BESOK,
                WaktuUji.AWAL_HARI_BESOK + SATU_HARI_MILLIS,
            ).first(),
        )
        assertEquals(2, repository.countPasienAktif().first())
    }

    @Test
    fun `pencarian riwayat memakai LIKE dan hanya melihat pasien selesai`() = runBlocking {
        val hasil = repository.simpanPasien(masukan(nama = "Andi Saputra"), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)
        repository.simpanPasien(masukan(nama = "Budi"), WaktuUji.SEKARANG, WaktuUji.AWAL_HARI_INI)
        repository.selesaikanIstirahat((hasil as HasilSimpanPasien.Berhasil).idPasien, WaktuUji.SEKARANG)

        assertEquals(listOf("Andi Saputra"), repository.cariRiwayat("Andi").first().map { it.namaSiswa })
        assertEquals(listOf("Andi Saputra"), repository.cariRiwayat("andi").first().map { it.namaSiswa })
        assertTrue(repository.cariRiwayat("zzz").first().isEmpty())
        // Nama kosong berarti semua riwayat (pasien AKTIF tidak ikut).
        assertEquals(1, repository.cariRiwayat("  ").first().size)
    }

    private fun masukan(nama: String = "Andi Saputra", obat: Obat? = null) = MasukanPasien(
        namaSiswa = nama,
        kelas = "VII-A",
        jenisKelamin = Constants.JENIS_KELAMIN_LAKI_LAKI,
        keluhan = Constants.gabungPilihan(listOf("Pusing", "Demam")),
        tindakan = Constants.gabungPilihan(listOf("Istirahat", Constants.DIBERI_OBAT)),
        obat = obat,
    )

    private suspend fun obat(nama: String): Obat = obatRepository.getSemuaObat().first().first { it.namaObat == nama }

    private suspend fun obatBaru(nama: String, stok: Int, expiredDate: Long): Obat =
        db.obatDao().getById(obatRepository.tambahObat(nama, stok, expiredDate).getOrThrow().toInt())!!

    private suspend fun stok(id: Int): Int = db.obatDao().getById(id)!!.stok
}

package com.sekolah.ukscare.ui.form

import androidx.test.core.app.ApplicationProvider
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.WaktuUji
import com.sekolah.ukscare.data.buatDatabaseInMemory
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.data.model.Pasien
import com.sekolah.ukscare.data.repository.ObatRepository
import com.sekolah.ukscare.data.repository.PasienRepository
import com.sekolah.ukscare.ui.JamUji
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.ui.detakUji
import com.sekolah.ukscare.ui.tunggu
import com.sekolah.ukscare.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Validasi form, pemetaan hasil simpan, dan pencegahan simpan ganda (AGENTS.md 6.2, 7.3). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class FormPasienViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var obatRepository: ObatRepository
    private lateinit var pasienRepository: PasienRepository
    private lateinit var jam: JamUji
    private lateinit var detak: MutableSharedFlow<Unit>

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = buatDatabaseInMemory(ApplicationProvider.getApplicationContext(), WaktuUji.SEKARANG)
        obatRepository = ObatRepository(db.obatDao())
        pasienRepository = PasienRepository(db = db, obatRepository = obatRepository)
        jam = JamUji(WaktuUji.SEKARANG)
        detak = detakUji()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun buatViewModel() = FormPasienViewModel(
        pasienRepository = pasienRepository,
        obatRepository = obatRepository,
        sumberWaktu = jam,
        zone = WaktuUji.ZONE,
        detak = detak,
    )

    @Test
    fun `form kosong tidak disimpan dan setiap field dilaporkan salah`() = runBlocking {
        val viewModel = buatViewModel()

        viewModel.simpan()

        val kesalahan = viewModel.uiState.value.kesalahan
        assertEquals(UiText.Teks(R.string.validasi_nama_wajib), kesalahan.namaSiswa)
        assertEquals(UiText.Teks(R.string.validasi_kelas_wajib), kesalahan.kelas)
        assertEquals(UiText.Teks(R.string.validasi_jenis_kelamin_wajib), kesalahan.jenisKelamin)
        assertEquals(UiText.Teks(R.string.validasi_keluhan_wajib), kesalahan.keluhan)
        assertEquals(UiText.Teks(R.string.validasi_tindakan_wajib), kesalahan.tindakan)
        assertNull(kesalahan.obat)
        assertEquals(0, jumlahPasien())
    }

    @Test
    fun `tindakan diberi obat tanpa memilih obat ditolak`() = runBlocking {
        val viewModel = buatViewModel()
        isiFormValid(viewModel)
        viewModel.alihTindakan(Constants.DIBERI_OBAT)

        viewModel.simpan()

        val state = viewModel.uiState.value
        assertEquals(UiText.Teks(R.string.validasi_obat_wajib), state.kesalahan.obat)
        assertEquals(0, jumlahPasien())
    }

    @Test
    fun `memilih kelas dan keluhan membersihkan kesalahan field tersebut`() {
        val viewModel = buatViewModel()
        viewModel.simpan()
        assertEquals(UiText.Teks(R.string.validasi_kelas_wajib), viewModel.uiState.value.kesalahan.kelas)

        viewModel.pilihKelas(Constants.DAFTAR_KELAS.first())
        viewModel.alihKeluhan(Constants.PILIHAN_KELUHAN.first())

        assertNull(viewModel.uiState.value.kesalahan.kelas)
        assertNull(viewModel.uiState.value.kesalahan.keluhan)
    }

    @Test
    fun `membatalkan tindakan diberi obat menghapus obat terpilih`() {
        val viewModel = buatViewModel()
        viewModel.alihTindakan(Constants.DIBERI_OBAT)
        viewModel.pilihObat(3)

        viewModel.alihTindakan(Constants.DIBERI_OBAT)

        assertNull(viewModel.uiState.value.idObatTerpilih)
        assertFalse(viewModel.uiState.value.membutuhkanObat)
    }

    @Test
    fun `daftar obat form hanya berisi obat yang dapat diberikan`() = runBlocking {
        val paracetamol = obat("Paracetamol")
        // Stok dikosongkan dan satu obat dibuat kadaluwarsa.
        db.obatDao().updateStok(paracetamol.id, 0)
        val antasida = obat("Antasida")
        db.obatDao().updateObat(
            antasida.id,
            antasida.namaObat,
            antasida.stok,
            WaktuUji.AWAL_HARI_INI - WaktuUji.SATU_HARI_MILLIS,
        )

        val viewModel = buatViewModel()
        val state = viewModel.uiState.tunggu { it.daftarObat.isNotEmpty() }

        assertEquals(3, state.daftarObat.size)
        assertFalse(state.daftarObat.any { it.id == paracetamol.id })
        assertFalse(state.daftarObat.any { it.id == antasida.id })
    }

    @Test
    fun `simpan berhasil menyimpan pasien aktif mengurangi stok dan menyusun draf pesan`() = runBlocking {
        val paracetamol = obat("Paracetamol")
        val viewModel = buatViewModel()
        isiFormValid(viewModel)
        viewModel.alihTindakan(Constants.DIBERI_OBAT)
        viewModel.pilihObat(paracetamol.id)

        viewModel.simpan()

        val state = viewModel.uiState.tunggu { it.drafPesan != null }
        assertFalse(state.sedangMenyimpan)
        assertNull(state.errorSimpan)
        assertEquals(paracetamol.stok - 1, db.obatDao().getById(paracetamol.id)!!.stok)

        val pasien = pasienRepository.getPasienAktif().first().single()
        assertEquals("Budi Santoso", pasien.namaSiswa)
        assertEquals("VII-A", pasien.kelas)
        assertEquals(Constants.JENIS_KELAMIN_LAKI_LAKI, pasien.jenisKelamin)
        assertEquals("Pusing", pasien.keluhan)
        assertEquals("Istirahat, Diberi Obat", pasien.tindakan)
        assertEquals("Paracetamol", pasien.obatDiberikan)
        assertEquals(Pasien.STATUS_AKTIF, pasien.status)
        assertEquals(WaktuUji.SEKARANG, pasien.waktuMasuk)
        assertNull(pasien.waktuKeluar)

        val draf = state.drafPesan!!
        assertTrue(draf.contains("Budi Santoso"))
        assertTrue(draf.contains("Paracetamol"))
        assertTrue(draf.contains("VII-A"))
    }

    @Test
    fun `simpan tanpa obat menulis obat kosong pada draf pesan`() = runBlocking {
        val viewModel = buatViewModel()
        isiFormValid(viewModel)

        viewModel.simpan()

        val draf = viewModel.uiState.tunggu { it.drafPesan != null }.drafPesan!!
        assertTrue(draf.contains("\nObat diberikan: -"))
        assertNull(pasienRepository.getPasienAktif().first().single().obatDiberikan)
    }

    @Test
    fun `simpan dengan stok habis dilaporkan sebagai kesalahan stok`() = runBlocking {
        val paracetamol = obat("Paracetamol")
        val viewModel = buatViewModel()
        isiFormValid(viewModel)
        viewModel.alihTindakan(Constants.DIBERI_OBAT)
        viewModel.pilihObat(paracetamol.id)
        // Stok habis setelah obat dipilih (misalnya dipakai transaksi lain).
        db.obatDao().updateStok(paracetamol.id, 0)

        viewModel.simpan()

        val state = viewModel.uiState.tunggu { it.errorSimpan != null }
        assertEquals(UiText.Teks(R.string.pesan_stok_obat_habis), state.errorSimpan)
        assertFalse(state.sedangMenyimpan)
        assertNull(state.drafPesan)
        assertEquals(0, jumlahPasien())
        assertEquals(0, db.obatDao().getById(paracetamol.id)!!.stok)
    }

    @Test
    fun `simpan dengan obat kadaluwarsa dilaporkan sebagai kesalahan kadaluwarsa`() = runBlocking {
        val betadine = obat("Betadine")
        val viewModel = buatViewModel()
        isiFormValid(viewModel)
        viewModel.alihTindakan(Constants.DIBERI_OBAT)
        viewModel.pilihObat(betadine.id)
        db.obatDao().updateObat(
            betadine.id,
            betadine.namaObat,
            betadine.stok,
            WaktuUji.AWAL_HARI_INI - WaktuUji.SATU_HARI_MILLIS,
        )

        viewModel.simpan()

        assertEquals(
            UiText.Teks(R.string.pesan_obat_kadaluwarsa),
            viewModel.uiState.tunggu { it.errorSimpan != null }.errorSimpan,
        )
        assertEquals(0, jumlahPasien())
    }

    @Test
    fun `simpan dengan obat yang sudah dihapus dilaporkan sebagai obat tidak ditemukan`() = runBlocking {
        val plester = obat("Plester")
        val viewModel = buatViewModel()
        isiFormValid(viewModel)
        viewModel.alihTindakan(Constants.DIBERI_OBAT)
        viewModel.pilihObat(plester.id)
        db.obatDao().deleteById(plester.id)

        viewModel.simpan()

        assertEquals(
            UiText.Teks(R.string.pesan_obat_tidak_ditemukan),
            viewModel.uiState.tunggu { it.errorSimpan != null }.errorSimpan,
        )
        assertEquals(0, jumlahPasien())
    }

    @Test
    fun `simpan dua kali dengan cepat hanya menghasilkan satu pasien`() = runBlocking {
        val viewModel = buatViewModel()
        isiFormValid(viewModel)

        viewModel.simpan()
        viewModel.simpan()

        viewModel.uiState.tunggu { it.drafPesan != null }
        // Panggilan berikutnya setelah sukses juga diabaikan.
        viewModel.simpan()

        assertEquals(1, jumlahPasien())
    }

    private fun isiFormValid(viewModel: FormPasienViewModel) {
        viewModel.ubahNama("Budi Santoso")
        viewModel.pilihKelas("VII-A")
        viewModel.pilihJenisKelamin(Constants.JENIS_KELAMIN_LAKI_LAKI)
        viewModel.alihKeluhan("Pusing")
        viewModel.alihTindakan("Istirahat")
    }

    private suspend fun obat(nama: String): Obat =
        obatRepository.getSemuaObat().first().first { it.namaObat == nama }

    private suspend fun jumlahPasien(): Int =
        pasienRepository.getPasienAktif().first().size + pasienRepository.getRiwayat().first().size
}

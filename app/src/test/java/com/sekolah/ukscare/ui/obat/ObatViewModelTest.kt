package com.sekolah.ukscare.ui.obat

import androidx.test.core.app.ApplicationProvider
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.WaktuUji
import com.sekolah.ukscare.data.buatDatabaseInMemory
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.data.repository.ObatRepository
import com.sekolah.ukscare.ui.JamUji
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.ui.detakUji
import com.sekolah.ukscare.ui.tunggu
import com.sekolah.ukscare.util.StatusObat
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Urutan daftar obat dan pemetaan kesalahan aksi Kelola Obat (AGENTS.md 6.4). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ObatViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ObatRepository
    private lateinit var jam: JamUji
    private lateinit var detak: MutableSharedFlow<Unit>

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = buatDatabaseInMemory(ApplicationProvider.getApplicationContext(), WaktuUji.SEKARANG)
        repository = ObatRepository(db.obatDao())
        jam = JamUji(WaktuUji.SEKARANG)
        detak = detakUji()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun buatViewModel() = ObatViewModel(
        obatRepository = repository,
        sumberWaktu = jam,
        zone = WaktuUji.ZONE,
        detak = detak,
    )

    @Test
    fun `daftar obat mengurutkan obat bermasalah lebih dulu`() = runBlocking {
        val antasida = obat("Antasida")
        val betadine = obat("Betadine")
        val plester = obat("Plester")
        db.obatDao().updateObat(
            antasida.id,
            antasida.namaObat,
            antasida.stok,
            WaktuUji.AWAL_HARI_INI - WaktuUji.SATU_HARI_MILLIS,
        )
        db.obatDao().updateObat(
            betadine.id,
            betadine.namaObat,
            4,
            WaktuUji.AWAL_HARI_INI + 200 * WaktuUji.SATU_HARI_MILLIS,
        )
        db.obatDao().updateObat(
            plester.id,
            plester.namaObat,
            plester.stok,
            WaktuUji.AWAL_HARI_INI + 10 * WaktuUji.SATU_HARI_MILLIS,
        )

        val state = buatViewModel().uiState.tunggu { !it.memuat && it.daftar.size == 5 }

        assertEquals(
            listOf(
                StatusObat.KADALUWARSA,
                StatusObat.STOK_KRITIS,
                StatusObat.HAMPIR_KADALUWARSA,
                StatusObat.AMAN,
                StatusObat.AMAN,
            ),
            state.daftar.map { it.status },
        )
        assertEquals("Antasida", state.daftar.first().obat.namaObat)
        assertTrue(state.adaMasalah)
    }

    @Test
    fun `status obat dihitung ulang setelah detak dan jam bergeser`() = runBlocking {
        val plester = obat("Plester")
        // Kadaluwarsa 10 hari lagi: awalnya "hampir kadaluwarsa".
        db.obatDao().updateObat(
            plester.id,
            plester.namaObat,
            plester.stok,
            WaktuUji.AWAL_HARI_INI + 10 * WaktuUji.SATU_HARI_MILLIS,
        )
        val viewModel = buatViewModel()
        val awal = viewModel.uiState.tunggu { it.daftar.any { item -> item.obat.id == plester.id } }
        assertEquals(
            StatusObat.HAMPIR_KADALUWARSA,
            awal.daftar.first { it.obat.id == plester.id }.status,
        )

        jam.maju(20 * WaktuUji.SATU_HARI_MILLIS)
        detak.emit(Unit)

        val setelah = viewModel.uiState.tunggu {
            it.daftar.any { item -> item.obat.id == plester.id && item.status == StatusObat.KADALUWARSA }
        }
        assertEquals(
            StatusObat.KADALUWARSA,
            setelah.daftar.first { it.obat.id == plester.id }.status,
        )
    }

    @Test
    fun `tambah obat tanpa nama ditolak`() = runBlocking {
        val viewModel = buatViewModel()

        viewModel.tambahObat("   ", stok = 5, expiredDate = WaktuUji.SEKARANG)

        assertEquals(UiText.Teks(R.string.pesan_nama_obat_wajib), viewModel.uiState.value.kesalahan)
        assertEquals(5, jumlahObat())
    }

    @Test
    fun `tambah obat baru menambahkan baris dan menghapus kesalahan`() = runBlocking {
        val viewModel = buatViewModel()
        viewModel.tambahObat("", stok = 1, expiredDate = WaktuUji.SEKARANG)

        viewModel.bersihkanKesalahan()
        viewModel.tambahObat("Vitamin C", stok = 7, expiredDate = WaktuUji.SEKARANG)

        val state = viewModel.uiState.tunggu { it.daftar.any { item -> item.obat.namaObat == "Vitamin C" } }
        assertNull(state.kesalahan)
        assertEquals(6, jumlahObat())
    }

    @Test
    fun `restock positif menambah stok`() = runBlocking {
        val plester = obat("Plester")
        val viewModel = buatViewModel()

        viewModel.restock(plester.id, 5)

        val state = viewModel.uiState.tunggu {
            it.daftar.any { item -> item.obat.id == plester.id && item.obat.stok == 15 }
        }
        assertNull(state.kesalahan)
    }

    @Test
    fun `restock yang membuat stok negatif dilaporkan sebagai kesalahan stok`() = runBlocking {
        val plester = obat("Plester")
        val viewModel = buatViewModel()

        viewModel.restock(plester.id, -50)

        val state = viewModel.uiState.tunggu { it.kesalahan != null }
        assertEquals(UiText.Teks(R.string.pesan_stok_tidak_valid), state.kesalahan)
        assertEquals(10, db.obatDao().getById(plester.id)!!.stok)
    }

    @Test
    fun `edit obat dengan stok negatif ditolak`() = runBlocking {
        val antasida = obat("Antasida")
        val viewModel = buatViewModel()

        viewModel.editObat(antasida.id, antasida.namaObat, -1, antasida.expiredDate)

        assertEquals(
            UiText.Teks(R.string.pesan_stok_tidak_valid),
            viewModel.uiState.tunggu { it.kesalahan != null }.kesalahan,
        )
        assertEquals(10, db.obatDao().getById(antasida.id)!!.stok)
    }

    @Test
    fun `edit obat mengubah nama dan stok`() = runBlocking {
        val antasida = obat("Antasida")
        val viewModel = buatViewModel()

        viewModel.editObat(antasida.id, "Antasida Tablet", 20, antasida.expiredDate)

        val state = viewModel.uiState.tunggu {
            it.daftar.any { item -> item.obat.id == antasida.id && item.obat.namaObat == "Antasida Tablet" }
        }
        assertNull(state.kesalahan)
        assertEquals(20, db.obatDao().getById(antasida.id)!!.stok)
    }

    @Test
    fun `hapus obat mengurangi daftar`() = runBlocking {
        val plester = obat("Plester")
        val viewModel = buatViewModel()
        viewModel.uiState.tunggu { !it.memuat }

        viewModel.hapusObat(plester.id)

        viewModel.uiState.tunggu { it.daftar.none { item -> item.obat.id == plester.id } }
        assertEquals(4, jumlahObat())
    }

    @Test
    fun `hapus obat yang tidak ada dilaporkan sebagai obat tidak ditemukan`() = runBlocking {
        val viewModel = buatViewModel()

        viewModel.hapusObat(999)

        assertEquals(
            UiText.Teks(R.string.pesan_obat_tidak_ditemukan),
            viewModel.uiState.tunggu { it.kesalahan != null }.kesalahan,
        )
    }

    private suspend fun obat(nama: String): Obat =
        repository.getSemuaObat().first().first { it.namaObat == nama }

    private suspend fun jumlahObat(): Int = repository.getSemuaObat().first().size
}

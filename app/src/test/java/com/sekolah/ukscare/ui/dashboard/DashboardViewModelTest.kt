package com.sekolah.ukscare.ui.dashboard

import androidx.test.core.app.ApplicationProvider
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.WaktuUji
import com.sekolah.ukscare.data.buatDatabaseInMemory
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.data.model.Pasien
import com.sekolah.ukscare.data.repository.HasilSimpanPasien
import com.sekolah.ukscare.data.repository.MasukanPasien
import com.sekolah.ukscare.data.repository.ObatRepository
import com.sekolah.ukscare.data.repository.PasienRepository
import com.sekolah.ukscare.ui.JamUji
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.ui.detakUji
import com.sekolah.ukscare.ui.tunggu
import com.sekolah.ukscare.util.Constants
import com.sekolah.ukscare.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

/** Ringkasan, durasi istirahat dari jam inject + detak, pencarian riwayat, dan tab. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

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

    private fun buatViewModel(jedaHentikanMillis: Long = 5_000L) = DashboardViewModel(
        pasienRepository = pasienRepository,
        obatRepository = obatRepository,
        sumberWaktu = jam,
        zone = WaktuUji.ZONE,
        detak = detak,
        jedaHentikanMillis = jedaHentikanMillis,
    )

    @Test
    fun `detak hanya dikoleksi sekali walau dipakai ringkasan dan daftar aktif`() = runBlocking {
        val viewModel = buatViewModel()
        assertEquals(0, detak.subscriptionCount.value)

        val kolektor = launch { viewModel.uiState.collect {} }
        detak.subscriptionCount.tunggu { it > 0 }

        // Sebelum diperbaiki setiap aliran mengoleksi detak sendiri, sehingga ada dua timer.
        assertEquals(1, detak.subscriptionCount.value)
        kolektor.cancel()
    }

    @Test
    fun `detak berhenti saat tidak ada lagi yang mengoleksi state`() = runBlocking {
        // Jeda hentikan 0 ms agar penghentian detak dapat diuji tanpa menunggu 5 detik.
        val viewModel = buatViewModel(jedaHentikanMillis = 0L)

        val kolektor = launch { viewModel.uiState.collect {} }
        detak.subscriptionCount.tunggu { it > 0 }

        // Seperti aplikasi yang masuk ke belakang: berhenti mengoleksi state.
        kolektor.cancel()

        // Timer detak ikut berhenti karena stateIn/shareIn memakai WhileSubscribed.
        assertEquals(0, detak.subscriptionCount.tunggu { it == 0 })
    }

    @Test
    fun `ringkasan menghitung pasien dirawat kunjungan hari ini dan obat bermasalah`() = runBlocking {
        val antasida = obat("Antasida")
        val betadine = obat("Betadine")
        // Satu kadaluwarsa dan satu stok kritis: dua obat bermasalah.
        db.obatDao().updateObat(
            antasida.id,
            antasida.namaObat,
            antasida.stok,
            WaktuUji.AWAL_HARI_INI - WaktuUji.SATU_HARI_MILLIS,
        )
        db.obatDao().updateObat(betadine.id, betadine.namaObat, 4, betadine.expiredDate)
        simpanPasien("Andi", WaktuUji.SEKARANG)
        // Kunjungan kemarin yang sudah selesai: tidak dihitung di kedua angka.
        val kemarin = simpanPasien("Budi", WaktuUji.SEKARANG - WaktuUji.SATU_HARI_MILLIS)
        pasienRepository.selesaikanIstirahat(kemarin, WaktuUji.SEKARANG - WaktuUji.SATU_HARI_MILLIS + 1_000)

        val state = buatViewModel().uiState.tunggu { !it.memuat && it.ringkasan.sedangDirawat == 1 }

        assertEquals(1, state.ringkasan.sedangDirawat)
        assertEquals(1, state.ringkasan.kunjunganHariIni)
        assertEquals(2, state.ringkasan.obatBermasalah)
        assertTrue(state.adaObatBermasalah)
        assertEquals(listOf("Andi"), state.daftarAktif.map { it.pasien.namaSiswa })
        assertEquals(listOf("Budi"), state.daftarRiwayat.map { it.pasien.namaSiswa })
    }

    @Test
    fun `durasi istirahat memakai jam inject dan baru berubah setelah detak`() = runBlocking {
        simpanPasien("Andi", WaktuUji.SEKARANG)
        val viewModel = buatViewModel()
        val awal = viewModel.uiState.tunggu { it.daftarAktif.isNotEmpty() }
        assertEquals(0L, awal.daftarAktif.single().durasiIstirahatMillis)

        jam.maju(45 * 60_000L)
        // Tanpa detak, state belum berubah.
        assertEquals(0L, viewModel.uiState.value.daftarAktif.single().durasiIstirahatMillis)

        detak.emit(Unit)

        val setelah = viewModel.uiState.tunggu {
            it.daftarAktif.singleOrNull()?.durasiIstirahatMillis == 45 * 60_000L
        }
        assertEquals(45 * 60_000L, setelah.daftarAktif.single().durasiIstirahatMillis)
    }

    @Test
    fun `waktu masuk diformat untuk kartu pasien`() = runBlocking {
        simpanPasien("Andi", WaktuUji.SEKARANG)

        val state = buatViewModel().uiState.tunggu { it.daftarAktif.isNotEmpty() }

        assertEquals(
            DateUtils.formatTanggalJam(WaktuUji.SEKARANG, WaktuUji.ZONE),
            state.daftarAktif.single().teksWaktuMasuk,
        )
    }

    @Test
    fun `pencarian hanya menyaring riwayat`() = runBlocking {
        simpanPasien("Andi", WaktuUji.SEKARANG)
        simpanPasien("Budi", WaktuUji.SEKARANG)
        val citra = simpanPasien("Citra", WaktuUji.SEKARANG)
        pasienRepository.selesaikanIstirahat(citra, WaktuUji.SEKARANG + 60_000)
        val viewModel = buatViewModel()
        viewModel.uiState.tunggu { it.daftarAktif.size == 2 && it.daftarRiwayat.size == 1 }

        viewModel.setQuery("Citra")

        val cocok = viewModel.uiState.tunggu {
            it.kueriPencarian == "Citra" && it.daftarRiwayat.singleOrNull() != null
        }
        assertEquals("Citra", cocok.daftarRiwayat.single().pasien.namaSiswa)
        assertEquals(2, cocok.daftarAktif.size)

        viewModel.setQuery("Budi")

        val tanpaRiwayat = viewModel.uiState.tunggu {
            it.kueriPencarian == "Budi" && it.daftarRiwayat.isEmpty()
        }
        // Nama di daftar aktif tidak boleh terpengaruh pencarian riwayat.
        assertEquals(2, tanpaRiwayat.daftarAktif.size)
    }

    @Test
    fun `memilih tab mengubah state`() = runBlocking {
        val viewModel = buatViewModel()
        viewModel.uiState.tunggu { !it.memuat }
        assertEquals(TabDashboard.SEDANG_DIRAWAT, viewModel.uiState.value.tabTerpilih)

        viewModel.selectTab(TabDashboard.RIWAYAT)

        val state = viewModel.uiState.tunggu { it.tabTerpilih == TabDashboard.RIWAYAT }
        assertEquals(TabDashboard.RIWAYAT, state.tabTerpilih)
    }

    @Test
    fun `selesai istirahat memindahkan pasien ke riwayat dengan waktu keluar dari jam inject`() = runBlocking {
        val id = simpanPasien("Andi", WaktuUji.SEKARANG)
        val viewModel = buatViewModel()
        viewModel.uiState.tunggu { it.daftarAktif.size == 1 }
        jam.maju(60 * 60_000L)

        viewModel.selesaikanIstirahat(id)

        val state = viewModel.uiState.tunggu { it.daftarRiwayat.size == 1 }
        assertTrue(state.daftarAktif.isEmpty())
        val pasien = state.daftarRiwayat.single().pasien
        assertEquals(Pasien.STATUS_SELESAI, pasien.status)
        assertEquals(WaktuUji.SEKARANG + 60 * 60_000L, pasien.waktuKeluar)
        assertNull(state.kesalahan)
    }

    @Test
    fun `selesai istirahat untuk pasien yang tidak ada dilaporkan`() = runBlocking {
        val viewModel = buatViewModel()
        viewModel.uiState.tunggu { !it.memuat }

        viewModel.selesaikanIstirahat(999)

        assertEquals(
            UiText.Teks(R.string.pesan_pasien_tidak_ditemukan),
            viewModel.uiState.tunggu { it.kesalahan != null }.kesalahan,
        )
        viewModel.bersihkanKesalahan()
        // stateIn menyimpan nilai terakhir, jadi tunggu emisi berikutnya agar tidak
        // membaca nilai cache (bisa balapan dengan coroutine pembagi state).
        assertNull(viewModel.uiState.tunggu { it.kesalahan == null }.kesalahan)
    }

    private suspend fun simpanPasien(nama: String, waktuMasuk: Long): Int {
        val hasil = pasienRepository.simpanPasien(
            masukan = MasukanPasien(
                namaSiswa = nama,
                kelas = "VII-A",
                jenisKelamin = Constants.JENIS_KELAMIN_LAKI_LAKI,
                keluhan = "Pusing",
                tindakan = "Istirahat",
            ),
            sekarang = waktuMasuk,
            awalHariIni = DateUtils.startOfDay(waktuMasuk, WaktuUji.ZONE),
        )
        return (hasil as HasilSimpanPasien.Berhasil).idPasien
    }

    private suspend fun obat(nama: String): Obat =
        obatRepository.getSemuaObat().first().first { it.namaObat == nama }
}

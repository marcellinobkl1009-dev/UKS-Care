package com.sekolah.ukscare.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.repository.ObatRepository
import com.sekolah.ukscare.data.repository.PasienRepository
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.util.DateUtils
import com.sekolah.ukscare.util.ObatStatus
import com.sekolah.ukscare.util.StatusObat
import com.sekolah.ukscare.util.SumberWaktu
import com.sekolah.ukscare.util.SumberWaktuSistem
import com.sekolah.ukscare.util.Ticker
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel Dashboard (AGENTS.md 6.1).
 *
 * Durasi istirahat dihitung dari [sumberWaktu] yang di-inject dan diperbarui setiap
 * [detak], sehingga UI cukup membaca [PasienAktifUi.durasiIstirahatMillis] dan
 * pengujian dapat menggeser jam tanpa menunggu waktu nyata.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val pasienRepository: PasienRepository,
    private val obatRepository: ObatRepository,
    private val sumberWaktu: SumberWaktu = SumberWaktuSistem,
    private val zone: ZoneId = ZoneId.systemDefault(),
    detak: Flow<Unit> = Ticker.detak(),
) : ViewModel() {

    private val kueriPencarian = MutableStateFlow("")
    private val tabTerpilih = MutableStateFlow(TabDashboard.SEDANG_DIRAWAT)
    private val kesalahanMuat = MutableStateFlow<UiText?>(null)
    private val pesanAksi = MutableStateFlow<UiText?>(null)

    /** Satu-satunya sumber "waktu sekarang": jam inject + detak berkala. */
    private val sekarang: Flow<Long> = detak
        .map { sumberWaktu.sekarang() }
        .onStart { emit(sumberWaktu.sekarang()) }

    private val aliranAktif: Flow<List<PasienAktifUi>> = combine(
        pasienRepository.getPasienAktif(),
        sekarang,
    ) { daftar, waktu ->
        daftar.map { pasien ->
            PasienAktifUi(
                pasien = pasien,
                durasiIstirahatMillis = (waktu - pasien.waktuMasuk).coerceAtLeast(0L),
                teksWaktuMasuk = DateUtils.formatTanggalJam(pasien.waktuMasuk, zone),
            )
        }
    }.catch {
        laporkanGagalMuat()
        emit(emptyList())
    }

    /** Pencarian hanya memengaruhi tab Riwayat. */
    private val aliranRiwayat: Flow<List<PasienRiwayatUi>> = kueriPencarian
        .flatMapLatest { kueri -> pasienRepository.cariRiwayat(kueri) }
        .map { daftar ->
            daftar.map { pasien ->
                PasienRiwayatUi(
                    pasien = pasien,
                    teksWaktuMasuk = DateUtils.formatTanggalJam(pasien.waktuMasuk, zone),
                    teksWaktuKeluar = pasien.waktuKeluar?.let { DateUtils.formatTanggalJam(it, zone) },
                )
            }
        }
        .catch {
            laporkanGagalMuat()
            emit(emptyList())
        }

    private val aliranRingkasan: Flow<RingkasanDashboard> = combine(
        pasienRepository.countPasienAktif(),
        sekarang.flatMapLatest { waktu ->
            pasienRepository.countKunjunganHariIni(
                awalHariIni = DateUtils.startOfDay(waktu, zone),
                awalHariBesok = DateUtils.startOfNextDay(waktu, zone),
            )
        },
        obatRepository.getSemuaObat(),
        sekarang,
    ) { aktif, kunjunganHariIni, daftarObat, waktu ->
        RingkasanDashboard(
            sedangDirawat = aktif,
            kunjunganHariIni = kunjunganHariIni,
            obatBermasalah = daftarObat.count {
                ObatStatus.dari(it, waktu, zone) != StatusObat.AMAN
            },
        )
    }.catch {
        laporkanGagalMuat()
        emit(RingkasanDashboard())
    }

    private val isi: Flow<IsiDashboard> = combine(
        aliranRingkasan,
        aliranAktif,
        aliranRiwayat,
        tabTerpilih,
        kueriPencarian,
    ) { ringkasan, aktif, riwayat, tab, kueri ->
        IsiDashboard(
            ringkasan = ringkasan,
            aktif = aktif,
            riwayat = riwayat,
            tab = tab,
            kueri = kueri,
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        isi,
        kesalahanMuat,
        pesanAksi,
    ) { isi, muat, aksi ->
        DashboardUiState(
            memuat = false,
            ringkasan = isi.ringkasan,
            tabTerpilih = isi.tab,
            daftarAktif = isi.aktif,
            daftarRiwayat = isi.riwayat,
            kueriPencarian = isi.kueri,
            kesalahan = aksi ?: muat,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DashboardUiState(),
    )

    fun setQuery(kueri: String) {
        kueriPencarian.value = kueri
    }

    fun selectTab(tab: TabDashboard) {
        tabTerpilih.value = tab
    }

    fun selesaikanIstirahat(id: Int) {
        viewModelScope.launch {
            try {
                val berhasil = pasienRepository.selesaikanIstirahat(id, sumberWaktu.sekarang())
                if (!berhasil) {
                    pesanAksi.value = UiText.Teks(R.string.pesan_pasien_tidak_ditemukan)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pesanAksi.value = UiText.Teks(R.string.pesan_gagal_selesaikan_istirahat)
            }
        }
    }

    /** Dipanggil UI setelah Snackbar kesalahan ditutup. */
    fun bersihkanKesalahan() {
        kesalahanMuat.value = null
        pesanAksi.value = null
    }

    private fun laporkanGagalMuat() {
        kesalahanMuat.value = UiText.Teks(R.string.pesan_gagal_memuat_data)
    }

    private data class IsiDashboard(
        val ringkasan: RingkasanDashboard,
        val aktif: List<PasienAktifUi>,
        val riwayat: List<PasienRiwayatUi>,
        val tab: TabDashboard,
        val kueri: String,
    )

    private companion object {
        /** Data tetap hidup sebentar saat rotasi layar agar tidak memuat ulang. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

package com.sekolah.ukscare.ui.obat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.repository.ObatRepository
import com.sekolah.ukscare.data.repository.ObatTidakDitemukanException
import com.sekolah.ukscare.data.repository.StokTidakValidException
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.util.ObatStatus
import com.sekolah.ukscare.util.SumberWaktu
import com.sekolah.ukscare.util.SumberWaktuSistem
import com.sekolah.ukscare.util.Ticker
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel Kelola Obat (AGENTS.md 6.4).
 * Status obat bergantung pada tanggal, jadi dihitung ulang setiap [detak]
 * dengan jam yang di-inject.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObatViewModel(
    private val obatRepository: ObatRepository,
    private val sumberWaktu: SumberWaktu = SumberWaktuSistem,
    private val zone: ZoneId = ZoneId.systemDefault(),
    detak: Flow<Unit> = Ticker.detak(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObatUiState())
    val uiState: StateFlow<ObatUiState> = _uiState.asStateFlow()

    private val sekarang: Flow<Long> = detak
        .map { sumberWaktu.sekarang() }
        .onStart { emit(sumberWaktu.sekarang()) }

    init {
        viewModelScope.launch {
            combine(obatRepository.getSemuaObat(), sekarang) { daftar, waktu ->
                ObatStatus.urutkan(daftar, waktu, zone).map { obat ->
                    ObatUiItem(obat = obat, status = ObatStatus.dari(obat, waktu, zone))
                }
            }
                .catch { _uiState.update { it.copy(memuat = false, kesalahan = UiText.Teks(R.string.pesan_gagal_memuat_data)) } }
                .collect { daftar ->
                    _uiState.update { it.copy(memuat = false, daftar = daftar) }
                }
        }
    }

    fun tambahObat(namaObat: String, stok: Int, expiredDate: Long) {
        if (namaObat.isBlank()) {
            tampilkanKesalahan(UiText.Teks(R.string.pesan_nama_obat_wajib))
            return
        }
        jalankanAksi(UiText.Teks(R.string.pesan_gagal_menyimpan_obat)) {
            obatRepository.tambahObat(namaObat, stok, expiredDate).map { }
        }
    }

    fun editObat(id: Int, namaObat: String, stok: Int, expiredDate: Long) {
        if (namaObat.isBlank()) {
            tampilkanKesalahan(UiText.Teks(R.string.pesan_nama_obat_wajib))
            return
        }
        jalankanAksi(UiText.Teks(R.string.pesan_gagal_menyimpan_obat)) {
            obatRepository.perbaruiObat(id, namaObat, stok, expiredDate)
        }
    }

    /** Restock cepat; [jumlah] negatif berarti mengurangi stok. */
    fun restock(id: Int, jumlah: Int) {
        jalankanAksi(UiText.Teks(R.string.pesan_gagal_menyimpan_obat)) {
            obatRepository.tambahStok(id, jumlah)
        }
    }

    fun hapusObat(id: Int) {
        jalankanAksi(UiText.Teks(R.string.pesan_gagal_menyimpan_obat)) {
            obatRepository.hapusObat(id)
        }
    }

    fun bersihkanKesalahan() {
        _uiState.update { it.copy(kesalahan = null) }
    }

    private fun jalankanAksi(pesanUmum: UiText, aksi: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            val hasil = try {
                aksi()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                tampilkanKesalahan(pesanUmum)
                return@launch
            }
            val gagal = hasil.exceptionOrNull()
            if (gagal != null) tampilkanKesalahan(pesanKesalahan(gagal, pesanUmum))
        }
    }

    private fun pesanKesalahan(gagal: Throwable, pesanUmum: UiText): UiText = when (gagal) {
        is StokTidakValidException -> UiText.Teks(R.string.pesan_stok_tidak_valid)
        is ObatTidakDitemukanException -> UiText.Teks(R.string.pesan_obat_tidak_ditemukan)
        else -> pesanUmum
    }

    private fun tampilkanKesalahan(pesan: UiText) {
        _uiState.update { it.copy(kesalahan = pesan) }
    }
}

package com.sekolah.ukscare.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.data.repository.AlasanStokGagal
import com.sekolah.ukscare.data.repository.HasilSimpanPasien
import com.sekolah.ukscare.data.repository.MasukanPasien
import com.sekolah.ukscare.data.repository.ObatRepository
import com.sekolah.ukscare.data.repository.PasienRepository
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.util.Constants
import com.sekolah.ukscare.util.DateUtils
import com.sekolah.ukscare.util.PesanGenerator
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel form pasien (AGENTS.md 6.2).
 *
 * Waktu masuk dan batas "hari ini" berasal dari [sumberWaktu] agar dapat diuji,
 * dan daftar obat yang dapat diberikan ikut disegarkan setiap [detak]
 * karena bergantung pada tanggal kadaluwarsa.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormPasienViewModel(
    private val pasienRepository: PasienRepository,
    private val obatRepository: ObatRepository,
    private val sumberWaktu: SumberWaktu = SumberWaktuSistem,
    private val zone: ZoneId = ZoneId.systemDefault(),
    detak: Flow<Unit> = Ticker.detak(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(FormPasienUiState())
    val uiState: StateFlow<FormPasienUiState> = _uiState.asStateFlow()

    /** Penjaga tambahan: sekali tersimpan, form tidak boleh menulis pasien kedua. */
    private var pasienSudahDisimpan = false

    private val sekarang: Flow<Long> = detak
        .map { sumberWaktu.sekarang() }
        .onStart { emit(sumberWaktu.sekarang()) }

    init {
        viewModelScope.launch {
            sekarang
                .flatMapLatest { waktu ->
                    obatRepository.getObatDapatDiberikan(DateUtils.startOfDay(waktu, zone))
                }
                .catch {
                    _uiState.update { it.copy(errorSimpan = UiText.Teks(R.string.pesan_gagal_memuat_obat)) }
                }
                .collect { daftar -> _uiState.update { it.copy(daftarObat = daftar) } }
        }
    }

    fun ubahNama(nilai: String) {
        _uiState.update { it.copy(namaSiswa = nilai, kesalahan = it.kesalahan.copy(namaSiswa = null)) }
    }

    fun pilihKelas(kelas: String) {
        _uiState.update { it.copy(kelas = kelas, kesalahan = it.kesalahan.copy(kelas = null)) }
    }

    fun pilihJenisKelamin(jenisKelamin: String) {
        _uiState.update {
            it.copy(
                jenisKelamin = jenisKelamin,
                kesalahan = it.kesalahan.copy(jenisKelamin = null),
            )
        }
    }

    fun alihKeluhan(pilihan: String) {
        _uiState.update {
            it.copy(
                keluhan = alih(pilihan, it.keluhan),
                kesalahan = it.kesalahan.copy(keluhan = null),
            )
        }
    }

    fun alihTindakan(pilihan: String) {
        _uiState.update { state ->
            val tindakanBaru = alih(pilihan, state.tindakan)
            // Pilihan obat dibuang begitu tindakan "Diberi Obat" tidak lagi dipilih.
            val masihButuhObat = tindakanBaru.contains(Constants.DIBERI_OBAT)
            state.copy(
                tindakan = tindakanBaru,
                idObatTerpilih = if (masihButuhObat) state.idObatTerpilih else null,
                kesalahan = state.kesalahan.copy(
                    tindakan = null,
                    obat = if (masihButuhObat) state.kesalahan.obat else null,
                ),
            )
        }
    }

    fun pilihObat(idObat: Int?) {
        _uiState.update {
            it.copy(idObatTerpilih = idObat, kesalahan = it.kesalahan.copy(obat = null))
        }
    }

    /**
     * Menyimpan pasien. Validasi dijalankan lebih dulu; bila gagal tidak ada
     * penulisan ke database. Selama [FormPasienUiState.sedangMenyimpan] bernilai
     * true, pemanggilan berikutnya diabaikan sehingga tidak ada pasien ganda.
     */
    fun simpan() {
        val state = _uiState.value
        if (state.sedangMenyimpan || pasienSudahDisimpan) return

        val kesalahan = ValidasiFormPasien.validasi(
            namaSiswa = state.namaSiswa,
            kelas = state.kelas,
            jenisKelamin = state.jenisKelamin,
            keluhan = state.keluhan,
            tindakan = state.tindakan,
            idObatTerpilih = state.idObatTerpilih,
        )
        if (kesalahan.adaKesalahan) {
            _uiState.update { it.copy(kesalahan = kesalahan) }
            return
        }

        _uiState.update { it.copy(sedangMenyimpan = true, errorSimpan = null) }
        viewModelScope.launch { prosesSimpan(state) }
    }

    private suspend fun prosesSimpan(state: FormPasienUiState) {
        val waktuMasuk = sumberWaktu.sekarang()
        val keluhan = gabungSesuaiUrutan(state.keluhan, state.daftarKeluhan)
        val tindakan = gabungSesuaiUrutan(state.tindakan, state.daftarTindakan)

        val obat = try {
            cariObatTerpilih(state)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            gagalkanSimpan(UiText.Teks(R.string.pesan_gagal_menyimpan_pasien))
            return
        }
        // Bila "Diberi Obat" dipilih tetapi barisnya sudah hilang, simpan dibatalkan
        // agar tidak ada pasien tanpa obat tercatat.
        if (state.membutuhkanObat && obat == null) {
            gagalkanSimpan(UiText.Teks(R.string.pesan_obat_tidak_ditemukan))
            return
        }

        val hasil = try {
            pasienRepository.simpanPasien(
                masukan = MasukanPasien(
                    namaSiswa = state.namaSiswa,
                    kelas = state.kelas,
                    jenisKelamin = state.jenisKelamin,
                    keluhan = keluhan,
                    tindakan = tindakan,
                    obat = obat,
                ),
                sekarang = waktuMasuk,
                awalHariIni = DateUtils.startOfDay(waktuMasuk, zone),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            gagalkanSimpan(UiText.Teks(R.string.pesan_gagal_menyimpan_pasien))
            return
        }

        when (hasil) {
            is HasilSimpanPasien.Berhasil -> {
                pasienSudahDisimpan = true
                val draf = PesanGenerator.buatPesan(
                    nama = state.namaSiswa.trim(),
                    kelas = state.kelas,
                    keluhan = keluhan,
                    tindakan = tindakan,
                    obat = obat?.namaObat,
                    waktuMasuk = DateUtils.formatTanggalJam(waktuMasuk, zone),
                )
                _uiState.update { it.copy(sedangMenyimpan = false, drafPesan = draf) }
            }

            is HasilSimpanPasien.GagalObat -> {
                _uiState.update {
                    it.copy(sedangMenyimpan = false, errorSimpan = pesanGagalObat(hasil.alasan))
                }
            }
        }
    }

    /**
     * Obat diambil dari daftar pilihan bila masih ada, atau dibaca ulang dari repository
     * supaya alasan penolakan berasal dari repository (stok habis / kadaluwarsa),
     * bukan dari daftar UI yang mungkin sudah usang.
     */
    private suspend fun cariObatTerpilih(state: FormPasienUiState): Obat? {
        val id = state.idObatTerpilih ?: return null
        return state.daftarObat.firstOrNull { it.id == id } ?: obatRepository.getObat(id)
    }

    private fun gagalkanSimpan(pesan: UiText) {
        _uiState.update { it.copy(sedangMenyimpan = false, errorSimpan = pesan) }
    }

    private fun pesanGagalObat(alasan: AlasanStokGagal): UiText = when (alasan) {
        AlasanStokGagal.STOK_HABIS -> UiText.Teks(R.string.pesan_stok_obat_habis)
        AlasanStokGagal.OBAT_KADALUWARSA -> UiText.Teks(R.string.pesan_obat_kadaluwarsa)
        AlasanStokGagal.OBAT_TIDAK_DITEMUKAN -> UiText.Teks(R.string.pesan_obat_tidak_ditemukan)
    }

    private fun alih(pilihan: String, terpilih: Set<String>): Set<String> =
        if (pilihan in terpilih) terpilih - pilihan else terpilih + pilihan

    /** Urutan teks mengikuti urutan pilihan di form supaya isi kolom `keluhan`/`tindakan` konsisten. */
    private fun gabungSesuaiUrutan(terpilih: Set<String>, daftar: List<String>): String =
        Constants.gabungPilihan(daftar.filter { it in terpilih })

    fun bersihkanErrorSimpan() {
        _uiState.update { it.copy(errorSimpan = null) }
    }
}

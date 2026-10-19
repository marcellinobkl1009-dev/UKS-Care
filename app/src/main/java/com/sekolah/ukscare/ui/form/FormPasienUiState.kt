package com.sekolah.ukscare.ui.form

import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.util.Constants

/** Kesalahan per field form pasien (ditampilkan inline, AGENTS.md 6.2). */
data class KesalahanForm(
    val namaSiswa: UiText? = null,
    val kelas: UiText? = null,
    val jenisKelamin: UiText? = null,
    val keluhan: UiText? = null,
    val tindakan: UiText? = null,
    val obat: UiText? = null,
) {
    val adaKesalahan: Boolean
        get() = listOf(namaSiswa, kelas, jenisKelamin, keluhan, tindakan, obat).any { it != null }
}

/**
 * State form pasien. Tanpa logika; aturan validasi ada di [ValidasiFormPasien]
 * dan aksi ada di [FormPasienViewModel].
 */
data class FormPasienUiState(
    val namaSiswa: String = "",
    val kelas: String = "",
    val jenisKelamin: String = "",
    val keluhan: Set<String> = emptySet(),
    val tindakan: Set<String> = emptySet(),
    /** Id obat terpilih; hanya dipakai bila [membutuhkanObat] bernilai true. */
    val idObatTerpilih: Int? = null,
    val daftarKelas: List<String> = Constants.DAFTAR_KELAS,
    val daftarJenisKelamin: List<String> = Constants.PILIHAN_JENIS_KELAMIN,
    val daftarKeluhan: List<String> = Constants.PILIHAN_KELUHAN,
    val daftarTindakan: List<String> = Constants.PILIHAN_TINDAKAN,
    /** Hanya obat yang stoknya ada dan belum kadaluwarsa. */
    val daftarObat: List<Obat> = emptyList(),
    val kesalahan: KesalahanForm = KesalahanForm(),
    val sedangMenyimpan: Boolean = false,
    /** Draf surat izin setelah pasien berhasil disimpan. */
    val drafPesan: String? = null,
    val errorSimpan: UiText? = null,
) {
    val membutuhkanObat: Boolean get() = tindakan.contains(Constants.DIBERI_OBAT)

    val dapatDisimpan: Boolean get() = !sedangMenyimpan
}

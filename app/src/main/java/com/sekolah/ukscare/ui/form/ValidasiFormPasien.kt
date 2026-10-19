package com.sekolah.ukscare.ui.form

import com.sekolah.ukscare.R
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.util.Constants

/**
 * Aturan validasi form pasien (AGENTS.md 6.2). Fungsi murni supaya dapat
 * diuji tanpa Android maupun coroutine.
 */
object ValidasiFormPasien {

    fun validasi(
        namaSiswa: String,
        kelas: String,
        jenisKelamin: String,
        keluhan: Collection<String>,
        tindakan: Collection<String>,
        idObatTerpilih: Int?,
    ): KesalahanForm = KesalahanForm(
        namaSiswa = salah(namaSiswa.isBlank(), R.string.validasi_nama_wajib),
        kelas = salah(kelas.isBlank(), R.string.validasi_kelas_wajib),
        jenisKelamin = salah(jenisKelamin.isBlank(), R.string.validasi_jenis_kelamin_wajib),
        keluhan = salah(keluhan.isEmpty(), R.string.validasi_keluhan_wajib),
        tindakan = salah(tindakan.isEmpty(), R.string.validasi_tindakan_wajib),
        obat = salah(
            kondisi = tindakan.contains(Constants.DIBERI_OBAT) && idObatTerpilih == null,
            idPesan = R.string.validasi_obat_wajib,
        ),
    )

    private fun salah(kondisi: Boolean, idPesan: Int): UiText? =
        if (kondisi) UiText.Teks(idPesan) else null
}

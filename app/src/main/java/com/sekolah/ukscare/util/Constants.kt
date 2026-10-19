package com.sekolah.ukscare.util

/**
 * Daftar pilihan yang dipakai form dan disimpan ke database.
 * Ubah di sini bila struktur kelas atau pilihan di sekolah berbeda (lihat README).
 */
object Constants {

    const val JENIS_KELAMIN_LAKI_LAKI = "Laki-laki"
    const val JENIS_KELAMIN_PEREMPUAN = "Perempuan"

    val PILIHAN_JENIS_KELAMIN: List<String> = listOf(
        JENIS_KELAMIN_LAKI_LAKI,
        JENIS_KELAMIN_PEREMPUAN,
    )

    /**
     * Daftar kelas default SMK (Sekolah Menengah Kejuruan): tingkat X, XI, dan XII
     * dengan kode jurusan AKL, MPLB, ULP, PM, TJKT, DKV, PPLG, dan AM.
     * Data pasien lama tetap berupa teks bebas, jadi mengganti daftar ini aman.
     */
    val DAFTAR_KELAS: List<String> = listOf(
        "X AKL 1", "X AKL 2", "X MPLB 1", "X MPLB 2", "X ULP 1", "X ULP 2",
        "X PM 1", "X PM 2", "X TJKT 1", "X TJKT 2", "X DKV 1", "X DKV 2",
        "X PPLG", "X AM",
        "XI AKL 1", "XI AKL 2", "XI MPLB 1", "XI MPLB 2", "XI ULP 1", "XI ULP 2",
        "XI PM 1", "XI PM 2", "XI TJKT 1", "XI TJKT 2", "XI DKV 1", "XI DKV 2",
        "XI PPLG", "XI AM",
        "XII AKL 1", "XII AKL 2", "XII MPLB 1", "XII MPLB 2", "XII ULP 1", "XII ULP 2",
        "XII PM 1", "XII PM 2", "XII TJKT 1", "XII TJKT 2", "XII DKV 1", "XII DKV 2",
        "XII PPLG", "XII AM",
    )

    val PILIHAN_KELUHAN: List<String> = listOf(
        "Pusing",
        "Demam",
        "Maag",
        "Luka",
        "Mual",
        "Sakit Kepala",
        "Lainnya",
    )

    val PILIHAN_TINDAKAN: List<String> = listOf(
        "Istirahat",
        DIBERI_OBAT,
        "Kompres",
        "Dibalut/P3K",
    )

    /** Tindakan yang memunculkan pilihan obat pada form. */
    const val DIBERI_OBAT = "Diberi Obat"

    /** Pemisah saat beberapa pilihan digabung ke satu kolom TEXT. */
    const val PEMISAH_PILIHAN = ", "

    fun gabungPilihan(pilihan: Collection<String>): String = pilihan.joinToString(PEMISAH_PILIHAN)

    fun pisahPilihan(teks: String): List<String> =
        teks.split(PEMISAH_PILIHAN).filter { it.isNotBlank() }
}

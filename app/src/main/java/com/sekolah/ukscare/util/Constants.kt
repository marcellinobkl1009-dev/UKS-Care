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

    /** Default SMP: VII-A sampai IX-E. */
    val DAFTAR_KELAS: List<String> = listOf(
        "VII-A", "VII-B", "VII-C", "VII-D", "VII-E",
        "VIII-A", "VIII-B", "VIII-C", "VIII-D", "VIII-E",
        "IX-A", "IX-B", "IX-C", "IX-D", "IX-E",
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

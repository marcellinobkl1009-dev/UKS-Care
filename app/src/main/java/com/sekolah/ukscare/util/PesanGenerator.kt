package com.sekolah.ukscare.util

import com.sekolah.ukscare.data.model.Pasien

/**
 * Penyusun draf surat izin istirahat UKS (lihat AGENTS.md 6.3).
 * Fungsi murni: seluruh nilai masuk lewat parameter, termasuk waktu yang sudah diformat.
 */
object PesanGenerator {

    /** Ditampilkan bila pasien tidak diberi obat. */
    const val OBAT_KOSONG = "-"

    fun buatPesan(
        nama: String,
        kelas: String,
        keluhan: String,
        tindakan: String,
        obat: String?,
        waktuMasuk: String,
    ): String {
        val obatTeks = obat?.trim().takeUnless { it.isNullOrEmpty() } ?: OBAT_KOSONG
        return """
            *SURAT IZIN ISTIRAHAT - UKS*

            Kepada Yth. Wali Kelas / Guru Piket
            Kelas: $kelas

            Dengan hormat, siswa berikut sedang berada di UKS:
            Nama: $nama
            Kelas: $kelas
            Keluhan: $keluhan
            Tindakan: $tindakan
            Obat diberikan: $obatTeks
            Waktu masuk: $waktuMasuk

            Mohon izin agar siswa tersebut dapat beristirahat di UKS. Terima kasih.

            Petugas UKS
        """.trimIndent()
    }

    /** Varian untuk mengirim ulang pesan dari kartu pasien di Dashboard. */
    fun buatPesan(pasien: Pasien, waktuMasuk: String): String = buatPesan(
        nama = pasien.namaSiswa,
        kelas = pasien.kelas,
        keluhan = pasien.keluhan,
        tindakan = pasien.tindakan,
        obat = pasien.obatDiberikan,
        waktuMasuk = waktuMasuk,
    )
}

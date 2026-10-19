package com.sekolah.ukscare.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu kunjungan pasien ke UKS. Semua waktu disimpan sebagai epoch millis.
 */
@Entity(tableName = "pasien")
data class Pasien(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "nama_siswa")
    val namaSiswa: String,

    @ColumnInfo(name = "kelas")
    val kelas: String,

    @ColumnInfo(name = "jenis_kelamin")
    val jenisKelamin: String,

    /** Keluhan terpilih, digabung dengan koma. */
    @ColumnInfo(name = "keluhan")
    val keluhan: String,

    /** Tindakan terpilih, digabung dengan koma. */
    @ColumnInfo(name = "tindakan")
    val tindakan: String,

    /** Nama obat yang diberikan, atau null bila tidak ada. */
    @ColumnInfo(name = "obat_diberikan")
    val obatDiberikan: String? = null,

    @ColumnInfo(name = "waktu_masuk")
    val waktuMasuk: Long,

    /** Null sampai tombol "Selesai Istirahat" ditekan. */
    @ColumnInfo(name = "waktu_keluar")
    val waktuKeluar: Long? = null,

    @ColumnInfo(name = "status")
    val status: String,
) {
    companion object {
        const val STATUS_AKTIF = "AKTIF"
        const val STATUS_SELESAI = "SELESAI"
    }
}

package com.sekolah.ukscare.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu jenis obat di gudang UKS.
 * [stok] tidak boleh negatif; [expiredDate] adalah epoch millis hari kadaluwarsa.
 */
@Entity(tableName = "obat")
data class Obat(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "nama_obat")
    val namaObat: String,

    @ColumnInfo(name = "stok")
    val stok: Int,

    @ColumnInfo(name = "expired_date")
    val expiredDate: Long,
)

package com.sekolah.ukscare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sekolah.ukscare.data.model.Obat
import kotlinx.coroutines.flow.Flow

/**
 * Akses tabel `obat`. Query daftar mengembalikan [Flow], operasi tulis `suspend`.
 * Semua query yang mengubah stok memakai klausa penjaga (guard) agar stok tidak pernah negatif.
 */
@Dao
interface ObatDao {

    @Query("SELECT * FROM obat ORDER BY nama_obat ASC")
    fun getAll(): Flow<List<Obat>>

    /** Obat yang boleh dipilih saat memberi obat: stok masih ada dan belum kadaluwarsa. */
    @Query("SELECT * FROM obat WHERE stok > 0 AND expired_date >= :awalHariIni ORDER BY nama_obat ASC")
    fun getDapatDiberikan(awalHariIni: Long): Flow<List<Obat>>

    @Query("SELECT * FROM obat WHERE id = :id")
    suspend fun getById(id: Int): Obat?

    @Insert
    suspend fun insert(obat: Obat): Long

    /** Mengubah data obat sekaligus; ditolak bila stok negatif. Mengembalikan jumlah baris berubah. */
    @Query(
        """
        UPDATE obat SET nama_obat = :namaObat, stok = :stok, expired_date = :expiredDate
        WHERE id = :id AND :stok >= 0
        """,
    )
    suspend fun updateObat(id: Int, namaObat: String, stok: Int, expiredDate: Long): Int

    /** Menimpa stok dengan nilai baru; ditolak bila negatif. */
    @Query("UPDATE obat SET stok = :stok WHERE id = :id AND :stok >= 0")
    suspend fun updateStok(id: Int, stok: Int): Int

    /** Restock cepat (+/-): ditolak bila hasilnya negatif. */
    @Query("UPDATE obat SET stok = stok + :jumlah WHERE id = :id AND stok + :jumlah >= 0")
    suspend fun tambahStokTerjaga(id: Int, jumlah: Int): Int

    /**
     * Penjaga utama pengurangan stok: hanya berkurang bila stok masih ada dan obat belum kadaluwarsa.
     * Mengembalikan jumlah baris yang berubah; 0 berarti pengurangan ditolak.
     */
    @Query("UPDATE obat SET stok = stok - 1 WHERE id = :id AND stok > 0 AND expired_date >= :awalHariIni")
    suspend fun kurangiStokTerjaga(id: Int, awalHariIni: Long): Int

    @Query("DELETE FROM obat WHERE id = :id")
    suspend fun deleteById(id: Int): Int
}

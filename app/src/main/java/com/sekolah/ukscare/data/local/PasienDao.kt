package com.sekolah.ukscare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sekolah.ukscare.data.model.Pasien
import kotlinx.coroutines.flow.Flow

/**
 * Akses tabel `pasien`. Query daftar mengembalikan [Flow], operasi tulis `suspend`.
 */
@Dao
interface PasienDao {

    @Query("SELECT * FROM pasien WHERE status = :status ORDER BY waktu_masuk DESC")
    fun getPasienByStatus(status: String): Flow<List<Pasien>>

    @Query(
        """
        SELECT * FROM pasien
        WHERE status = :status AND nama_siswa LIKE '%' || :nama || '%'
        ORDER BY waktu_masuk DESC
        """,
    )
    fun cariRiwayat(status: String, nama: String): Flow<List<Pasien>>

    @Query("SELECT COUNT(*) FROM pasien WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pasien WHERE waktu_masuk >= :awalHari AND waktu_masuk < :awalHariBesok")
    fun countKunjunganAntara(awalHari: Long, awalHariBesok: Long): Flow<Int>

    @Query("SELECT * FROM pasien WHERE id = :id")
    suspend fun getById(id: Int): Pasien?

    @Insert
    suspend fun insert(pasien: Pasien): Long

    /** Menandai pasien selesai istirahat; mengembalikan jumlah baris yang berubah. */
    @Query(
        """
        UPDATE pasien SET waktu_keluar = :waktuKeluar, status = :statusBaru
        WHERE id = :id AND status = :statusLama
        """,
    )
    suspend fun selesaikanIstirahat(
        id: Int,
        waktuKeluar: Long,
        statusLama: String,
        statusBaru: String,
    ): Int
}

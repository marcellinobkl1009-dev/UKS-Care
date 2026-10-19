package com.sekolah.ukscare.data.repository

import com.sekolah.ukscare.data.local.ObatDao
import com.sekolah.ukscare.data.model.Obat
import kotlinx.coroutines.flow.Flow

/** Alasan pengurangan stok obat ditolak. */
enum class AlasanStokGagal {
    OBAT_TIDAK_DITEMUKAN,
    STOK_HABIS,
    OBAT_KADALUWARSA,
}

/** Hasil pengurangan stok satu unit. */
sealed interface HasilKurangiStok {
    data class Berhasil(val sisaStok: Int) : HasilKurangiStok

    data class Gagal(val alasan: AlasanStokGagal) : HasilKurangiStok
}

/** Obat dengan id tertentu tidak ada di database. */
class ObatTidakDitemukanException(id: Int) :
    IllegalStateException("Obat dengan id $id tidak ditemukan")

/** Input stok tidak valid; stok tidak boleh negatif. */
class StokTidakValidException(pesan: String) : IllegalArgumentException(pesan)

/**
 * Aturan bisnis stok obat. Semua perubahan stok melewati klausa penjaga di [ObatDao]
 * sehingga stok tidak pernah negatif, bahkan bila dipanggil dari beberapa tempat.
 */
class ObatRepository(private val obatDao: ObatDao) {

    fun getSemuaObat(): Flow<List<Obat>> = obatDao.getAll()

    /** Obat yang boleh diberikan: stok masih ada dan belum kadaluwarsa. */
    fun getObatDapatDiberikan(awalHariIni: Long): Flow<List<Obat>> =
        obatDao.getDapatDiberikan(awalHariIni)

    suspend fun getObat(id: Int): Obat? = obatDao.getById(id)

    suspend fun tambahObat(namaObat: String, stok: Int, expiredDate: Long): Result<Long> {
        if (namaObat.isBlank()) return Result.failure(StokTidakValidException("Nama obat wajib diisi"))
        if (stok < 0) return Result.failure(StokTidakValidException("Stok awal tidak boleh negatif"))
        val id = obatDao.insert(
            Obat(namaObat = namaObat.trim(), stok = stok, expiredDate = expiredDate),
        )
        return Result.success(id)
    }

    suspend fun perbaruiObat(
        id: Int,
        namaObat: String,
        stok: Int,
        expiredDate: Long,
    ): Result<Unit> {
        if (namaObat.isBlank()) return Result.failure(StokTidakValidException("Nama obat wajib diisi"))
        if (stok < 0) return Result.failure(StokTidakValidException("Stok tidak boleh negatif"))
        val baris = obatDao.updateObat(id, namaObat.trim(), stok, expiredDate)
        return if (baris > 0) Result.success(Unit) else Result.failure(ObatTidakDitemukanException(id))
    }

    /** Menimpa stok dengan nilai baru (dipakai form edit). */
    suspend fun aturStok(id: Int, stokBaru: Int): Result<Unit> {
        if (stokBaru < 0) return Result.failure(StokTidakValidException("Stok tidak boleh negatif"))
        val baris = obatDao.updateStok(id, stokBaru)
        return if (baris > 0) Result.success(Unit) else Result.failure(ObatTidakDitemukanException(id))
    }

    /** Restock cepat. [jumlah] boleh negatif selama stok akhir tidak menjadi negatif. */
    suspend fun tambahStok(id: Int, jumlah: Int): Result<Unit> {
        if (obatDao.tambahStokTerjaga(id, jumlah) > 0) return Result.success(Unit)
        val obat = obatDao.getById(id) ?: return Result.failure(ObatTidakDitemukanException(id))
        return Result.failure(
            StokTidakValidException("Stok tersisa ${obat.stok}, tidak cukup untuk perubahan $jumlah"),
        )
    }

    suspend fun hapusObat(id: Int): Result<Unit> {
        val baris = obatDao.deleteById(id)
        return if (baris > 0) Result.success(Unit) else Result.failure(ObatTidakDitemukanException(id))
    }

    /**
     * Mengurangi stok satu unit lewat UPDATE berpengaman. Harus dipanggil di dalam transaksi
     * penyimpanan pasien agar bisa dibatalkan bila [HasilKurangiStok.Gagal] dikembalikan.
     */
    suspend fun kurangiStokUntukDispensasi(id: Int, awalHariIni: Long): HasilKurangiStok {
        val obat = obatDao.getById(id)
            ?: return HasilKurangiStok.Gagal(AlasanStokGagal.OBAT_TIDAK_DITEMUKAN)

        val barisBerubah = obatDao.kurangiStokTerjaga(id, awalHariIni)
        if (barisBerubah > 0) return HasilKurangiStok.Berhasil(obat.stok - 1)

        // Pengurangan ditolak. Kadaluwarsa diperiksa lebih dulu (lihat AGENTS.md 6.4),
        // sisanya berarti stok memang habis.
        val alasan = if (obat.expiredDate < awalHariIni) {
            AlasanStokGagal.OBAT_KADALUWARSA
        } else {
            AlasanStokGagal.STOK_HABIS
        }
        return HasilKurangiStok.Gagal(alasan)
    }
}

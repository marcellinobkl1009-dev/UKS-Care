package com.sekolah.ukscare.data.repository

import androidx.room.withTransaction
import com.sekolah.ukscare.data.local.AppDatabase
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.data.model.Pasien
import kotlinx.coroutines.flow.Flow

/** Data yang dibutuhkan untuk mencatat satu kunjungan baru. */
data class MasukanPasien(
    val namaSiswa: String,
    val kelas: String,
    val jenisKelamin: String,
    val keluhan: String,
    val tindakan: String,
    /** Obat yang diberikan, atau null bila tidak ada. */
    val obat: Obat? = null,
)

/** Hasil penyimpanan pasien. Waktu selalu dikirim pemanggil agar jam dapat di-inject saat pengujian. */
sealed interface HasilSimpanPasien {
    data class Berhasil(val idPasien: Int) : HasilSimpanPasien

    /** Obat gagal diberikan; dipastikan tidak ada baris pasien yang tersimpan. */
    data class GagalObat(val alasan: AlasanStokGagal) : HasilSimpanPasien
}

/**
 * Repository kunjungan pasien. Penyimpanan pasien dan pengurangan stok obat
 * dijalankan dalam satu transaksi Room.
 */
class PasienRepository(
    private val db: AppDatabase,
    private val obatRepository: ObatRepository,
) {
    private val pasienDao = db.pasienDao()

    fun getPasienAktif(): Flow<List<Pasien>> = pasienDao.getPasienByStatus(Pasien.STATUS_AKTIF)

    fun getRiwayat(): Flow<List<Pasien>> = pasienDao.getPasienByStatus(Pasien.STATUS_SELESAI)

    /** Pencarian riwayat berdasarkan nama; nama kosong berarti tampilkan semua riwayat. */
    fun cariRiwayat(nama: String): Flow<List<Pasien>> = if (nama.isBlank()) {
        getRiwayat()
    } else {
        pasienDao.cariRiwayat(Pasien.STATUS_SELESAI, nama.trim())
    }

    fun countPasienAktif(): Flow<Int> = pasienDao.countByStatus(Pasien.STATUS_AKTIF)

    fun countKunjunganHariIni(awalHariIni: Long, awalHariBesok: Long): Flow<Int> =
        pasienDao.countKunjunganAntara(awalHariIni, awalHariBesok)

    /**
     * Menyimpan pasien baru dan (bila ada) mengurangi stok obat dalam SATU transaksi.
     * Bila pengurangan stok ditolak, transaksi dibatalkan sehingga tidak ada baris pasien yang tersisa.
     */
    suspend fun simpanPasien(
        masukan: MasukanPasien,
        sekarang: Long,
        awalHariIni: Long,
    ): HasilSimpanPasien = try {
        db.withTransaction {
            val obat = masukan.obat
            if (obat != null) {
                val hasil = obatRepository.kurangiStokUntukDispensasi(obat.id, awalHariIni)
                if (hasil is HasilKurangiStok.Gagal) {
                    // Dilempar agar Room melakukan rollback transaksi.
                    throw PemberianObatGagalException(hasil.alasan)
                }
            }
            val id = pasienDao.insert(
                Pasien(
                    namaSiswa = masukan.namaSiswa.trim(),
                    kelas = masukan.kelas,
                    jenisKelamin = masukan.jenisKelamin,
                    keluhan = masukan.keluhan,
                    tindakan = masukan.tindakan,
                    obatDiberikan = obat?.namaObat,
                    waktuMasuk = sekarang,
                    waktuKeluar = null,
                    status = Pasien.STATUS_AKTIF,
                ),
            )
            HasilSimpanPasien.Berhasil(id.toInt())
        }
    } catch (e: PemberianObatGagalException) {
        HasilSimpanPasien.GagalObat(e.alasan)
    }

    /** Menandai pasien selesai istirahat. Mengembalikan true bila ada baris yang berubah. */
    suspend fun selesaikanIstirahat(id: Int, waktuKeluar: Long): Boolean = pasienDao.selesaikanIstirahat(
        id = id,
        waktuKeluar = waktuKeluar,
        statusLama = Pasien.STATUS_AKTIF,
        statusBaru = Pasien.STATUS_SELESAI,
    ) > 0
}

/** Sinyal internal untuk membatalkan transaksi saat obat tidak bisa diberikan. */
private class PemberianObatGagalException(val alasan: AlasanStokGagal) :
    IllegalStateException("Pengurangan stok obat gagal: $alasan")

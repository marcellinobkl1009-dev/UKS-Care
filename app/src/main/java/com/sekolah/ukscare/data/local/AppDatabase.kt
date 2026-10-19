package com.sekolah.ukscare.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.data.model.Pasien

/**
 * Database Room aplikasi. Skema dibuat sekali dengan data awal 5 jenis obat.
 */
@Database(
    entities = [Pasien::class, Obat::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pasienDao(): PasienDao

    abstract fun obatDao(): ObatDao

    companion object {
        private const val NAMA_DATABASE = "ukscare.db"

        /** Obat bawaan saat database pertama kali dibuat. */
        private val OBAT_AWAL = listOf(
            "Paracetamol",
            "Antasida",
            "Betadine",
            "Plester",
            "Minyak Kayu Putih",
        )

        private const val STOK_AWAL = 10
        private const val SATU_HARI_MILLIS = 24L * 60 * 60 * 1000

        /**
         * [clock] bisa diganti pada pengujian agar tanggal kadaluwarsa data awal deterministik.
         */
        fun build(
            context: Context,
            clock: () -> Long = System::currentTimeMillis,
        ): AppDatabase = Room
            .databaseBuilder(context.applicationContext, AppDatabase::class.java, NAMA_DATABASE)
            .addCallback(SeedCallback(clock))
            .build()
    }

    /**
     * Callback pembuatan database. Sengaja memakai SQL mentah pada [SupportSQLiteDatabase]
     * (bukan instance database) agar tidak terjadi deadlock/rekursi.
     * `internal` supaya pengujian unit dapat memakai callback yang sama pada database in-memory.
     */
    internal class SeedCallback(private val clock: () -> Long) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            val kadaluwarsa = clock() + 365 * SATU_HARI_MILLIS
            OBAT_AWAL.forEach { nama ->
                db.execSQL(
                    "INSERT INTO obat (nama_obat, stok, expired_date) VALUES (?, ?, ?)",
                    arrayOf<Any>(nama, STOK_AWAL, kadaluwarsa),
                )
            }
        }
    }
}

package com.sekolah.ukscare.ui.dashboard

import com.sekolah.ukscare.data.model.Pasien
import com.sekolah.ukscare.ui.UiText

/** Tab pada Dashboard (AGENTS.md 6.1). */
enum class TabDashboard {
    SEDANG_DIRAWAT,
    RIWAYAT,
}

/**
 * Satu kartu pasien yang masih dirawat.
 * [durasiIstirahatMillis] dihitung ViewModel dari jam yang di-inject + detak berkala,
 * bukan dihitung ulang di dalam composable.
 */
data class PasienAktifUi(
    val pasien: Pasien,
    val durasiIstirahatMillis: Long,
    val teksWaktuMasuk: String,
)

/** Satu kartu riwayat pasien yang sudah selesai istirahat. */
data class PasienRiwayatUi(
    val pasien: Pasien,
    val teksWaktuMasuk: String,
    /** Null bila baris lama belum sempat mengisi waktu keluar. */
    val teksWaktuKeluar: String?,
)

/** Angka ringkasan di bagian atas Dashboard. */
data class RingkasanDashboard(
    val sedangDirawat: Int = 0,
    val kunjunganHariIni: Int = 0,
    /** Jumlah obat yang statusnya bukan "Aman" (kritis, hampir kadaluwarsa, kadaluwarsa). */
    val obatBermasalah: Int = 0,
)

/** Seluruh state Dashboard. Tanpa logika; logika ada di [DashboardViewModel]. */
data class DashboardUiState(
    val memuat: Boolean = true,
    val ringkasan: RingkasanDashboard = RingkasanDashboard(),
    val tabTerpilih: TabDashboard = TabDashboard.SEDANG_DIRAWAT,
    val daftarAktif: List<PasienAktifUi> = emptyList(),
    val daftarRiwayat: List<PasienRiwayatUi> = emptyList(),
    val kueriPencarian: String = "",
    val kesalahan: UiText? = null,
) {
    /** Badge ikon Kelola Obat hanya muncul bila ada obat bermasalah. */
    val adaObatBermasalah: Boolean get() = ringkasan.obatBermasalah > 0
}

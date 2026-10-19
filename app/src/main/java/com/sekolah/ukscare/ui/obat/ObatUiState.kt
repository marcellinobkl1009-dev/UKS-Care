package com.sekolah.ukscare.ui.obat

import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.util.StatusObat

/** Satu baris daftar obat beserta statusnya (warna + label, AGENTS.md 6.4). */
data class ObatUiItem(
    val obat: Obat,
    val status: StatusObat,
)

/** State layar Kelola Obat. Tanpa logika. */
data class ObatUiState(
    val memuat: Boolean = true,
    /** Sudah diurutkan: obat bermasalah lebih dulu. */
    val daftar: List<ObatUiItem> = emptyList(),
    val kesalahan: UiText? = null,
) {
    val adaMasalah: Boolean get() = daftar.any { it.status != StatusObat.AMAN }
}

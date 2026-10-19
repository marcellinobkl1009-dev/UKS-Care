package com.sekolah.ukscare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.sekolah.ukscare.util.StatusObat

/** Pasangan warna wadah dan teks untuk badge status obat. */
@Immutable
data class WarnaBadge(
    val wadah: Color,
    val konten: Color,
)

/** Hijau untuk aman, jingga untuk hampir kadaluwarsa, merah untuk kritis/kadaluwarsa. */
private val WARNA_TERANG = mapOf(
    StatusObat.KADALUWARSA to WarnaBadge(BadgeMerahWadah, BadgeMerahTeks),
    StatusObat.STOK_KRITIS to WarnaBadge(BadgeMerahWadah, BadgeMerahTeks),
    StatusObat.HAMPIR_KADALUWARSA to WarnaBadge(BadgeJinggaWadah, BadgeJinggaTeks),
    StatusObat.AMAN to WarnaBadge(BadgeHijauWadah, BadgeHijauTeks),
)

private val WARNA_GELAP = mapOf(
    StatusObat.KADALUWARSA to WarnaBadge(BadgeMerahWadahGelap, BadgeMerahTeksGelap),
    StatusObat.STOK_KRITIS to WarnaBadge(BadgeMerahWadahGelap, BadgeMerahTeksGelap),
    StatusObat.HAMPIR_KADALUWARSA to WarnaBadge(BadgeJinggaWadahGelap, BadgeJinggaTeksGelap),
    StatusObat.AMAN to WarnaBadge(BadgeHijauWadahGelap, BadgeHijauTeksGelap),
)

@Composable
fun warnaStatus(status: StatusObat): WarnaBadge =
    (if (isSystemInDarkTheme()) WARNA_GELAP else WARNA_TERANG).getValue(status)

package com.sekolah.ukscare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Skema terang hijau/teal. */
private val SkemaTerang = lightColorScheme(
    primary = HijauUtama,
    onPrimary = Color.White,
    primaryContainer = HijauWadah,
    onPrimaryContainer = HijauTeksGelap,
    secondary = HijauSekunder,
    onSecondary = Color.White,
    secondaryContainer = HijauSekunderWadah,
    onSecondaryContainer = HijauSekunderTeksGelap,
    tertiary = BiruTersier,
    onTertiary = Color.White,
    tertiaryContainer = BiruWadah,
    onTertiaryContainer = BiruGelap,
    error = MerahError,
    onError = Color.White,
    errorContainer = MerahWadahError,
    onErrorContainer = MerahTeksError,
    background = Latar,
    onBackground = Teks,
    surface = Latar,
    onSurface = Teks,
    surfaceVariant = VarianPermukaan,
    onSurfaceVariant = TeksVarian,
    outline = Garis,
)

/** Skema gelap dengan keluarga warna yang sama. */
private val SkemaGelap = darkColorScheme(
    primary = HijauTerang,
    onPrimary = HijauGelap,
    primaryContainer = HijauWadahGelap,
    onPrimaryContainer = HijauWadah,
    secondary = HijauSekunderTerang,
    onSecondary = HijauSekunderGelap,
    secondaryContainer = HijauSekunderWadahGelap,
    onSecondaryContainer = HijauSekunderWadah,
    tertiary = BiruTersierTerang,
    onTertiary = BiruGelap,
    tertiaryContainer = BiruWadahGelap,
    onTertiaryContainer = BiruWadah,
    error = MerahErrorTerang,
    onError = MerahTeksErrorGelap,
    errorContainer = MerahWadahErrorGelap,
    onErrorContainer = MerahWadahError,
    background = LatarGelap,
    onBackground = TeksGelap,
    surface = LatarGelap,
    onSurface = TeksGelap,
    surfaceVariant = VarianPermukaanGelap,
    onSurfaceVariant = TeksVarianGelap,
    outline = GarisGelap,
)

/**
 * Tema aplikasi. Dynamic color sengaja TIDAK dipakai (AGENTS.md 8):
 * identitas hijau/teal harus tetap sama di semua perangkat.
 */
@Composable
fun UksCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SkemaGelap else SkemaTerang,
        typography = TipografiUks,
        content = content,
    )
}

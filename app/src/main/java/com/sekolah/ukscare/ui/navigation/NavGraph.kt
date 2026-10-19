package com.sekolah.ukscare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sekolah.ukscare.ui.dashboard.DashboardScreen
import com.sekolah.ukscare.ui.form.FormPasienScreen
import com.sekolah.ukscare.ui.obat.KelolaObatScreen

/** Rute navigasi aplikasi. String biasa karena proyek ini tidak memakai kotlinx.serialization. */
object Rute {
    const val DASHBOARD = "dashboard"
    const val FORM_PASIEN = "form_pasien"
    const val KELOLA_OBAT = "kelola_obat"
}

/**
 * Grafik navigasi aplikasi (AGENTS.md, struktur `ui/navigation/NavGraph`).
 *
 * - Dashboard adalah start destination, jadi tombol kembali di Dashboard keluar dari aplikasi.
 * - Form pasien dan Kelola Obat adalah destination sendiri; ViewModel-nya dibatasi
 *   [NavBackStackEntry] masing-masing, sehingga setiap pembukaan form mendapat
 *   `FormPasienViewModel` yang baru (satu instance hanya boleh menyimpan satu pasien)
 *   dan instance lama dibuang saat destination dikeluarkan dari back stack.
 * - Tombol kembali sistem dan tombol panah pada top bar sama-sama memanggil [popBackStack].
 */
@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Rute.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Rute.DASHBOARD) {
            DashboardScreen(
                onTambahPasien = { navController.bukaSekali(Rute.FORM_PASIEN) },
                onBukaKelolaObat = { navController.bukaSekali(Rute.KELOLA_OBAT) },
            )
        }

        composable(Rute.FORM_PASIEN) {
            // ViewModel form dibuat form layar itu sendiri dengan factory aplikasi,
            // dan otomatis dimiliki NavBackStackEntry destination ini.
            FormPasienScreen(onKembali = { navController.popBackStack() })
        }

        composable(Rute.KELOLA_OBAT) {
            KelolaObatScreen(onKembali = { navController.popBackStack() })
        }
    }
}

/**
 * Membuka [rute] sekali saja.
 *
 * Dua ketukan cepat pada FAB dapat memanggil navigasi dua kali; `launchSingleTop`
 * membuat ketukan kedua memakai ulang destination yang sudah di puncak, dan penjaga
 * rute ini menutup kasus saat puncak sudah berubah tetapi composable belum tergambar ulang.
 */
private fun NavHostController.bukaSekali(rute: String) {
    if (currentDestination?.route == rute) return
    navigate(rute) { launchSingleTop = true }
}

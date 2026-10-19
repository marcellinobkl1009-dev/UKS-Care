package com.sekolah.ukscare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sekolah.ukscare.ui.dashboard.DashboardScreen
import com.sekolah.ukscare.ui.form.FormPasienScreen
import com.sekolah.ukscare.ui.form.SesiForm
import com.sekolah.ukscare.ui.form.ingatPemilikSesiForm
import com.sekolah.ukscare.ui.obat.KelolaObatScreen
import com.sekolah.ukscare.ui.theme.UksCareTheme

/**
 * Satu-satunya Activity.
 *
 * M5c: Dashboard menjadi layar awal, dengan pengalih sederhana ke form pasien dan
 * Kelola Obat supaya ketiganya bisa dicoba di emulator. NavGraph menggantikan bagian
 * ini pada M6.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UksCareTheme {
                PengalihLayarSementara()
            }
        }
    }
}

/** Layar yang bisa dibuka lewat pengalih sementara (M5c, diganti NavGraph pada M6). */
private enum class LayarUji { DASHBOARD, FORM_PASIEN, KELOLA_OBAT }

/**
 * Pengalih layar sementara (M5c, diganti NavGraph pada M6).
 *
 * Satu sesi form = satu [com.sekolah.ukscare.ui.form.PemilikSesiForm]: ViewModel form
 * dibersihkan setiap form ditutup, sehingga pembukaan berikutnya selalu mulai dari form
 * kosong (FormPasienViewModel hanya boleh menyimpan satu pasien per instance).
 *
 * Setelah form ditutup, Dashboard ditampilkan lagi. Pasien baru muncul di tab
 * "Sedang Dirawat" sendiri karena daftar berasal dari Room lewat StateFlow, bukan dari
 * hasil simpan yang diteruskan antar layar.
 */
@Composable
private fun PengalihLayarSementara() {
    // Nama enum disimpan sebagai teks agar rememberSaveable tidak perlu Saver khusus.
    var layar by rememberSaveable { mutableStateOf(LayarUji.DASHBOARD.name) }
    val pemilikSesi = ingatPemilikSesiForm()

    fun pindahKe(target: LayarUji) {
        layar = target.name
    }

    fun tutupForm() {
        pemilikSesi.bersihkan()
        pindahKe(LayarUji.DASHBOARD)
    }

    when (LayarUji.valueOf(layar)) {
        LayarUji.DASHBOARD -> DashboardScreen(
            onTambahPasien = {
                // Sisa sesi sebelumnya dibuang agar form benar-benar baru.
                pemilikSesi.bersihkan()
                pindahKe(LayarUji.FORM_PASIEN)
            },
            onBukaKelolaObat = { pindahKe(LayarUji.KELOLA_OBAT) },
        )

        LayarUji.FORM_PASIEN -> {
            // Tombol kembali sistem ikut menutup form, sama seperti NavGraph nanti.
            BackHandler { tutupForm() }
            SesiForm(pemilik = pemilikSesi) {
                FormPasienScreen(onKembali = ::tutupForm)
            }
        }

        LayarUji.KELOLA_OBAT -> {
            BackHandler { pindahKe(LayarUji.DASHBOARD) }
            KelolaObatScreen(onKembali = { pindahKe(LayarUji.DASHBOARD) })
        }
    }
}

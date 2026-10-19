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
import com.sekolah.ukscare.ui.form.FormPasienScreen
import com.sekolah.ukscare.ui.form.SesiForm
import com.sekolah.ukscare.ui.form.ingatPemilikSesiForm
import com.sekolah.ukscare.ui.obat.KelolaObatScreen
import com.sekolah.ukscare.ui.theme.UksCareTheme

/**
 * Satu-satunya Activity.
 *
 * M5b: layar Kelola Obat dan form pasien ditampilkan bergantian lewat state sederhana
 * supaya keduanya bisa dicoba di emulator. NavGraph akan menggantikan bagian ini pada M6.
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

/**
 * Pengalih layar sementara (M5b, diganti NavGraph pada M6).
 *
 * Tombol kembali di Kelola Obat sementara dipakai untuk membuka form pasien.
 * Satu sesi form = satu [com.sekolah.ukscare.ui.form.PemilikSesiForm]: saat form ditutup
 * ViewModel-nya dibersihkan, sehingga pembukaan berikutnya selalu mulai dari form kosong
 * (FormPasienViewModel hanya boleh menyimpan satu pasien per instance).
 */
@Composable
private fun PengalihLayarSementara() {
    var formTerbuka by rememberSaveable { mutableStateOf(false) }
    val pemilikSesi = ingatPemilikSesiForm()

    fun tutupForm() {
        pemilikSesi.bersihkan()
        formTerbuka = false
    }

    if (formTerbuka) {
        // Tombol kembali sistem ikut menutup form, sama seperti NavGraph nanti.
        BackHandler { tutupForm() }
        SesiForm(pemilik = pemilikSesi) {
            FormPasienScreen(onKembali = ::tutupForm)
        }
    } else {
        KelolaObatScreen(onKembali = { formTerbuka = true })
    }
}

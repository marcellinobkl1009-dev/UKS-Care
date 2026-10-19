package com.sekolah.ukscare.ui.form

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pemilik ViewModelStore untuk SATU sesi form pasien baru.
 *
 * [FormPasienViewModel] hanya boleh menyimpan satu pasien per instance, sehingga setiap
 * kali form dibuka harus ada instance baru. Pengalih layar sementara di `MainActivity`
 * memakai pemilik ini: daftar ViewModel-nya dibersihkan saat form ditutup
 * ([bersihkan]), tetapi pemiliknya sendiri disimpan sebagai ViewModel Activity sehingga
 * isi form tetap bertahan saat layar diputar.
 *
 * **M6 (NavGraph):** cukup panggil `viewModel(factory = AppViewModelProvider.Factory)`
 * di dalam composable destination. `NavBackStackEntry` sudah menjadi
 * `ViewModelStoreOwner`, jadi tiap kunjungan ke destination form mendapat instance baru
 * dan instance lama dibuang saat destination keluar dari back stack. Kelas ini (beserta
 * fungsi [ingatPemilikSesiForm] dan [SesiForm]) bisa dihapus saat itu.
 */
class PemilikSesiForm(aplikasi: Application) :
    ViewModel(),
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    // Diperlukan oleh viewModel(): extras membawa Application untuk AppViewModelProvider.Factory.
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        ViewModelProvider.AndroidViewModelFactory.getInstance(aplikasi)

    override val defaultViewModelCreationExtras: CreationExtras = MutableCreationExtras().apply {
        this[APPLICATION_KEY] = aplikasi
    }

    /** Dipanggil saat form ditutup agar pembukaan berikutnya memakai ViewModel yang baru. */
    fun bersihkan() = viewModelStore.clear()
}

/**
 * Mengambil [PemilikSesiForm] dari ViewModelStore Activity.
 * Karena disimpan di sana, pemilik ini (dan isi form) bertahan saat rotasi.
 */
@Composable
fun ingatPemilikSesiForm(): PemilikSesiForm {
    val aplikasi = LocalContext.current.applicationContext as Application
    return viewModel(factory = FactoryPemilikSesiForm(aplikasi))
}

/** Meneruskan pemilik sesi sebagai [LocalViewModelStoreOwner] ke layar form. */
@Composable
fun SesiForm(pemilik: PemilikSesiForm, konten: @Composable () -> Unit) {
    CompositionLocalProvider(LocalViewModelStoreOwner provides pemilik) { konten() }
}

/** Pemilik sesi bukan bagian DI aplikasi, jadi dibuat factory tersendiri. */
private class FactoryPemilikSesiForm(private val aplikasi: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PemilikSesiForm::class.java)) {
            "FactoryPemilikSesiForm tidak dapat membuat $modelClass"
        }
        @Suppress("UNCHECKED_CAST")
        return PemilikSesiForm(aplikasi) as T
    }
}

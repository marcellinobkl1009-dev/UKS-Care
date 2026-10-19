package com.sekolah.ukscare.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sekolah.ukscare.UksCareApp
import com.sekolah.ukscare.ui.dashboard.DashboardViewModel
import com.sekolah.ukscare.ui.form.FormPasienViewModel
import com.sekolah.ukscare.ui.obat.ObatViewModel

/**
 * Factory ViewModel manual (tanpa Hilt, AGENTS.md 2).
 * Repository selalu diambil dari [UksCareApp] sehingga hanya ada satu sumber dependency.
 *
 * Pemakaian di layar: `viewModel(factory = AppViewModelProvider.Factory)`.
 */
object AppViewModelProvider {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val aplikasi = aplikasi()
            DashboardViewModel(
                pasienRepository = aplikasi.pasienRepository,
                obatRepository = aplikasi.obatRepository,
            )
        }
        initializer {
            val aplikasi = aplikasi()
            FormPasienViewModel(
                pasienRepository = aplikasi.pasienRepository,
                obatRepository = aplikasi.obatRepository,
            )
        }
        initializer {
            ObatViewModel(obatRepository = aplikasi().obatRepository)
        }
    }
}

/** Application dibagikan lewat CreationExtras oleh NavBackStackEntry/Activity. */
private fun CreationExtras.aplikasi(): UksCareApp =
    checkNotNull(this[APPLICATION_KEY] as? UksCareApp) {
        "AppViewModelProvider membutuhkan Application bertipe UksCareApp"
    }

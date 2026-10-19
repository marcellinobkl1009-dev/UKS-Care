package com.sekolah.ukscare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sekolah.ukscare.ui.navigation.NavGraph
import com.sekolah.ukscare.ui.theme.UksCareTheme

/**
 * Satu-satunya Activity. Seluruh perpindahan layar diatur [NavGraph].
 *
 * ViewModel layar dibatasi back stack entry masing-masing destination, sehingga
 * state (tab dan pencarian Dashboard, isi form pasien, lembar share yang terbuka)
 * tetap bertahan saat rotasi dan dibuang saat destination-nya ditutup.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UksCareTheme {
                NavGraph()
            }
        }
    }
}

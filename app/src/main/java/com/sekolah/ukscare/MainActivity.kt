package com.sekolah.ukscare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sekolah.ukscare.ui.obat.KelolaObatScreen
import com.sekolah.ukscare.ui.theme.UksCareTheme

/**
 * Satu-satunya Activity.
 *
 * M5a: layar Kelola Obat ditampilkan langsung supaya bisa dicoba di emulator.
 * NavGraph akan menggantikan bagian ini pada milestone M6.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UksCareTheme {
                KelolaObatScreen()
            }
        }
    }
}

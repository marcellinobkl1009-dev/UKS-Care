package com.sekolah.ukscare.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Pembagian surat izin lewat WhatsApp.
 * Pembuatan Intent dipisah dari peluncurannya supaya bagian pertama mudah diuji.
 */
object WhatsAppShare {

    const val PAKET_WHATSAPP = "com.whatsapp"

    /** Intent yang menarget langsung aplikasi WhatsApp. */
    fun buatIntentWhatsApp(pesan: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, pesan)
        setPackage(PAKET_WHATSAPP)
    }

    /** Intent pengirim generik + chooser, dipakai bila WhatsApp tidak terpasang. */
    fun buatIntentChooser(pesan: String, judul: String): Intent {
        val pengirim = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, pesan)
        }
        return Intent.createChooser(pengirim, judul)
    }

    /**
     * Coba WhatsApp lebih dulu; bila aplikasinya tidak ada, [ActivityNotFoundException]
     * ditangkap dan chooser aplikasi lain dipakai. Tidak bergantung pada hasil query package manager.
     */
    fun bagikan(context: Context, pesan: String, judul: String) {
        try {
            context.startActivity(siapDiluncurkan(buatIntentWhatsApp(pesan), context))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(siapDiluncurkan(buatIntentChooser(pesan, judul), context))
        }
    }

    private fun siapDiluncurkan(intent: Intent, context: Context): Intent = intent.apply {
        // Diperlukan bila dipanggil dari Context non-Activity.
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

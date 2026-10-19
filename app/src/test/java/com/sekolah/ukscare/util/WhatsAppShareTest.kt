package com.sekolah.ukscare.util

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Menguji bagian "membuat Intent"; peluncuran sebenarnya diverifikasi manual di perangkat. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppShareTest {

    @Test
    fun `intent WhatsApp menarget paket WhatsApp`() {
        val intent = WhatsAppShare.buatIntentWhatsApp("halo UKS")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals(WhatsAppShare.PAKET_WHATSAPP, intent.`package`)
        assertEquals("halo UKS", intent.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `chooser membungkus intent pengirim dan judulnya`() {
        val chooser = WhatsAppShare.buatIntentChooser("halo UKS", "Bagikan surat izin")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals("Bagikan surat izin", chooser.getStringExtra(Intent.EXTRA_TITLE))

        val pengirim = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals(Intent.ACTION_SEND, pengirim?.action)
        assertEquals("text/plain", pengirim?.type)
        assertEquals("halo UKS", pengirim?.getStringExtra(Intent.EXTRA_TEXT))
    }
}

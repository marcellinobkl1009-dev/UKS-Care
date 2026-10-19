package com.sekolah.ukscare.ui

import android.content.Context
import androidx.annotation.StringRes

/**
 * Teks yang ditampilkan UI. ViewModel hanya menyimpan id string (atau teks dinamis),
 * sehingga tidak ada teks Indonesia yang ditulis langsung di kode ViewModel
 * (lihat AGENTS.md 7.5 dan 8).
 */
sealed interface UiText {

    /** Teks dari `strings.xml`, dengan argumen opsional untuk format. */
    data class Teks(@param:StringRes val id: Int, val argumen: List<Any> = emptyList()) : UiText

    /** Teks yang sudah berupa nilai jadi, misalnya pesan dari sistem/kueri. */
    data class Dinamis(val nilai: String) : UiText
}

/** Diubah menjadi teks pada lapisan UI saja, agar ViewModel tetap bebas Android. */
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Teks -> if (argumen.isEmpty()) {
        context.getString(id)
    } else {
        context.getString(id, *argumen.toTypedArray())
    }

    is UiText.Dinamis -> nilai
}

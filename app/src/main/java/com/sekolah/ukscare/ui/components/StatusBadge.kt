package com.sekolah.ukscare.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sekolah.ukscare.R
import com.sekolah.ukscare.ui.theme.warnaStatus
import com.sekolah.ukscare.util.StatusObat

/**
 * Badge status obat. Warna SELALU disertai label teks supaya tetap terbaca
 * oleh pengguna dengan gangguan penglihatan warna (AGENTS.md 6.4).
 */
@Composable
fun StatusBadge(
    status: StatusObat,
    modifier: Modifier = Modifier,
) {
    val warna = warnaStatus(status)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(warna.wadah)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(warna.konten),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(labelStatusObat(status)),
            color = warna.konten,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Label teks status; dipisah agar dapat dipakai ulang (mis. pada ringkasan). */
@StringRes
fun labelStatusObat(status: StatusObat): Int = when (status) {
    StatusObat.KADALUWARSA -> R.string.status_kadaluwarsa
    StatusObat.STOK_KRITIS -> R.string.status_stok_kritis
    StatusObat.HAMPIR_KADALUWARSA -> R.string.status_hampir_kadaluwarsa
    StatusObat.AMAN -> R.string.status_aman
}

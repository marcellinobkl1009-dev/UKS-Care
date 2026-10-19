package com.sekolah.ukscare.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.sekolah.ukscare.R

/**
 * Dialog konfirmasi untuk aksi yang tidak bisa dibatalkan.
 * Semua teks dikirim pemanggil agar komponen ini tidak memuat teks Indonesia.
 */
@Composable
fun ConfirmDialog(
    judul: String,
    pesan: String,
    teksKonfirmasi: String,
    onKonfirmasi: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    teksBatal: String = stringResource(R.string.aksi_batal),
    destruktif: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = judul) },
        text = { Text(text = pesan) },
        confirmButton = {
            TextButton(onClick = onKonfirmasi) {
                Text(
                    text = teksKonfirmasi,
                    color = if (destruktif) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = teksBatal)
            }
        },
    )
}

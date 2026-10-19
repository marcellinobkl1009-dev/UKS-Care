package com.sekolah.ukscare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sekolah.ukscare.R
import com.sekolah.ukscare.ui.obat.ObatUiItem

/**
 * Kartu satu jenis obat: nama, status, tanggal kadaluwarsa, dan aksi cepat.
 * Stateless; seluruh aksi dinaikkan ke pemanggil. Semua tombol minimal 48dp.
 */
@Composable
fun ObatCard(
    item: ObatUiItem,
    teksKadaluwarsa: String,
    onRestock: (jumlah: Int) -> Unit,
    onEdit: () -> Unit,
    onHapus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deskripsiKurangiStok = stringResource(R.string.aksi_kurangi_stok)
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.obat.namaObat,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.obat_kadaluwarsa_nilai, teksKadaluwarsa),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.obat_stok_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Restock cepat. Stok tetap tidak bisa negatif karena repository memakai
                // klausa penjaga pada UPDATE (lihat AGENTS.md 7.1).
                OutlinedIconButton(
                    onClick = { onRestock(-1) },
                    enabled = item.obat.stok > 0,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = deskripsiKurangiStok },
                ) {
                    // Ikon minus digambar sendiri: material-icons-core tidak menyediakan
                    // Icons.Filled.Remove (hanya tersedia di material-icons-extended).
                    IkonKurang()
                }

                Text(
                    text = item.obat.stok.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 40.dp),
                )

                OutlinedIconButton(
                    onClick = { onRestock(1) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.aksi_tambah_stok),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.aksi_edit_obat),
                    )
                }
                IconButton(onClick = onHapus, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.aksi_hapus_obat),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** Tanda minus sederhana yang mengikuti warna konten tombol. */
@Composable
private fun IkonKurang(modifier: Modifier = Modifier) {
    val warna = LocalContentColor.current
    Canvas(modifier = modifier.size(14.dp)) {
        drawLine(
            color = warna,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

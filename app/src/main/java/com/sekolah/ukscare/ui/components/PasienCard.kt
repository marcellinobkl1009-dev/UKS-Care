package com.sekolah.ukscare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sekolah.ukscare.R
import com.sekolah.ukscare.ui.dashboard.PasienAktifUi
import com.sekolah.ukscare.ui.dashboard.PasienRiwayatUi

/**
 * Kartu pasien yang masih dirawat: durasi istirahat berjalan dan tombol Selesai Istirahat.
 * Stateless; aksi dinaikkan ke pemanggil (AGENTS.md 8).
 */
@Composable
fun PasienCard(
    item: PasienAktifUi,
    onBagikan: () -> Unit,
    onSelesaiIstirahat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KontenKartuPasien(
        namaSiswa = item.pasien.namaSiswa,
        kelas = item.pasien.kelas,
        keluhan = item.pasien.keluhan,
        teksWaktuMasuk = item.teksWaktuMasuk,
        durasiIstirahatMillis = item.durasiIstirahatMillis,
        teksWaktuKeluar = null,
        onBagikan = onBagikan,
        onSelesaiIstirahat = onSelesaiIstirahat,
        modifier = modifier,
    )
}

/** Kartu pasien riwayat: waktu keluar, tanpa tombol Selesai Istirahat. */
@Composable
fun PasienCard(
    item: PasienRiwayatUi,
    onBagikan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KontenKartuPasien(
        namaSiswa = item.pasien.namaSiswa,
        kelas = item.pasien.kelas,
        keluhan = item.pasien.keluhan,
        teksWaktuMasuk = item.teksWaktuMasuk,
        durasiIstirahatMillis = null,
        teksWaktuKeluar = item.teksWaktuKeluar,
        onBagikan = onBagikan,
        onSelesaiIstirahat = null,
        modifier = modifier,
    )
}

@Composable
private fun KontenKartuPasien(
    namaSiswa: String,
    kelas: String,
    keluhan: String,
    teksWaktuMasuk: String,
    /** Null untuk kartu riwayat. */
    durasiIstirahatMillis: Long?,
    /** Null bila baris lama belum mengisi waktu keluar. */
    teksWaktuKeluar: String?,
    onBagikan: () -> Unit,
    /** Null berarti kartu riwayat: tombol Selesai Istirahat tidak ditampilkan. */
    onSelesaiIstirahat: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = namaSiswa,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.pasien_kelas, kelas),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.pasien_keluhan, keluhan),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Tombol 48dp dengan ikon saja; maknanya dibawa contentDescription.
                IconButton(onClick = onBagikan, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.aksi_bagikan_surat),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pasien_waktu_masuk, teksWaktuMasuk),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (durasiIstirahatMillis != null) {
                Text(
                    text = stringResource(
                        R.string.pasien_durasi_istirahat,
                        teksDurasi(durasiIstirahatMillis),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (teksWaktuKeluar != null) {
                Text(
                    text = stringResource(R.string.pasien_waktu_keluar, teksWaktuKeluar),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (onSelesaiIstirahat != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSelesaiIstirahat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.aksi_selesai_istirahat))
                }
            }
        }
    }
}

/** Durasi istirahat berjalan, dibulatkan ke bawah ke satuan menit. */
@Composable
private fun teksDurasi(durasiMillis: Long): String {
    val menit = (durasiMillis / 60_000L).coerceAtLeast(0L)
    return when {
        menit < 1L -> stringResource(R.string.durasi_baru_mulai)
        menit < 60L -> stringResource(R.string.durasi_menit, menit.toInt())
        else -> stringResource(
            R.string.durasi_jam_menit,
            (menit / 60L).toInt(),
            (menit % 60L).toInt(),
        )
    }
}

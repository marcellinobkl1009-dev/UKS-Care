package com.sekolah.ukscare.ui.obat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.util.DateUtils
import java.time.Instant
import java.time.ZoneId

/**
 * Dialog tambah/ubah obat (AGENTS.md 6.4). [awal] bernilai null untuk mode tambah.
 * Stok diisi sebagai angka (input kuantitas) dan tanggal memakai DatePicker Material 3.
 */
@Composable
fun ObatFormDialog(
    awal: Obat?,
    onSimpan: (namaObat: String, stok: Int, expiredDate: Long) -> Unit,
    onDismiss: () -> Unit,
    zona: ZoneId = ZoneId.systemDefault(),
) {
    var namaObat by remember { mutableStateOf(awal?.namaObat.orEmpty()) }
    var stokTeks by remember { mutableStateOf((awal?.stok ?: 0).toString()) }
    var kadaluwarsa by remember { mutableLongStateOf(awal?.expiredDate ?: kadaluwarsaBawaan(zona)) }
    var tampilkanPemilihTanggal by remember { mutableStateOf(false) }

    val stok = stokTeks.toIntOrNull()
    val namaSalah = namaObat.isBlank()
    val stokSalah = stok == null || stok < 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (awal == null) R.string.dialog_obat_tambah_judul else R.string.dialog_obat_ubah_judul,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = namaObat,
                    onValueChange = { namaObat = it },
                    label = { Text(stringResource(R.string.field_nama_obat)) },
                    singleLine = true,
                    isError = namaSalah,
                    supportingText = if (namaSalah) {
                        { Text(stringResource(R.string.pesan_nama_obat_wajib)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = stokTeks,
                    // Hanya angka: stok negatif tidak mungkin dimasukkan dari keyboard.
                    onValueChange = { teks -> stokTeks = teks.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.field_stok)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = stokSalah,
                    supportingText = if (stokSalah) {
                        { Text(stringResource(R.string.pesan_stok_tidak_valid)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.field_kadaluwarsa),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { tampilkanPemilihTanggal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = DateUtils.formatTanggal(kadaluwarsa, zona))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSimpan(namaObat.trim(), stok ?: 0, kadaluwarsa) },
                enabled = !namaSalah && !stokSalah,
            ) {
                Text(stringResource(R.string.aksi_simpan))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.aksi_batal))
            }
        },
    )

    if (tampilkanPemilihTanggal) {
        DialogPemilihTanggal(
            awalMillis = kadaluwarsa,
            zona = zona,
            onPilih = { dipilih ->
                kadaluwarsa = dipilih
                tampilkanPemilihTanggal = false
            },
            onDismiss = { tampilkanPemilihTanggal = false },
        )
    }
}

/** DatePicker Material 3 mengembalikan millis UTC; konversinya ada di [DateUtils]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogPemilihTanggal(
    awalMillis: Long,
    zona: ZoneId,
    onPilih: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.dariLokalKeDatePicker(awalMillis, zona),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val dipilih = state.selectedDateMillis
                    if (dipilih == null) onDismiss() else onPilih(DateUtils.dariDatePickerKeLokal(dipilih, zona))
                },
            ) {
                Text(stringResource(R.string.aksi_pilih))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.aksi_batal))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * Bawaan satu tahun dari hari ini pada awal hari lokal.
 * Layar memakai jam perangkat; logika tanggal yang diuji berada di [DateUtils].
 */
private fun kadaluwarsaBawaan(zona: ZoneId): Long = Instant.ofEpochMilli(System.currentTimeMillis())
    .atZone(zona)
    .toLocalDate()
    .plusYears(1)
    .atStartOfDay(zona)
    .toInstant()
    .toEpochMilli()

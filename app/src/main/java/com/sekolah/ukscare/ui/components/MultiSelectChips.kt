package com.sekolah.ukscare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Deretan FilterChip yang dapat dipilih lebih dari satu dan otomatis
 * berpindah baris pada layar sempit.
 *
 * FilterChip Material 3 sudah memakai `minimumInteractiveComponentSize`
 * sehingga area sentuh tetap 48dp walau tinggi visualnya lebih kecil.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiSelectChips(
    pilihan: List<String>,
    terpilih: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pilihan.forEach { nilai ->
            FilterChip(
                selected = nilai in terpilih,
                onClick = { onToggle(nilai) },
                label = { Text(text = nilai) },
            )
        }
    }
}

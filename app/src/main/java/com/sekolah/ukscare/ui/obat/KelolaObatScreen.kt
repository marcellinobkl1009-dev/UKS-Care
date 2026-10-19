package com.sekolah.ukscare.ui.obat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sekolah.ukscare.R
import com.sekolah.ukscare.ui.AppViewModelProvider
import com.sekolah.ukscare.ui.asString
import com.sekolah.ukscare.ui.components.ConfirmDialog
import com.sekolah.ukscare.ui.components.EmptyState
import com.sekolah.ukscare.ui.components.ObatCard
import com.sekolah.ukscare.util.DateUtils
import java.time.ZoneId

/** Lebar maksimum konten agar tetap nyaman dibaca di tablet (AGENTS.md 8). */
private val LebarMaksKonten = 720.dp

/**
 * Layar Kelola Obat (AGENTS.md 6.4).
 * ViewModel disediakan [AppViewModelProvider] (DI manual, tanpa Hilt).
 */
@Composable
fun KelolaObatScreen(
    modifier: Modifier = Modifier,
    onKembali: (() -> Unit)? = null,
    viewModel: ObatViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    KelolaObatContent(
        state = state,
        onTambah = viewModel::tambahObat,
        onEdit = viewModel::editObat,
        onRestock = viewModel::restock,
        onHapus = viewModel::hapusObat,
        onKesalahanDibersihkan = viewModel::bersihkanKesalahan,
        modifier = modifier,
        onKembali = onKembali,
    )
}

/** Bagian stateless dari layar; seluruh state dan aksi dikirim dari luar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelolaObatContent(
    state: ObatUiState,
    onTambah: (namaObat: String, stok: Int, expiredDate: Long) -> Unit,
    onEdit: (id: Int, namaObat: String, stok: Int, expiredDate: Long) -> Unit,
    onRestock: (id: Int, jumlah: Int) -> Unit,
    onHapus: (id: Int) -> Unit,
    onKesalahanDibersihkan: () -> Unit,
    modifier: Modifier = Modifier,
    onKembali: (() -> Unit)? = null,
) {
    val konteks = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val zona = remember { ZoneId.systemDefault() }
    var dialogTambah by remember { mutableStateOf(false) }
    var itemDiedit by remember { mutableStateOf<ObatUiItem?>(null) }
    var itemDihapus by remember { mutableStateOf<ObatUiItem?>(null) }

    // Kesalahan dari ViewModel berupa UiText; teksnya baru diambil di lapisan UI.
    val kesalahan = state.kesalahan
    LaunchedEffect(kesalahan) {
        if (kesalahan != null) {
            snackbarHostState.showSnackbar(kesalahan.asString(konteks))
            onKesalahanDibersihkan()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.kelola_obat)) },
                navigationIcon = {
                    if (onKembali != null) {
                        IconButton(onClick = onKembali, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.aksi_kembali),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialogTambah = true },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.aksi_tambah_obat)) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = LebarMaksKonten)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                when {
                    state.memuat -> SedangMemuat()

                    state.daftar.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            ikon = Icons.AutoMirrored.Filled.List,
                            judul = stringResource(R.string.kelola_obat_kosong_judul),
                            penjelasan = stringResource(R.string.kelola_obat_kosong_pesan),
                            aksi = {
                                Button(onClick = { dialogTambah = true }) {
                                    Text(text = stringResource(R.string.aksi_tambah_obat))
                                }
                            },
                        )
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            // Ruang untuk FAB agar kartu terakhir tidak tertutup.
                            bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.daftar, key = { it.obat.id }) { item ->
                            ObatCard(
                                item = item,
                                teksKadaluwarsa = DateUtils.formatTanggal(item.obat.expiredDate, zona),
                                onRestock = { jumlah -> onRestock(item.obat.id, jumlah) },
                                onEdit = { itemDiedit = item },
                                onHapus = { itemDihapus = item },
                            )
                        }
                    }
                }
            }
        }
    }

    if (dialogTambah || itemDiedit != null) {
        ObatFormDialog(
            awal = if (dialogTambah) null else itemDiedit?.obat,
            zona = zona,
            onSimpan = { namaObat, stok, kadaluwarsa ->
                val target = itemDiedit
                if (target == null) {
                    onTambah(namaObat, stok, kadaluwarsa)
                } else {
                    onEdit(target.obat.id, namaObat, stok, kadaluwarsa)
                }
                dialogTambah = false
                itemDiedit = null
            },
            onDismiss = {
                dialogTambah = false
                itemDiedit = null
            },
        )
    }

    val targetHapus = itemDihapus
    if (targetHapus != null) {
        ConfirmDialog(
            judul = stringResource(R.string.hapus_obat_judul),
            pesan = stringResource(R.string.hapus_obat_pesan, targetHapus.obat.namaObat),
            teksKonfirmasi = stringResource(R.string.aksi_hapus),
            onKonfirmasi = {
                onHapus(targetHapus.obat.id)
                itemDihapus = null
            },
            onDismiss = { itemDihapus = null },
        )
    }
}

@Composable
private fun SedangMemuat() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
    }
}

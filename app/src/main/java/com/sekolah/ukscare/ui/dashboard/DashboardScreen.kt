package com.sekolah.ukscare.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.model.Pasien
import com.sekolah.ukscare.ui.AppViewModelProvider
import com.sekolah.ukscare.ui.asString
import com.sekolah.ukscare.ui.components.ConfirmDialog
import com.sekolah.ukscare.ui.components.EmptyState
import com.sekolah.ukscare.ui.components.PasienCard
import com.sekolah.ukscare.util.PesanGenerator
import com.sekolah.ukscare.util.WhatsAppShare

/** Lebar maksimum konten agar tetap nyaman dibaca di tablet (AGENTS.md 8). */
private val LebarMaksKonten = 720.dp

/** Ruang bawah daftar supaya kartu terakhir tidak tertutup FAB. */
private val PaddingBawahDaftar = 96.dp

/**
 * Layar Dashboard (AGENTS.md 6.1).
 * ViewModel disediakan [AppViewModelProvider] (DI manual, tanpa Hilt).
 *
 * State dikoleksi dengan `collectAsStateWithLifecycle` agar saat aplikasi di belakang
 * tidak ada lagi yang mengoleksi [DashboardViewModel.uiState] — dengan begitu detak
 * durasi ikut berhenti (lihat catatan pada ViewModel).
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onTambahPasien: () -> Unit,
    onBukaKelolaObat: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        state = state,
        onPilihTab = viewModel::selectTab,
        onUbahKueri = viewModel::setQuery,
        onSelesaikanIstirahat = viewModel::selesaikanIstirahat,
        onKesalahanDibersihkan = viewModel::bersihkanKesalahan,
        onTambahPasien = onTambahPasien,
        onBukaKelolaObat = onBukaKelolaObat,
        modifier = modifier,
    )
}

/** Bagian stateless dari layar; seluruh state dan aksi dikirim dari luar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onPilihTab: (TabDashboard) -> Unit,
    onUbahKueri: (String) -> Unit,
    onSelesaikanIstirahat: (Int) -> Unit,
    onKesalahanDibersihkan: () -> Unit,
    onTambahPasien: () -> Unit,
    onBukaKelolaObat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val konteks = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pasienDikonfirmasi by remember { mutableStateOf<Pasien?>(null) }

    // Kesalahan berupa UiText; teksnya baru diambil di lapisan UI ini.
    val kesalahan = state.kesalahan
    LaunchedEffect(kesalahan) {
        if (kesalahan != null) {
            snackbarHostState.showSnackbar(kesalahan.asString(konteks))
            onKesalahanDibersihkan()
        }
    }

    val judulBagikan = stringResource(R.string.share_judul_surat_izin)
    // Pesan dibuat ulang dari data pasien (overload Pasien di PesanGenerator).
    val bagikan: (Pasien, String) -> Unit = { pasien, teksWaktuMasuk ->
        WhatsAppShare.bagikan(
            context = konteks,
            pesan = PesanGenerator.buatPesan(pasien, teksWaktuMasuk),
            judul = judulBagikan,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    TombolKelolaObat(
                        jumlahBermasalah = state.ringkasan.obatBermasalah,
                        onClick = onBukaKelolaObat,
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onTambahPasien,
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = stringResource(R.string.aksi_pasien_baru)) },
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
                RingkasanDashboardBaris(ringkasan = state.ringkasan)
                PilihanTab(terpilih = state.tabTerpilih, onPilih = onPilihTab)

                when (state.tabTerpilih) {
                    TabDashboard.SEDANG_DIRAWAT -> DaftarAktif(
                        daftar = state.daftarAktif,
                        onBagikan = bagikan,
                        onMintaSelesaiIstirahat = { pasienDikonfirmasi = it },
                        onTambahPasien = onTambahPasien,
                    )

                    TabDashboard.RIWAYAT -> Column(modifier = Modifier.fillMaxSize()) {
                        KotakPencarian(kueri = state.kueriPencarian, onUbahKueri = onUbahKueri)
                        DaftarRiwayat(
                            daftar = state.daftarRiwayat,
                            kueri = state.kueriPencarian,
                            onBagikan = bagikan,
                        )
                    }
                }
            }
        }
    }

    val targetKonfirmasi = pasienDikonfirmasi
    if (targetKonfirmasi != null) {
        ConfirmDialog(
            judul = stringResource(R.string.selesai_istirahat_judul),
            pesan = stringResource(R.string.selesai_istirahat_pesan, targetKonfirmasi.namaSiswa),
            teksKonfirmasi = stringResource(R.string.aksi_selesai_istirahat),
            onKonfirmasi = {
                onSelesaikanIstirahat(targetKonfirmasi.id)
                pasienDikonfirmasi = null
            },
            onDismiss = { pasienDikonfirmasi = null },
            // Menyelesaikan istirahat bukan tindakan merusak data.
            destruktif = false,
        )
    }
}

/** Tiga angka ringkasan di bagian atas Dashboard. */
@Composable
private fun RingkasanDashboardBaris(ringkasan: RingkasanDashboard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KartuAngka(
            angka = ringkasan.sedangDirawat,
            label = stringResource(R.string.ringkasan_sedang_dirawat),
            modifier = Modifier.weight(1f),
        )
        KartuAngka(
            angka = ringkasan.kunjunganHariIni,
            label = stringResource(R.string.ringkasan_kunjungan_hari_ini),
            modifier = Modifier.weight(1f),
        )
        KartuAngka(
            angka = ringkasan.obatBermasalah,
            label = stringResource(R.string.ringkasan_obat_bermasalah),
            modifier = Modifier.weight(1f),
            menyorot = ringkasan.obatBermasalah > 0,
        )
    }
}

@Composable
private fun KartuAngka(
    angka: Int,
    label: String,
    modifier: Modifier = Modifier,
    menyorot: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = angka.toString(),
                style = MaterialTheme.typography.titleLarge,
                // Warna hanya penanda tambahan; angkanya sendiri sudah menyampaikan maksudnya.
                color = if (menyorot) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PilihanTab(terpilih: TabDashboard, onPilih: (TabDashboard) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        TabDashboard.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = terpilih == tab,
                onClick = { onPilih(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TabDashboard.entries.size,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(text = labelTab(tab))
            }
        }
    }
}

@Composable
private fun labelTab(tab: TabDashboard): String = when (tab) {
    TabDashboard.SEDANG_DIRAWAT -> stringResource(R.string.tab_sedang_dirawat)
    TabDashboard.RIWAYAT -> stringResource(R.string.tab_riwayat)
}

/** Tombol Kelola Obat di top bar, dengan badge jumlah obat bermasalah (tersembunyi bila 0). */
@Composable
private fun TombolKelolaObat(jumlahBermasalah: Int, onClick: () -> Unit) {
    val deskripsi = if (jumlahBermasalah > 0) {
        stringResource(R.string.kelola_obat_badge, jumlahBermasalah)
    } else {
        stringResource(R.string.kelola_obat)
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = deskripsi },
    ) {
        if (jumlahBermasalah > 0) {
            BadgedBox(badge = { Badge { Text(text = jumlahBermasalah.toString()) } }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null)
            }
        } else {
            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null)
        }
    }
}

/** Pencarian hanya relevan di tab Riwayat, jadi kotak ini hanya ada di tab tersebut. */
@Composable
private fun KotakPencarian(kueri: String, onUbahKueri: (String) -> Unit) {
    OutlinedTextField(
        value = kueri,
        onValueChange = onUbahKueri,
        label = { Text(text = stringResource(R.string.cari_riwayat)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = if (kueri.isEmpty()) {
            null
        } else {
            {
                IconButton(
                    onClick = { onUbahKueri("") },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.aksi_bersihkan_pencarian),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun DaftarAktif(
    daftar: List<PasienAktifUi>,
    onBagikan: (Pasien, String) -> Unit,
    onMintaSelesaiIstirahat: (Pasien) -> Unit,
    onTambahPasien: () -> Unit,
) {
    if (daftar.isEmpty()) {
        EmptyState(
            ikon = Icons.Filled.Person,
            judul = stringResource(R.string.dashboard_aktif_kosong_judul),
            penjelasan = stringResource(R.string.dashboard_aktif_kosong_pesan),
            aksi = {
                Button(
                    onClick = onTambahPasien,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.aksi_pasien_baru))
                }
            },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = PaddingBawahDaftar,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Key memakai id pasien agar animasi dan scroll tetap stabil saat data berubah.
        items(items = daftar, key = { it.pasien.id }) { item ->
            PasienCard(
                item = item,
                onBagikan = { onBagikan(item.pasien, item.teksWaktuMasuk) },
                onSelesaiIstirahat = { onMintaSelesaiIstirahat(item.pasien) },
            )
        }
    }
}

@Composable
private fun DaftarRiwayat(
    daftar: List<PasienRiwayatUi>,
    kueri: String,
    onBagikan: (Pasien, String) -> Unit,
) {
    if (daftar.isEmpty()) {
        // Tiga keadaan kosong yang berbeda: belum ada riwayat vs pencarian tidak ketemu.
        if (kueri.isBlank()) {
            EmptyState(
                ikon = Icons.AutoMirrored.Filled.List,
                judul = stringResource(R.string.dashboard_riwayat_kosong_judul),
                penjelasan = stringResource(R.string.dashboard_riwayat_kosong_pesan),
            )
        } else {
            EmptyState(
                ikon = Icons.Filled.Search,
                judul = stringResource(R.string.dashboard_pencarian_kosong_judul),
                penjelasan = stringResource(R.string.dashboard_pencarian_kosong_pesan, kueri),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = PaddingBawahDaftar,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = daftar, key = { it.pasien.id }) { item ->
            PasienCard(
                item = item,
                onBagikan = { onBagikan(item.pasien, item.teksWaktuMasuk) },
            )
        }
    }
}

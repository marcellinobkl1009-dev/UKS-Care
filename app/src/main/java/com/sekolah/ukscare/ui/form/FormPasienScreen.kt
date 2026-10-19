package com.sekolah.ukscare.ui.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sekolah.ukscare.R
import com.sekolah.ukscare.data.model.Obat
import com.sekolah.ukscare.ui.AppViewModelProvider
import com.sekolah.ukscare.ui.UiText
import com.sekolah.ukscare.ui.asString
import com.sekolah.ukscare.ui.components.MultiSelectChips
import com.sekolah.ukscare.util.WhatsAppShare

/** Lebar maksimum konten agar tetap nyaman dibaca di tablet (sama seperti Kelola Obat). */
private val LebarMaksKonten = 720.dp

/** Tinggi maksimum daftar pilihan agar menu tidak menutupi seluruh layar. */
private val TinggiMaksMenu = 320.dp

/**
 * Layar form pasien baru (AGENTS.md 6.2).
 * ViewModel disediakan [AppViewModelProvider]; NavGraph membatasinya pada
 * NavBackStackEntry destination ini, sehingga setiap pembukaan form mendapat instance
 * baru dan instance lama dibuang saat destination ditutup.
 */
@Composable
fun FormPasienScreen(
    modifier: Modifier = Modifier,
    onKembali: () -> Unit,
    viewModel: FormPasienViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FormPasienContent(
        state = state,
        onNama = viewModel::ubahNama,
        onKelas = viewModel::pilihKelas,
        onJenisKelamin = viewModel::pilihJenisKelamin,
        onKeluhan = viewModel::alihKeluhan,
        onTindakan = viewModel::alihTindakan,
        onObat = viewModel::pilihObat,
        onSimpan = viewModel::simpan,
        onErrorSimpanDibersihkan = viewModel::bersihkanErrorSimpan,
        modifier = modifier,
        onKembali = onKembali,
    )
}

/** Bagian stateless dari layar; seluruh state dan aksi dikirim dari luar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPasienContent(
    state: FormPasienUiState,
    onNama: (String) -> Unit,
    onKelas: (String) -> Unit,
    onJenisKelamin: (String) -> Unit,
    onKeluhan: (String) -> Unit,
    onTindakan: (String) -> Unit,
    onObat: (Int?) -> Unit,
    onSimpan: () -> Unit,
    onErrorSimpanDibersihkan: () -> Unit,
    modifier: Modifier = Modifier,
    onKembali: () -> Unit,
) {
    val konteks = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val fokusKelas = remember { FocusRequester() }

    // Kesalahan simpan berupa UiText (stok habis, kadaluwarsa, tidak ditemukan);
    // teksnya baru diambil di lapisan UI ini.
    val errorSimpan = state.errorSimpan
    LaunchedEffect(errorSimpan) {
        if (errorSimpan != null) {
            snackbarHostState.showSnackbar(errorSimpan.asString(konteks))
            onErrorSimpanDibersihkan()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.form_pasien_judul)) },
                navigationIcon = {
                    IconButton(onClick = onKembali, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.aksi_kembali),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Konten naik saat keyboard muncul sehingga field aktif dan tombol Simpan tetap terlihat.
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = LebarMaksKonten)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val kesalahanNama = state.kesalahan.namaSiswa
                OutlinedTextField(
                    value = state.namaSiswa,
                    onValueChange = onNama,
                    label = { Text(text = stringResource(R.string.field_nama_siswa)) },
                    singleLine = true,
                    isError = kesalahanNama != null,
                    supportingText = if (kesalahanNama != null) {
                        { PesanKesalahan(kesalahanNama) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    // Pindah ke pemilih kelas; field terakhir tidak perlu "Done"
                    // karena keluhan dan tindakan dipilih lewat chip.
                    keyboardActions = KeyboardActions(onNext = { fokusKelas.requestFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                PemilihKelas(
                    kelas = state.kelas,
                    daftarKelas = state.daftarKelas,
                    kesalahan = state.kesalahan.kelas,
                    onKelas = onKelas,
                    fokus = fokusKelas,
                )

                PemilihJenisKelamin(
                    terpilih = state.jenisKelamin,
                    pilihan = state.daftarJenisKelamin,
                    kesalahan = state.kesalahan.jenisKelamin,
                    onPilih = onJenisKelamin,
                )

                BagianPilihan(
                    judul = R.string.field_keluhan,
                    pilihan = state.daftarKeluhan,
                    terpilih = state.keluhan,
                    kesalahan = state.kesalahan.keluhan,
                    onToggle = onKeluhan,
                )

                BagianPilihan(
                    judul = R.string.field_tindakan,
                    pilihan = state.daftarTindakan,
                    terpilih = state.tindakan,
                    kesalahan = state.kesalahan.tindakan,
                    onToggle = onTindakan,
                )

                if (state.membutuhkanObat) {
                    PemilihObat(
                        daftarObat = state.daftarObat,
                        idTerpilih = state.idObatTerpilih,
                        kesalahan = state.kesalahan.obat,
                        onPilih = onObat,
                    )
                }

                Button(
                    onClick = onSimpan,
                    // Setelah tersimpan, tombol tidak dipakai lagi: satu form = satu pasien.
                    enabled = state.dapatDisimpan && state.drafPesan == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    if (state.sedangMenyimpan) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.form_sedang_menyimpan))
                    } else {
                        Text(text = stringResource(R.string.aksi_simpan))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    val draf = state.drafPesan
    if (draf != null) {
        // Draf diambil dari state sukses ViewModel sehingga isinya sama dengan yang disimpan.
        SheetSimpanBerhasil(draf = draf, onSelesai = onKembali)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PemilihKelas(
    kelas: String,
    daftarKelas: List<String>,
    kesalahan: UiText?,
    onKelas: (String) -> Unit,
    fokus: FocusRequester,
) {
    var terbuka by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = terbuka,
        onExpandedChange = { terbuka = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = kelas,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(R.string.field_kelas)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = terbuka) },
            isError = kesalahan != null,
            supportingText = if (kesalahan != null) {
                { PesanKesalahan(kesalahan) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fokus)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = terbuka,
            onDismissRequest = { terbuka = false },
            modifier = Modifier.heightIn(max = TinggiMaksMenu),
        ) {
            daftarKelas.forEach { nilai ->
                DropdownMenuItem(
                    text = { Text(text = nilai) },
                    onClick = {
                        onKelas(nilai)
                        terbuka = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PemilihJenisKelamin(
    terpilih: String,
    pilihan: List<String>,
    kesalahan: UiText?,
    onPilih: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JudulBagian(judul = R.string.field_jenis_kelamin)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            pilihan.forEachIndexed { index, nilai ->
                SegmentedButton(
                    selected = terpilih == nilai,
                    onClick = { onPilih(nilai) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = pilihan.size),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = nilai)
                }
            }
        }
        if (kesalahan != null) {
            PesanKesalahan(kesalahan)
        }
    }
}

@Composable
private fun BagianPilihan(
    @StringRes judul: Int,
    pilihan: List<String>,
    terpilih: Set<String>,
    kesalahan: UiText?,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        JudulBagian(judul = judul)
        MultiSelectChips(pilihan = pilihan, terpilih = terpilih, onToggle = onToggle)
        if (kesalahan != null) {
            PesanKesalahan(kesalahan)
        }
    }
}

/** Dropdown obat, hanya muncul saat tindakan "Diberi Obat" dipilih. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PemilihObat(
    daftarObat: List<Obat>,
    idTerpilih: Int?,
    kesalahan: UiText?,
    onPilih: (Int?) -> Unit,
) {
    if (daftarObat.isEmpty()) {
        // Pesan eksplisit supaya petugas tidak menunggu dropdown yang tidak akan muncul.
        Text(
            text = stringResource(R.string.form_obat_tidak_ada),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }

    var terbuka by remember { mutableStateOf(false) }
    val namaTerpilih = daftarObat.firstOrNull { it.id == idTerpilih }?.namaObat.orEmpty()

    ExposedDropdownMenuBox(
        expanded = terbuka,
        onExpandedChange = { terbuka = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = namaTerpilih,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(R.string.field_pilih_obat)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = terbuka) },
            isError = kesalahan != null,
            supportingText = if (kesalahan != null) {
                { PesanKesalahan(kesalahan) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = terbuka,
            onDismissRequest = { terbuka = false },
            modifier = Modifier.heightIn(max = TinggiMaksMenu),
        ) {
            daftarObat.forEach { obat ->
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(R.string.form_obat_stok, obat.namaObat, obat.stok))
                    },
                    onClick = {
                        // Menekan obat yang sama membatalkan pilihan.
                        onPilih(if (obat.id == idTerpilih) null else obat.id)
                        terbuka = false
                    },
                )
            }
        }
    }
}

/**
 * Lembar konfirmasi setelah pasien berhasil disimpan: menampilkan draf surat izin
 * apa adanya dan tombol pembagian (AGENTS.md 6.2, 6.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetSimpanBerhasil(
    draf: String,
    onSelesai: () -> Unit,
) {
    val konteks = LocalContext.current
    // Judul diambil di sini (bukan lewat konteks) agar tidak melanggar lint
    // LocalContextGetResourceValueCall dan tetap mengikuti bahasa perangkat.
    val judulBagikan = stringResource(R.string.share_judul_surat_izin)
    ModalBottomSheet(
        // Menutup lembar juga menutup form supaya pengguna tidak tertinggal pada form
        // yang sudah tersimpan (satu form = satu pasien).
        onDismissRequest = onSelesai,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.form_simpan_berhasil_judul),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.form_simpan_berhasil_pesan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.form_draf_surat),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
            ) {
                Text(
                    text = draf,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                )
            }
            Button(
                onClick = {
                    WhatsAppShare.bagikan(
                        context = konteks,
                        pesan = draf,
                        judul = judulBagikan,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.aksi_bagikan_whatsapp))
            }
            OutlinedButton(
                onClick = onSelesai,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.aksi_selesai))
            }
        }
    }
}

@Composable
private fun JudulBagian(@StringRes judul: Int) {
    Text(
        text = stringResource(judul),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PesanKesalahan(kesalahan: UiText) {
    Text(
        text = kesalahan.asString(LocalContext.current),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

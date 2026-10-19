**Bahasa Indonesia** | [English](README.en.md)

# UKS-Care

Aplikasi Android offline untuk Unit Kesehatan Sekolah (UKS): mencatat kunjungan pasien dengan cepat dan mengontrol stok obat.

## Fitur utama

- **Pencatatan pasien cepat:** isi nama, kelas, dan jenis kelamin, lalu pilih keluhan dan tindakan lewat chip. Tanpa NISN.
- **Surat izin & bagikan ke WhatsApp:** aplikasi menyusun surat izin istirahat dan membagikannya ke wali kelas atau guru piket lewat WhatsApp (ada pilihan aplikasi lain bila WhatsApp tidak terpasang).
- **Manajemen stok obat:** tambah, ubah, tambah stok, dan hapus obat; stok berkurang 1 otomatis setiap obat diberikan, dengan indikator warna (merah/kuning/hijau).
- **Status pasien & riwayat:** tab **Sedang Dirawat** dan **Riwayat**, lengkap dengan pencarian nama pada riwayat.

## Cara kerja singkat

1. Di Dashboard, tekan **+ Pasien Baru**.
2. Isi data pasien dan pilih keluhan serta tindakan. Bila memilih **Diberi Obat**, pilih obat dari daftar (hanya obat yang berstok dan belum kadaluwarsa).
3. Tekan **Simpan** — pasien tersimpan berstatus aktif dan stok obat berkurang 1 dalam satu transaksi.
4. Lembar konfirmasi muncul; kirim surat izin lewat **Share ke WhatsApp**, lalu tekan **Selesai** untuk kembali ke Dashboard.
5. Pasien tampil di tab **Sedang Dirawat**. Setelah pasien kembali ke kelas, tekan **Selesai Istirahat** dan setujui konfirmasi — waktu keluar tercatat dan pasien pindah ke **Riwayat**.

## Aturan status obat

| Kondisi | Warna | Label |
|---|---|---|
| stok < 5 | Merah | Stok Kritis |
| sudah lewat tanggal kadaluwarsa | Merah | Kadaluwarsa |
| kadaluwarsa dalam 30 hari | Kuning | Hampir Kadaluwarsa |
| selain itu | Hijau | Aman |

Obat yang kadaluwarsa atau berstok 0 tidak bisa dipilih saat memberi obat. Di **Kelola Obat**, obat bermasalah ditampilkan lebih dulu.

## Teknologi

Kotlin, Jetpack Compose (Material 3), Room (SQLite) dengan KSP, arsitektur MVVM (ViewModel + StateFlow + Repository), Navigation Compose, serta Coroutines dan Flow. Aplikasi berjalan 100% offline, tanpa login.

Versi yang dipatok di `gradle/libs.versions.toml`: Compose BOM 2026.06.01, Room 2.8.5, Navigation Compose 2.9.8, AGP 9.4.1, Kotlin 2.3.20.

## Kebutuhan dan cara menjalankan

- Android Studio (versi stable) dan **JDK 17**.
- Perangkat atau emulator Android dengan **minSdk 24**.
- `compileSdk` diset **36** (bukan 34) karena artefak AndroidX yang dipakai mensyaratkan `minCompileSdk 36`; `targetSdk` tetap 34.

```bash
./gradlew installDebug        # pasang ke perangkat/emulator
./gradlew testDebugUnitTest   # jalankan 74 unit test
```

Buka folder proyek di Android Studio, tunggu Gradle sync, pilih perangkat, lalu tekan **Run**.

## Menyesuaikan aplikasi

- **Daftar kelas:** `app/src/main/java/com/sekolah/ukscare/util/Constants.kt` → `DAFTAR_KELAS`. Bawaan untuk SMK: 42 kelas, tingkat X/XI/XII dengan kode jurusan AKL, MPLB, ULP, PM, TJKT, DKV, PPLG, dan AM (contoh `X AKL 1` sampai `XII AM`). Ubah di file ini bila struktur kelas sekolah berbeda.
- **Pilihan keluhan dan tindakan:** `app/src/main/java/com/sekolah/ukscare/util/Constants.kt` → `PILIHAN_KELUHAN` dan `PILIHAN_TINDAKAN`.
- **Teks antarmuka:** `app/src/main/res/values/strings.xml`.
- **Obat bawaan (data awal):** `app/src/main/java/com/sekolah/ukscare/data/local/AppDatabase.kt` → `SeedCallback` (5 obat, stok awal 10, kadaluwarsa 1 tahun setelah database dibuat).

## Data dan pencadangan

- Semua data tersimpan **hanya di perangkat**, pada database Room `ukscare.db`. Aplikasi tidak menyediakan fitur pencadangan otomatis.
- Data **hilang bila perangkat di-reset** atau aplikasi dihapus.
- Sebelum dipakai, ganti obat contoh dengan stok dan tanggal kadaluwarsa asli dari UKS.
- Catatan (belum diverifikasi): manifes menetapkan `android:allowBackup="true"`, sehingga Android dapat ikut mencadangkan data ke akun Google bila pencadangan sistem aktif. Jangan mengandalkan ini sebagai cadangan.

## Struktur proyek

```
com.sekolah.ukscare/
├── data/
│   ├── local/        AppDatabase, PasienDao, ObatDao
│   ├── model/        Pasien, Obat
│   └── repository/   PasienRepository, ObatRepository
├── ui/
│   ├── dashboard/    DashboardScreen, DashboardViewModel
│   ├── form/         FormPasienScreen, FormPasienViewModel
│   ├── obat/         KelolaObatScreen, ObatViewModel
│   ├── components/   PasienCard, ObatCard, StatusBadge, ...
│   ├── navigation/   NavGraph
│   └── theme/
├── util/             Constants, DateUtils, ObatStatus, PesanGenerator, WhatsAppShare
├── MainActivity.kt
└── UksCareApp.kt     Application class: membuat database dan repository
```

## Pengujian

74 unit test, semuanya jalan tanpa perangkat, dijalankan dengan `./gradlew testDebugUnitTest`. Cakupan:

- Aturan status obat, termasuk batas stok 4 vs 5 serta kadaluwarsa hari ini dan dalam 30 hari.
- Isi surat izin (`PesanGenerator`) dan Intent WhatsApp.
- Format tanggal (`DateUtils`).
- Transaksi penyimpanan pasien sekaligus pengurangan stok obat (Room in-memory).
- ViewModel Dashboard, Form, dan Obat.

## Di luar cakupan

Login atau multi-user, sinkronisasi cloud, NISN, notifikasi push, server backend, serta aplikasi untuk siswa atau orang tua.

## Dokumentasi lain

- [`AGENTS.md`](AGENTS.md) — spesifikasi teknis lengkap dan aturan kerja untuk AI coding agent.

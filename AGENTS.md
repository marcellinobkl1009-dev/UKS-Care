# AGENTS.md — UKS-Care (Staff/Admin Version)

Instructions for AI coding agents working on this repository. Read this file fully before writing any code.

## 1. Project Overview

UKS-Care is a standalone Android app for logging patient visits at a school health unit (UKS, *Unit Kesehatan Sekolah*) and controlling medicine stock. It runs 100% offline.

- **Users:** PMR (student Red Cross) members on duty or the UKS supervisor.
- **Device:** One shared school phone or tablet on the UKS desk.
- **Access:** Single-role. NO login, registration, or authentication. The app opens straight to the Dashboard.
- **Primary goal:** Fast patient entry (target: under 30 seconds per patient) and reliable medicine stock control.

## 2. Tech Stack (mandatory)

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) with KSP (not kapt) |
| Architecture | MVVM (ViewModel + StateFlow + Repository) |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| DI | Manual, via the Application class (no Hilt/Dagger) |
| SDK | minSdk 24, targetSdk 34, compileSdk 36 |
| Network | None. No internet, Firebase, or external APIs |

> **Note on `compileSdk`:** it is 36 (not 34) because the AndroidX artifacts we depend on publish `minCompileSdk=36` in their AAR metadata, which AGP enforces at build time. `minSdk 24` and `targetSdk 34` are unchanged.

Use a version catalog (`gradle/libs.versions.toml`). Pin stable library versions compatible with each other.

## 3. Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew testDebugUnitTest      # run unit tests
./gradlew lintDebug              # run lint
./gradlew installDebug           # install on connected device/emulator
```

Always run `assembleDebug` and `testDebugUnitTest` and fix all errors before declaring work complete.

## 4. Project Structure

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
├── util/             DateUtils, WhatsAppShare, Constants, PesanGenerator
├── MainActivity.kt
└── UksCareApp.kt     Application class: creates database and repositories
```

## 5. Database Schema (Room)

Use `@ColumnInfo(name = "...")` so column names are snake_case.

**Entity `Pasien`, table `pasien`**

| Field | Type | Column | Notes |
|---|---|---|---|
| id | Int | id | PK, autoGenerate |
| namaSiswa | String | nama_siswa | |
| kelas | String | kelas | |
| jenisKelamin | String | jenis_kelamin | "Laki-laki" / "Perempuan" |
| keluhan | String | keluhan | selected complaints joined by comma |
| tindakan | String | tindakan | selected actions joined by comma |
| obatDiberikan | String? | obat_diberikan | medicine name, nullable |
| waktuMasuk | Long | waktu_masuk | epoch millis |
| waktuKeluar | Long? | waktu_keluar | null until "Selesai Istirahat" |
| status | String | status | "AKTIF" / "SELESAI" |

**Entity `Obat`, table `obat`**

| Field | Type | Column | Notes |
|---|---|---|---|
| id | Int | id | PK, autoGenerate |
| namaObat | String | nama_obat | |
| stok | Int | stok | never negative |
| expiredDate | Long | expired_date | epoch millis |

DAO rules:
- List queries return `Flow`.
- Insert/update/delete are `suspend` functions.
- Saving a patient and decrementing medicine stock MUST be a single `@Transaction`.

Seed data on first creation (via `RoomDatabase.Callback`): Paracetamol, Antasida, Betadine, Plester, Minyak Kayu Putih.

## 6. Features

### 6.1 Dashboard
- Summary: patients currently being treated, visits today, number of critical/expired medicines.
- Tabs/segmented button: **Sedang Dirawat** (AKTIF) and **Riwayat** (SELESAI).
- Patient card: name, class, complaints, entry time, running rest duration (AKTIF), exit time (SELESAI).
- **Selesai Istirahat** button on AKTIF cards: sets `waktuKeluar` = now, `status` = SELESAI, after a short confirmation dialog.
- FAB **+ Pasien Baru** opens the form.
- Icon to open Kelola Obat, with a badge when any medicine has a problem.
- Search by student name in Riwayat.
- Friendly empty states.
- Share button on each card to re-send the permission message.

### 6.2 Patient Form
- Nama Siswa: text field (required).
- Kelas: `ExposedDropdownMenu`. The class list lives in `Constants` so it is easy to replace (default VII-A..IX-E, junior high).
- Jenis Kelamin: SegmentedButton or RadioButton.
- Keluhan: multi-select FilterChips: Pusing, Demam, Maag, Luka, Mual, Sakit Kepala, Lainnya.
- Tindakan: multi-select FilterChips: Istirahat, Diberi Obat, Kompres, Dibalut/P3K.
- If "Diberi Obat" is selected, show a **Pilih Obat** dropdown listing only medicines with stock > 0 and not expired, with remaining stock.
- Validation (inline errors): name, class, gender, at least 1 complaint, at least 1 action.
- **Simpan:** saves with status AKTIF and `waktuMasuk` = now; if a medicine is chosen, stock -1 in the same transaction; reject if stock is 0.
- After saving, show a confirmation sheet with the message draft and a **Share ke WhatsApp** button.

### 6.3 Permission Slip and WhatsApp Share
Message template (Indonesian):

```
*SURAT IZIN ISTIRAHAT - UKS*

Kepada Yth. Wali Kelas / Guru Piket
Kelas: {kelas}

Dengan hormat, siswa berikut sedang berada di UKS:
Nama: {nama}
Kelas: {kelas}
Keluhan: {keluhan}
Tindakan: {tindakan}
Obat diberikan: {obat atau "-"}
Waktu masuk: {hari, tanggal, jam}

Mohon izin agar siswa tersebut dapat beristirahat di UKS. Terima kasih.

Petugas UKS
```

- Use `Intent.ACTION_SEND` (text/plain) with `setPackage("com.whatsapp")`.
- If WhatsApp is missing, fall back to `Intent.createChooser`.
- Declare `<queries>` for `com.whatsapp` in `AndroidManifest.xml` (Android 11+).

### 6.4 Kelola Obat
- List: name, stock, expiry date.
- Status indicator (color AND text label, for accessibility):

| Condition | Color | Label |
|---|---|---|
| stock < 5 | Red | Stok Kritis |
| already expired | Red | Kadaluwarsa |
| expires within 30 days | Yellow/Orange | Hampir Kadaluwarsa |
| otherwise | Green | Aman |

- Add medicine (dialog/bottom sheet): name, initial stock, expiry date (Material 3 DatePicker).
- Edit medicine and quick restock (+/- or quantity input).
- Delete with confirmation dialog.
- Sort so problematic (red) medicines appear first.

## 7. Business Rules

1. Stock must never go negative. Validate in both the repository and the UI.
2. Expired medicines must not be selectable when dispensing.
3. Patient save and stock decrement happen in ONE Room transaction.
4. Store all times as epoch millis; display with `Locale("id", "ID")`, e.g. `Senin, 19 Okt 2026 • 09.30`.
5. All UI text is Indonesian and lives in `strings.xml`.

## 8. Code Conventions

- Idiomatic Kotlin; follow the official Kotlin style guide.
- Keep UI state in data classes, separate from logic. Expose `StateFlow` from ViewModels.
- Composables are stateless where possible (state hoisting).
- Handle loading and error states explicitly.
- Short comments in Indonesian on non-obvious logic; code identifiers stay as defined in the schema.
- Touch targets at least 48dp. Material 3 theme in green/teal with dark mode support. Light animation only.
- Layout must work on both phone and tablet.
- No hardcoded user-facing strings in Kotlin files.

## 9. Testing

Write JUnit unit tests for:
- Medicine status logic (red / yellow / green, including boundary values: stock 4 and 5, expiry today and in 30 days).
- `PesanGenerator` output (with and without a medicine).

Keep date logic injectable (pass the current time as a parameter) so tests are deterministic.

## 10. Out of Scope (do not build)

Login or multi-user, cloud sync, NISN, push notifications, backend server, student/parent-facing app.

## 11. Working Agreement

1. Present a short plan (files to create) before coding.
2. Build in this order: Gradle/Manifest, data layer, utilities and ViewModels, UI screens, NavGraph and MainActivity.
3. Run the build and tests; fix errors before reporting completion.
4. Do not add dependencies or features beyond this document without noting them as assumptions.
5. At the end, report: feature summary, how to run, and a list of assumptions.

## 12. Definition of Done

- Project builds without errors and runs on an emulator/device.
- End-to-end flow works: add patient → stock decreases → share to WhatsApp → finish rest → patient moves to Riwayat.
- Medicine indicators follow the rules in section 6.4.
- Data persists after the app is closed and works with no internet.
- Unit tests pass.

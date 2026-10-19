[Bahasa Indonesia](README.md) | **English**

# UKS-Care

An offline Android app for the school health unit (UKS): it logs patient visits quickly and keeps medicine stock under control.

## Main features

- **Fast patient entry:** enter name, class, and gender, then tap complaint and action chips. No NISN needed.
- **Permission slip & WhatsApp share:** the app builds a rest permission slip and shares it to the class teacher or duty teacher via WhatsApp (with other apps offered as a fallback if WhatsApp is not installed).
- **Medicine stock control:** add, edit, restock, and delete medicines; stock drops by 1 automatically each time medicine is given, with color indicators (red/yellow/green).
- **Patient status & history:** **Sedang Dirawat** (active) and **Riwayat** (finished) tabs, with name search in the history tab.

## How it works

1. On the Dashboard, tap **+ Pasien Baru**.
2. Fill in the patient's details and pick their complaints and actions. If you choose **Diberi Obat** (medicine given), pick a medicine from the list (only medicines with stock and not expired).
3. Tap **Simpan** (save) — the patient is saved as active and the medicine stock drops by 1 in a single transaction.
4. A confirmation sheet appears; send the slip via **Share ke WhatsApp**, then tap **Selesai** to return to the Dashboard.
5. The patient appears in the **Sedang Dirawat** tab. Once the student returns to class, tap **Selesai Istirahat** (finish resting) and confirm — the exit time is recorded and the patient moves to **Riwayat**.

## Medicine status rules

| Condition | Color | Label |
|---|---|---|
| stock < 5 | Red | Stok Kritis |
| past its expiry date | Red | Kadaluwarsa |
| expires within 30 days | Yellow | Hampir Kadaluwarsa |
| otherwise | Green | Aman |

Expired medicines or those with 0 stock cannot be selected when giving medicine. In **Kelola Obat**, medicines with problems are listed first.

## Tech stack

Kotlin, Jetpack Compose (Material 3), Room (SQLite) with KSP, MVVM architecture (ViewModel + StateFlow + Repository), Navigation Compose, and Coroutines and Flow. The app runs fully offline, with no login.

Pinned versions in `gradle/libs.versions.toml`: Compose BOM 2026.06.01, Room 2.8.5, Navigation Compose 2.9.8, AGP 9.4.1, Kotlin 2.3.20.

## Requirements and how to run

- Android Studio (stable) and **JDK 17**.
- An Android device or emulator with **minSdk 24**.
- `compileSdk` is set to **36** (not 34) because the AndroidX artifacts we use require `minCompileSdk 36`; `targetSdk` stays at 34.

```bash
./gradlew installDebug        # install on a device/emulator
./gradlew testDebugUnitTest   # run the 74 unit tests
```

Open the project folder in Android Studio, wait for Gradle sync, select a device, then press **Run**.

## Customizing the app

- **Class list:** `app/src/main/java/com/sekolah/ukscare/util/Constants.kt` → `DAFTAR_KELAS`. The default is a vocational high school (SMK) list of 42 classes: levels X/XI/XII with major codes AKL, MPLB, ULP, PM, TJKT, DKV, PPLG, and AM (for example `X AKL 1` to `XII AM`). Edit this file if your school's classes differ.
- **Complaint and action options:** `app/src/main/java/com/sekolah/ukscare/util/Constants.kt` → `PILIHAN_KELUHAN` and `PILIHAN_TINDAKAN`.
- **UI text:** `app/src/main/res/values/strings.xml`.
- **Seed medicines:** `app/src/main/java/com/sekolah/ukscare/data/local/AppDatabase.kt` → `SeedCallback` (5 medicines, initial stock 10, expiry 1 year after the database is created).

## Data and backup

- All data is stored **only on the device**, in the Room database `ukscare.db`. The app has no built-in automatic backup.
- Data is **lost if the device is reset** or the app is uninstalled.
- Before use, replace the sample medicines with the real stock and expiry dates from the UKS.
- Note (not verified): the manifest sets `android:allowBackup="true"`, so Android may also back up app data to a Google account when system backup is on. Do not rely on this as a backup.

## Project structure

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
└── UksCareApp.kt     Application class: creates the database and repositories
```

## Testing

74 unit tests, all runnable without a device, via `./gradlew testDebugUnitTest`. Coverage:

- Medicine status rules, including the stock 4 vs 5 boundary and expiry today and in 30 days.
- Permission slip text (`PesanGenerator`) and the WhatsApp Intent.
- Date formatting (`DateUtils`).
- The patient-save transaction together with the stock decrement (in-memory Room).
- The Dashboard, Form, and Obat ViewModels.

## Out of scope

Login or multi-user, cloud sync, NISN, push notifications, a backend server, and student- or parent-facing apps.

## Other documentation

- [`AGENTS.md`](AGENTS.md) — the full technical spec and working rules for AI coding agents.

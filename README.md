# UKS-Care

An offline Android app for school health units (UKS) to log patient visits quickly and keep medicine stock under control. This is the **Staff/Admin version**, intended for PMR members on duty or the UKS supervisor.

## Features

- **Fast patient entry:** name, class, gender, then tap complaint and action chips. No NISN needed.
- **Permission slip generator:** creates an official message and shares it to the class teacher or duty teacher via WhatsApp.
- **Medicine stock control:** stock drops by 1 automatically each time medicine is given. Red/yellow/green indicators flag critical stock (< 5) and near or past expiry.
- **Patient status and history:** separate lists for patients currently resting and those who have returned to class, with an automatic exit time.

## Tech Stack

Kotlin, Jetpack Compose (Material 3), Room (SQLite), MVVM, Navigation Compose, Coroutines and Flow. Fully offline, no login.

## Requirements

- Android Studio (latest stable)
- JDK 17
- Android device or emulator, API 24 or higher

## Run the Project

1. Open the project folder in Android Studio and let Gradle sync.
2. Select a device or emulator.
3. Press **Run**, or from the terminal:

```bash
./gradlew installDebug
```

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

## Customize

- **Class list:** edit the list in `util/Constants.kt` (default VII-A to IX-E, junior high).
- **Complaint and action options:** also in `util/Constants.kt`.
- **UI text:** `res/values/strings.xml` (Indonesian).

## Medicine Status Rules

| Condition | Color | Label |
|---|---|---|
| Stock below 5 | Red | Stok Kritis |
| Already expired | Red | Kadaluwarsa |
| Expires within 30 days | Yellow | Hampir Kadaluwarsa |
| Otherwise | Green | Aman |

## Project Docs

- [`AGENTS.md`](AGENTS.md): full technical spec and rules for AI coding agents.

## Out of Scope

Login or multi-user, cloud sync, push notifications, backend server, and student or parent-facing apps.

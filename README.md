# FieldFlow

FieldFlow is a standalone Android inspection and maintenance prototype for assets, locations, checklist templates, inspection execution, issues, and report export.

Architecture: **Circuit-Based Feature-Modular Clean Architecture**.

The checked-in source, Gradle files, and architecture docs describe the current implementation. The proposal PDF is product context and includes future-scope ideas that are not all implemented.

## Overview

- Android application module: `:app`
- Application ID: `com.topic11.cs426`
- Current runtime: Room-backed, offline-first local app
- Database: `FieldFlowDatabase`, Room schema version 3
- Presentation: Jetpack Compose with Slack Circuit screen, presenter, state, event, and UI contracts
- Runtime services: no production backend, no login, no cloud database, and no runtime secret configuration

## Current Features

Implemented in the current repository:

- Dashboard with inspection summaries, overview metrics, status filters, quick actions, start-inspection flow, and Settings entry.
- Editable Inspection workflow with autosave, Save Draft, section navigation, review, validation, completion, score display, issue creation, notes, and camera/gallery evidence capture through the app boundary.
- Room-backed Assets workflow with list, search, detail, add/edit, location selection, validation, and start-inspection handoff.
- Room-backed Locations workflow with list, search, detail, add/edit, validation, and non-destructive handling for asset associations.
- Room-backed Templates workflow with list, search, detail, add template with one initial section/item, metadata editing for existing templates, validation, and start-inspection handoff.
- Room-backed Issues workflow with list filters, detail, inspection/asset context, and Domain-validated status transitions.
- Room-backed Reports workflow with completed-inspection candidates, generated report detail, JSON export, PDF export, persisted export history, and Android open/share actions.
- Settings workflow for System, Light, and Dark appearance modes, persisted in app preferences and applied at the theme root.
- App-scoped sample-data seeding and a `PendingSyncDrain` loop that consumes the durable `pending_sync` queue through `FakeRemoteSyncAdapter`.

Current prototype limitations:

- No FieldFlow account, authentication, production backend, production cloud sync, email delivery, push notifications, report scheduling, AI service, QR/GPS workflow, or remote conflict resolution.
- Existing template editing changes metadata only. Full multi-section/item authoring and active/archive lifecycle controls remain future scope.
- Report open/share depends on another installed app that can handle the exported MIME type.

## Architecture

FieldFlow uses **Circuit-Based Feature-Modular Clean Architecture**:

- `:domain` owns pure Kotlin business models, repository ports, validation, scoring, lifecycle, report, issue, scheduling, and appearance preference use cases.
- `:data` implements Domain ports using Room mappings, local file storage, report exporters, sample seeding, and fake remote-sync behavior.
- `:core:database` owns Room entities, DAOs, migrations, and exported schemas.
- `:core:navigation` owns typed Slack Circuit `Screen` contracts.
- `:core:designsystem` owns shared Compose Material 3 UI primitives.
- `:feature:*` modules own presentation for their feature slice.
- `:app` is the Android composition root. It opens Room with migrations, builds repositories and use cases, assembles Circuit factories, wires evidence capture and report open/share actions, applies appearance preferences, seeds sample data, and starts pending-sync draining.

Dependency direction:

```text
:app -> :data, :domain, :core:database, :core:navigation, :core:designsystem
:app -> :feature:dashboard, :feature:assets, :feature:templates
:app -> :feature:inspection, :feature:issues, :feature:locations
:app -> :feature:reports, :feature:settings

:feature:* -> :domain, :core:navigation, :core:designsystem
:data      -> :domain, :core:database
:domain    -> Kotlin and Coroutines only
```

Feature modules must not depend on `:data` or `:core:database`. `:domain` must not import Android, Compose, Circuit, Room, app, feature, or data code.

## Repository Modules

`settings.gradle.kts` currently includes 15 Gradle modules, including 8 feature modules.

| Module | Responsibility |
| --- | --- |
| `:app` | Android application, process-scoped composition root, Circuit assembly, Room runtime wiring, evidence capture bridge, report open/share bridge, theme root |
| `:domain` | Pure Kotlin business rules, models, repository/export/storage ports, and use cases |
| `:data` | Room repositories, mappers, sample seeding, evidence storage, JSON/PDF report exporters, fake remote-sync adapter |
| `:core:navigation` | Typed Slack Circuit screen contracts |
| `:core:database` | Room database version 3, entities, DAOs, migrations, exported schemas, database tests |
| `:core:designsystem` | Shared Compose Material 3 theme and UI primitives |
| `:core:testing` | Test fakes and fixtures for module tests |
| `:feature:dashboard` | Dashboard presentation |
| `:feature:assets` | Asset list, detail, editor, and start-inspection presentation |
| `:feature:templates` | Template list, detail, editor, and start-inspection presentation |
| `:feature:inspection` | Inspection workflow presentation |
| `:feature:issues` | Maintenance issue list/detail presentation |
| `:feature:locations` | Location list/detail/editor presentation |
| `:feature:reports` | Report list/detail/export presentation |
| `:feature:settings` | Appearance settings presentation |

## Required Tools

Use the checked-in Gradle wrapper. A separate global Gradle installation is not required.

| Tool | Requirement |
| --- | --- |
| Git | Required to clone and update the repository. |
| Operating system | Local development is supported through Android Studio on Windows, macOS, or Linux. CI runs on Ubuntu 24.04. The repository does not pin a local OS version. |
| Android Studio | A current Android Studio version compatible with AGP `9.2.1`, Gradle `9.4.1`, JDK 21, and Android SDK `android-36.1`. The repository does not pin an Android Studio release. |
| JDK | JDK 21. `gradle/gradle-daemon-jvm.properties` pins toolchain version 21, and CI uses Temurin 21. |
| Android SDK Platform | Android SDK platform `android-36.1`, from `compileSdk { version = release(36) { minorApiLevel = 1 } }`. |
| Android SDK Build Tools | Not explicitly pinned in this repository. Install/update through SDK Manager if Android Studio or AGP requests them. |
| Android SDK Platform Tools | Required for device detection and command-line install. Version is not pinned. |
| Android Emulator or physical Android device | Required to launch the app and run connected/instrumented tests. Not required for JVM/unit tests, lint, or debug assembly. |
| Gradle wrapper | Included as `gradlew`, `gradlew.bat`, and wrapper distribution `9.4.1`. |
| PowerShell | Required only for `scripts/agent/verify.ps1`. Direct Gradle wrapper commands work without PowerShell. |

Internet access may be needed during first setup so Gradle can download dependencies, the Gradle wrapper distribution, the JDK toolchain, and Android SDK or managed-device artifacts.

## Major Libraries

Project libraries are declared in Gradle files and resolved automatically. Do not install these libraries manually.

| Library or plugin | Version | Used for |
| --- | --- | --- |
| Gradle wrapper | `9.4.1` | Repository build execution |
| Android Gradle Plugin | `9.2.1` | Android app/library builds |
| Kotlin | `2.2.10` | Kotlin JVM and Android source |
| KSP | `2.3.10` | Room annotation processing |
| Jetpack Compose BOM | `2026.02.01` | Compose UI dependency alignment |
| AndroidX Activity Compose | `1.13.0` | Compose activity integration and activity-result based flows |
| Room | `2.8.4` | Local database, DAOs, migrations, exported schemas |
| Slack Circuit | `0.33.1` | Screen contracts, presenters, UI factories, navigation, presenter tests |
| Kotlin Coroutines | `1.10.2` | Flows, app-scoped background work, async use cases |
| JUnit / Robolectric / AndroidX Test / Espresso | `4.13.2` / `4.16` / `1.7.0` / `3.7.0` | JVM, Robolectric, and Android instrumentation testing |

## Installation and Configuration

1. Clone the repository.

   ```powershell
   git clone https://github.com/qthangus78/CS426---Project
   cd CS426---Project
   ```

2. Open the repository root in Android Studio. Open the folder that contains `settings.gradle.kts`, not an individual module folder.

3. Select JDK 21 for Gradle. In Android Studio, use Settings or Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK, then choose a JDK 21 installation.

4. Install Android SDK components with SDK Manager:

   - Android SDK Platform `android-36.1`;
   - Android SDK Platform Tools;
   - Android Emulator if you will run an emulator;
   - SDK Build Tools if Android Studio or AGP requests them;
   - for the Gradle-managed instrumented suite, the Pixel 6 / API 35 / `aosp-atd` system image may be downloaded by Gradle on first use.

5. Let Gradle Sync complete. The first sync can download Gradle, Android, Kotlin, Compose, Room, Circuit, and test dependencies from the repositories configured in `settings.gradle.kts`.

6. Configure `local.properties` if Android Studio did not create it automatically. This file contains only the machine-specific Android SDK path and must remain untracked.

   Windows example:

   ```properties
   sdk.dir=C\:\\Users\\your-name\\AppData\\Local\\Android\\Sdk
   ```

   Linux/macOS example:

   ```properties
   sdk.dir=/home/your-name/Android/Sdk
   ```

   FieldFlow does not require API keys, access tokens, passwords, client secrets, or private certificates in `local.properties`.

7. Verify that Gradle can discover the project.

   PowerShell:

   ```powershell
   .\gradlew.bat projects --no-daemon
   ```

   Unix shell:

   ```sh
   ./gradlew projects --no-daemon
   ```

## Running the Application

Android Studio is the recommended way to launch the application during development.

### Run with Android Studio

1. Start an Android emulator or connect a physical Android device.
2. For a physical device, enable Developer options and USB debugging.
3. Select the `app` run configuration.
4. Select the target device.
5. Click Run.

### Build and run from the command line

Build the debug APK without launching a device:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
```

Unix shell:

```sh
./gradlew :app:assembleDebug --no-daemon
```

Install the debug APK only after you have started an emulator or connected a USB-debuggable physical device:

```powershell
.\gradlew.bat :app:installDebug --no-daemon
```

`assembleDebug` builds the app. `installDebug` needs a running target device. The Gradle-managed device is primarily for instrumented tests, not normal product use.

Generated APKs are written under ignored `build/` directories. Do not commit generated APKs or other build outputs.

## Testing and Verification

Replace `.\gradlew.bat` with `./gradlew` on Unix-like systems.

### Project discovery

No device required:

```powershell
.\gradlew.bat projects --no-daemon
```

### Unit tests

No device required. Targeted examples:

```powershell
.\gradlew.bat :domain:test --no-daemon
.\gradlew.bat :data:testDebugUnitTest --no-daemon
.\gradlew.bat :core:database:testDebugUnitTest --no-daemon
.\gradlew.bat :feature:dashboard:testDebugUnitTest --no-daemon
.\gradlew.bat :feature:inspection:testDebugUnitTest --no-daemon
```

All local JVM/Robolectric unit tests:

```powershell
.\gradlew.bat test --no-daemon
```

### Lint and build

No device required:

```powershell
.\gradlew.bat lintDebug test assembleDebug :app:assembleDebugAndroidTest --no-daemon
```

This is the same fast build/lint/test gate used by CI. `:app:assembleDebugAndroidTest` compiles the instrumented test APK but does not boot a device.

### Connected device tests

Device or emulator required:

```powershell
.\gradlew.bat connectedDebugAndroidTest --no-daemon
```

Use this only after you have started an emulator or connected a physical device with USB debugging enabled.

### Gradle-managed-device tests

The app module declares a Gradle-managed device named `ciDevice`:

- model: Pixel 6;
- API level: 35;
- system image: `aosp-atd`;
- test task: `:app:ciDeviceDebugAndroidTest`.

CI provisions the managed device with:

```powershell
.\gradlew.bat :app:ciDeviceSetup --no-daemon --stacktrace `
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect `
  -Pandroid.experimental.testOptions.managedDevices.setupTimeoutMinutes=30
```

Run the managed-device suite:

```powershell
.\gradlew.bat :app:ciDeviceDebugAndroidTest --no-daemon --stacktrace `
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect `
  -Pandroid.experimental.testOptions.managedDevices.setupTimeoutMinutes=30
```

Gradle or the Android SDK may download the required system image on first use. Reports are generated under:

```text
app/build/reports/androidTests/managedDevice/
app/build/outputs/androidTest-results/managedDevice/
```

### Repository verification script

The PowerShell helper selects the expected Gradle tasks for a changed path. For documentation-only changes, `-Plan` reports that no Gradle verification is selected.

```powershell
.\scripts\agent\verify.ps1 -Plan -Path README.md
.\scripts\agent\verify.ps1 -Full
```

## Device Requirements

- An Android emulator or physical Android device is required to launch FieldFlow.
- A device is not required for normal JVM/unit tests, lint, or APK assembly.
- The app has `minSdk = 24`, `targetSdk = 36`, and compile SDK `android-36.1`.
- Command-line installation to a physical device requires USB debugging and Android SDK Platform Tools.
- Evidence capture uses Android activity result contracts for `TakePicturePreview` and `GetContent`. Camera testing needs a camera-capable emulator/device or compatible camera app. Gallery testing needs a content/gallery provider with images.
- The app manifest does not declare dangerous permissions such as `CAMERA`, media-read permissions, or `INTERNET`.
- Report open/share uses a `FileProvider` and Android `ACTION_VIEW` / `ACTION_SEND`; it needs another app on the device that can handle the exported JSON or PDF.
- The Gradle-managed instrumented suite uses a Pixel 6 API 35 `aosp-atd` image. This image does not include Google Play Services.

## Account and External-Service Requirements

No runtime account or external production service is required for the current prototype.

Verified from dependencies, manifest, and source:

- No FieldFlow account is required.
- No Google account is required.
- No Firebase project is required.
- No backend server is required.
- No cloud database is required.
- No external REST API is required.
- No API key, access token, client secret, or private certificate is required.
- No external AI service, e-mail service, push service, or production remote synchronization service is required.
- No Google Play Services dependency is declared.

The app is offline-first and uses Room as the local source of truth. `PendingSyncDrain` consumes the local `pending_sync` queue with `FakeRemoteSyncAdapter`; it simulates remote synchronization state and does not contact a production service.

Internet access may still be needed during initial Gradle dependency resolution, JDK toolchain resolution, Android SDK setup, or managed-device system-image download. That is build-time setup, not an app-runtime service requirement.

The manifest enables normal Android backup/device-transfer rules, but backup is OS-managed and not required to use FieldFlow.

## Local Data and Offline Behavior

- Room stores local app data in `fieldflow.db`.
- Room schema JSON files under `core/database/schemas/` are intentionally versioned for migration verification.
- Sample catalog and inspection data are seeded asynchronously when the app composition root starts.
- Evidence files are copied into app-managed internal storage through `AndroidEvidenceStore`.
- Report exports are written into app-managed internal `reports/` storage, and successful exports are recorded in Room report history.
- Completing an inspection marks sync state locally and enqueues `pending_sync`; the app-scoped drain loop settles it through the fake adapter with exponential backoff and a bounded attempt count.
- Uninstalling or clearing app data removes local Room data, app-managed evidence files, and app-managed report files.

## Security and Repository Hygiene

Do not commit:

- passwords, API keys, access tokens, client secrets, real credentials, or service-account files;
- private certificates, private signing keystores, or signing passwords;
- machine-specific SDK configuration;
- generated APK/AAB files;
- generated test reports;
- module `build/` directories or unnecessary generated files.

`.gitignore` intentionally ignores examples such as:

- `local.properties`;
- `.gradle/`;
- `/build` and `**/build/`;
- `/captures`;
- `*.apk`, `*.ap_`, and `*.aab`;
- `*.jks`, `*.keystore`, and signing property files;
- temporary editor files.

Room exported schema JSON files are an intentional checked-in generated artifact because Room migration tests use them. Do not remove them as ordinary build output.

## Continuous Integration

`.github/workflows/android-ci.yml` runs on every push and pull request to `main`.

| Job | Runner | Main command |
| --- | --- | --- |
| Build and lint | `ubuntu-24.04`, Temurin JDK 21 | `./gradlew lintDebug test assembleDebug :app:assembleDebugAndroidTest --no-daemon --stacktrace` |
| Instrumented tests | `ubuntu-24.04`, Temurin JDK 21, KVM-enabled emulator | `./gradlew :app:ciDeviceDebugAndroidTest --no-daemon --stacktrace -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect -Pandroid.experimental.testOptions.managedDevices.setupTimeoutMinutes=30` |

The instrumented job first runs `:app:ciDeviceSetup`, caches the managed device/system image, and uploads managed-device reports from `app/build/reports/androidTests/managedDevice/` and `app/build/outputs/androidTest-results/managedDevice/`.

## Team Ownership

| Module or area | Normal approver | Responsibility |
| --- | --- | --- |
| Root Gradle, `:app`, `:core:navigation`, `:feature:inspection` | Thang | Integration, Circuit foundation, composition root, inspection UI |
| `:domain` | Huy | Business contracts, validation, scoring, lifecycle, Domain tests |
| `:data`, `:core:database` | Linh | Persistence, adapters, mappings, evidence, sync, database tests |
| `:feature:dashboard`, `:feature:reports`, `:core:designsystem`, docs/demo | Linh | Dashboard, reports workflow, design system, documentation |
| `:feature:assets`, `:feature:locations`, `:feature:templates`, `:feature:issues`, `:feature:settings` | Assigned later | Assets, locations, templates, issue lifecycle, and settings workflows |

Detailed ownership rules: [docs/architecture/TEAM_OWNERSHIP.md](docs/architecture/TEAM_OWNERSHIP.md).

## Related Documentation

- [Project proposal](docs/FieldFlow_Project_Proposal.pdf)
- [Module graph](docs/architecture/MODULE_GRAPH.md)
- [Data schema](docs/architecture/DATA_SCHEMA.md)
- [Presentation structure](docs/architecture/PRESENTATION_STRUCTURE.md)
- [Team ownership](docs/architecture/TEAM_OWNERSHIP.md)
- [Database module](core/database/README.md)
- [Demo script](docs/demo/DEMO_SCRIPT.md)
- [Manual test checklist](docs/demo/MANUAL_TEST_CHECKLIST.md)
- [Final manual acceptance checklist](docs/demo/FINAL_MANUAL_ACCEPTANCE_CHECKLIST.md)

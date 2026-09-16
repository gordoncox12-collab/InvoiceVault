# InvoiceVault

Offline-first Android invoicing for South African small businesses. Everything lives on the phone: businesses, customers, invoices, receipts, Excel sheets, pictures, notes, handwritten signatures, and PDFs. There is no backend and no login.

This repository **is** the Android Gradle project (`settings.gradle.kts` at the repo root).

## Phone install (debug APK)

GitHub Releases on this **public** repo give a direct APK URL that works without a GitHub account. Private-repo release links 404 and save as HTML, which Android then refuses to install.

1. Open the latest [Release](https://github.com/gordoncox12-collab/InvoiceVault/releases).
2. Download **InvoiceVault-debug.apk**.
3. On the phone: allow install from the browser, then open the file.
4. Direct asset URL (once the `v0.1.0-debug` release exists):

`https://github.com/gordoncox12-collab/InvoiceVault/releases/download/v0.1.0-debug/InvoiceVault-debug.apk`

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Koala / Ladybug or newer is fine).
2. **File → Open** and choose this repository folder (the one that contains `settings.gradle.kts`).
3. Let Gradle sync. Install SDK **36** if Android Studio prompts for it.
4. Connect a phone (USB debugging) or start an emulator with API 26+.
5. Click **Run**.

Command line (Linux / macOS), from the repo root:

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Install on a plugged-in device:

```bash
./gradlew :app:installDebug
```

Unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## What the app does

- Business profiles and customers (Cape Town / Western Cape sample data on first launch)
- Per-customer folders on local disk: invoices, receipts, Excel, images, notes — partitioned by business and customer
- Room database for every invoice, line, status, and file index
- Invoice editor: line items, 15% VAT, discounts, ZAR default, `PREFIX-YEAR-0001` numbering, draft / sent / paid / overdue
- Handwritten signature capture stamped onto an on-device PDF
- Insert or paste pictures on invoices and templates
- Excel import/export (`.xlsx`, `.xls`, CSV) with column mapping; capture app data into customer Excel folders
- Share PDF via Email and WhatsApp using Android intents and FileProvider
- Light / dark / system themes plus six accent palettes
- Editable templates: logo, colours, classic / modern / compact layouts

## Tech

- Kotlin, Jetpack Compose, Material 3
- Min SDK 26, target / compile SDK 36
- Gradle Kotlin DSL, Room, DataStore
- Locale flavour: `en-ZA`, currency ZAR, VAT 15%

## Privacy

No network calls are required. Files stay under the app’s private storage (`files/vault/business-…/customer-…/`). Uninstalling the app removes local data.

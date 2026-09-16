# InvoiceVault

Offline-first Android invoicing for South African businesses (en-ZA, ZAR).

**Install on a phone (no GitHub login):**  
https://github.com/gordoncox12-collab/InvoiceVault/releases/download/v0.1.0-debug/InvoiceVault-debug.apk

That link is a public GitHub Release asset. If a previous download was a few bytes and the installer rejected it, the file was HTML (`Not Found`) from a private repo — this release is on a public repository.

## What it does

- Business profiles and customers, with a Cape Town seed vault on first launch
- Per-customer folders on device storage: invoices, receipts, Excel, images, notes (partitioned by business)
- Room database as the offline source of truth for every invoice, line, receipt, and note
- Invoice editor: line items, 15% VAT (editable), discounts, ZAR, numbering, draft / sent / paid / overdue
- Handwritten signature capture stamped onto the invoice PDF
- On-device PDF generation (no internet)
- Excel / CSV import with column mapping; export customers and invoices to `.xlsx` / `.csv` (`.xls` import included)
- Share PDFs via Email and WhatsApp (Android sharesheet + FileProvider)
- Light / dark / system theme and six accent palettes
- Editable invoice templates (logo, colours, Classic / Modern / Compact layout, picture insert and clipboard paste)

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer is fine).
2. `File → Open` and select this repository root (the folder that contains `settings.gradle.kts`, not a nested module).
3. Let Gradle sync. The IDE will create `local.properties` with your `sdk.dir`.
4. Select the `app` run configuration and a device or emulator (API 26+).
5. Run.

Command line:

```bash
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Minimum SDK 26, target / compile SDK 35. Kotlin, Jetpack Compose, Material 3, Room.

## First launch

The app seeds **Atlantic & Kloof Bookkeeping** in Gardens, Cape Town, with four local customers and sample invoices in ZAR (including paid, sent, draft, and overdue). Everything stays on the phone.

## Privacy

No backend. Files live under app-specific storage: `files/vault/<businessId>/<customerId>/{invoices,receipts,excel,images,notes}/`.

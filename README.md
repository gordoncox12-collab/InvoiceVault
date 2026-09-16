# InvoiceVault

Offline-first Android invoicing for **Gordon Cox** (South Africa, `en-ZA`).

Every business, customer, invoice, signature, spreadsheet and receipt lives on the phone. There is no server.

- **App name:** InvoiceVault  
- **Application ID:** `com.gordoncox.invoicevault`  
- **Min SDK:** 26 (Android 8.0)  
- **Target / compile SDK:** 35  
- **UI:** Kotlin, Jetpack Compose, Material 3  
- **Database:** Room (source of truth)  
- **PDF:** on-device `PdfDocument`  
- **Spreadsheets:** Apache POI (`.xlsx` / `.xls`) plus CSV  

## Open, build and run (Android Studio)

1. Install [Android Studio](https://developer.android.com/studio) (Koala / Ladybug or newer is fine).
2. **File → Open** and choose this repository **root** (the folder that contains `settings.gradle.kts`). Do not open a nested subproject.
3. Wait for Gradle sync. Accept any SDK licence prompts (Platform 35 and Build-Tools 35.0.0).
4. Plug in a phone with **USB debugging**, or start an emulator (API 26+).
5. Click **Run** (green triangle) on the `app` configuration.

First launch seeds two Cape Town businesses (Cox Electrical & Solar, Harbour View Bookkeeping), customers, invoices (draft / sent / paid / overdue), transactions, notes and a sample Excel workbook.

### Command line

```bash
export ANDROID_HOME="$HOME/Android/Sdk"   # or your SDK path
./gradlew :app:assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

Install on a device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Sideload a public APK (phone installer)

This repository is **public**. Do **not** use a private GitHub release link — the phone will download an HTML “Not Found” page, save it as `.apk`, and the installer will reject it.

Use the GitHub Release asset URL (it must start with `PK` / be a ZIP, tens of megabytes). After a tagged debug build the file is named `InvoiceVault-debug.apk`.

## What you can do offline

1. **Business profiles** and **customers** (multiple books).
2. **Per-customer folders** on disk: invoices, receipts, Excel, images, notes — under `files/businesses/{businessId}/customers/{customerId}/…`.
3. **Room** stores every invoice, line item, payment, note and file index.
4. **Invoices:** line items, 15% VAT default, discounts, ZAR default, `PREFIX-YEAR-0001` numbering, draft / sent / paid / overdue.
5. **Handwritten signature** stamped onto the PDF.
6. Complete **local partition** per business and customer.
7. **Excel / CSV** import with column mapping, export, and in-app sheet capture into the customer folder (`.xlsx` / `.xls` / `.csv`).
8. **Share PDF** via Email and WhatsApp (Android `ACTION_SEND` + `FileProvider` attachment).
9. **Theme:** light / dark / system plus six accent palettes.
10. **Invoice templates:** layout (classic / modern / compact), colours, logo, header/extra pictures, clipboard paste.

## Project layout

```
settings.gradle.kts          Gradle root
app/src/main/java/com/gordoncox/invoicevault/
  data/                      Room, files, PDF, Excel, seed
  ui/                        Compose screens
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

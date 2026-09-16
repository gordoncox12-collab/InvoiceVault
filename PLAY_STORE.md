# Play Console listing — InvoiceVault

Use this copy when creating the Google Play listing. Language: **English (South Africa)**.

**Application id:** `com.gordoncox.invoicevault`  
**Version:** 1.0.0 (versionCode 1)  
**Upload artefact:** `InvoiceVault.aab` from the GitHub Release (or `app/build/outputs/bundle/release/app-release.aab`)

## Short description (80 characters max)

Offline invoicing for South African businesses. ZAR, 15% VAT, on this phone.

## Full description

InvoiceVault is an offline invoicing app for South African businesses. Create a business profile, add customers, and store every invoice, receipt, spreadsheet, image and note on the phone. There is no account and no server.

Create tax invoices with line items, 15% VAT (default), discounts, ZAR (default) and PREFIX-YEAR-0001 numbering. Mark invoices draft, sent, paid or overdue. Sign with your finger; the signature is stamped on the PDF.

Each customer has folders for invoices, receipts, Excel, images and notes. Import and export .xlsx, .xls and CSV with column mapping, or capture a sheet in the app. Share PDFs and receipts via Email or WhatsApp.

Choose light, dark or system appearance and eight accent palettes. Edit invoice templates: classic, modern, compact, letterhead or minimal layouts, plus margins, logo position, colours, and pictures you insert or paste.

InvoiceVault works without internet. Your books stay on this device.

## Feature bullets (Play listing)

- Business profiles and customer records, fully offline
- Tax invoices: line items, 15% VAT, discounts, ZAR, numbering
- Draft / sent / paid / overdue, plus handwritten signature on the PDF
- Per-customer folders: invoices, receipts, Excel, images, notes
- Excel and CSV import/export with column mapping
- Share invoices and receipts via Email and WhatsApp
- Light, dark and system themes with accent palettes
- Adjustable invoice layouts, logo, colours and pasted pictures
- Local-only storage — no account, no cloud

## Content rating

- **Category:** Business / Productivity
- **User-generated content:** No public UGC, no social features
- **Target age:** 18+ typical for business tools; no content aimed at children
- **Violence / sexual content / drugs:** None
- **In-app purchases / ads:** None
- Complete the IARC questionnaire as a business utility with no sharing of user content

## Data safety (Play Console)

Declare **no data collected** and **no data shared**.

| Question | Answer |
| --- | --- |
| Does your app collect or share user data? | **No** |
| Data collected | None. Invoices, customers, files and signatures stay in the app’s private storage on the device. |
| Data shared | None. There is no backend. |
| Security practices | Data is not encrypted in transit because nothing is transmitted. Android app sandbox holds files. Optional device backup (Android Auto Backup) may copy local files to the user’s Google account if they have backup enabled — this is the OS, not InvoiceVault servers. |
| Account | No account, no login |
| Approximate location / contacts / photos | Not collected. The user may pick images or spreadsheets from the device; those files are copied into the app’s private folders only. |
| Internet | The app does not require INTERNET permission. Email and WhatsApp sharing use Android intents and the apps already on the phone. |

## Screenshots checklist

Capture on a phone (or 1080×1920 emulator), **en-ZA**, with a **real** business you create — never the old Cape Town demo data.

1. Onboarding — empty first launch, create business profile  
2. Home dashboard after one real business (empty books / first invoice)  
3. Invoice editor — line items, 15% VAT, ZAR  
4. Invoice PDF / view with signature  
5. Customer folder (invoices, receipts, Excel, images, notes)  
6. Excel import with column mapping  
7. Settings — light/dark/system + accent palettes  
8. Template editor — layout chips (classic / modern / compact / letterhead / minimal) and picture insert  

Graphic: feature graphic 1024×500 optional. App icon is the adaptive launcher already in the project.

## How Gordon uploads the AAB

1. Open [Google Play Console](https://play.google.com/console) → Create app → **InvoiceVault**.
2. Complete Store listing with the copy above.
3. App content: Data safety (no data shared), ads (no), content rating.
4. Production (or Internal testing first) → Create new release → Upload **InvoiceVault.aab**.
5. Play App Signing: accept Google-managed signing. Keep `app/play-upload.jks` — you need it for every future upload.
6. Roll out to Internal testing, install from the Play link, then promote to Production.

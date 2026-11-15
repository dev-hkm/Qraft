<div align="center">

# Qraft

**High-craft offline QR Code & Barcode Studio for Android — 100% free, zero paywalls, zero watermarks, zero tracking.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-111113?style=flat-square)](LICENSE)
[![Offline First](https://img.shields.io/badge/100%25%20Offline-Privacy%20First-16A34A?style=flat-square)](#)

Built by **[Trần Hoàng Khánh Minh (`dev-hkm`)](https://khanhminh.tech/)** — *Turning Ideas Into Products.*

</div>

---

## Why Qraft?

Most QR generator apps on mobile throw subscription walls at you just to change a module shape, stamp a watermark unless you watch an ad, upload your private Wi-Fi credentials to someone else's server, or output blurry 512px images.

**Qraft** was built to fix that: a fast, crisp, completely offline QR & barcode workshop where every single feature — vector SVG & 8K PNG exports, custom gradients, center logos, printable A4 label sheets, batch generation, and smart scanning — is unlocked out of the box.

---

## Key Features

### Studio & Design Engine
- **Pinned Live Preview**: The preview card and studio mode tabs stay anchored while you scroll through long forms or tweak colors and shapes underneath — zero scrolling up and down to check your edits.
- **12 Structured QR Types**: Website URL, Wi-Fi Pass, vCard 3.0 Contact Card, Plain Text, Email Draft, Phone Call, SMS, Geo Location, Calendar Event (with quick schedule presets), Social Profile, Payment & Crypto URI, and App / Deep Links.
- **10 Retail & Industrial Barcode Formats**: Code 128, EAN-13, EAN-8, UPC-A, Code 39, ITF-14, Codabar, PDF417, Aztec, and Data Matrix — with optional framed label cards.
- **Total Visual Control**: 2-column preset showcase grid, borderless pixel/eye shape pickers, solid & multi-stop linear/radial/sweep gradients, photo backgrounds, built-in or gallery center logos with automatic palette extraction, and 12 outer frames (including plain/frameless export).
- **Live Scannability Telemetry**: Offline contrast check and self-decode verification (`QR` contrast & ECC gauge + one-tap auto-fix) while you design.

### Export, Print & Batch
- **Up to 8K Raster & True Vector Export**: Export cleanly at **1K, 2K, 4K, or 8K** (`8192px`) in PNG, WEBP, JPEG, **infinite-scale SVG**, or **A4 Print PDF** (with automatic OOM-safe fallback on tighter hardware).
- **Batch Generator**: Generate numbered sequence packs (`PREFIX-{n}`) in one run — save straight to your library, export individual PNGs, or compile ready-to-print A4 sticker sheets.

### Pro Camera & 8K Image Scanner
- **Tactile CameraX Scanner**: Pinch-to-zoom, quick zoom pills (`1x / 2x / 3x / 5x`), smooth zoom slider, tap-to-focus AF/AE reticle, torch toggle, front/rear flip, continuous batch scan mode, and session scan history.
- **Distant & High-Res Decoding**: Dedicated center-ROI luminance pass for small/distant codes plus multi-scale & center-cropped ZXing decoding so even 4K/8K framed Qraft exports read cleanly from your gallery.
- **Recreate & Customize in Studio**: Scan any existing QR code or barcode and load its parsed payload straight into the Qraft Studio to restyle, add a frame, upgrade resolution to 8K, or save it.

### Interface, Apple Motion & Liquid Glass
- **3-Tier Liquid Glass (`AppleGlass`)**: AGSL `RuntimeShader` Fresnel & chromatic edge refraction on Android 13+ hero controls, live Haze multi-layer backdrop sampling (`AppleProgressiveEdgeFrost` headers & floating action docks), and adaptive translucent catchlight fallbacks down to Android 7.0 — plus full `Reduce Transparency` high-contrast support.
- **Perceptual Spring Physics (`AppMotion`)**: Layered depth-stack navigation (`SaveableStateHolder`) with shared-element hero morphs, finger-coupled predictive back peel, velocity-projected drag-to-dismiss sheets & fullscreen previews (`projectOffset` + asymptotic `rubberBand`), pointer-down spring compression, gliding pill segmented tabs, tabular odometer counters, and full `ANIMATOR_DURATION_SCALE` respect.

### Vault Library, Onboarding & Preferences
- **Interactive Onboarding**: 3-slide Liquid Glass welcome walkthrough showcasing Qraft's "100% Free Forever, Zero Paywalls" philosophy with a live interactive preset preview switcher and replay access from Settings.
- **Archive & Organization**: Search across active & archived codes, filter by type/barcodes/favorites/folders, attach custom tags, rename items, bulk-favorite, bulk-archive, bulk-print, or export portable `.qraft` JSON backups.
- **Dedicated Settings Screen**: Clean adaptive light/dark theme switcher (`System / Light / Dark`), default export format & resolution, live scannability toggle, and offline vault backup/restore.

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language & Async | Kotlin + Coroutines & Flow |
| UI & Motion | Jetpack Compose + Material 3 + `AppMotion` Perceptual Springs & Predictive Back |
| Liquid Glass | AGSL `RuntimeShader` (`API 33+`) + Haze 1.7 Backdrop Blur + Fresnel Specular Rims |
| QR Vector & Styling Engine | QRose + Custom Android Canvas / SVG / PDF Engine |
| Barcode & Decoding | ZXing Core + Multi-Scale Bitmap/Luminance Decoder |
| Live Camera | CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) |
| Persistence | Jetpack DataStore Preferences + Null-Sanitized Gson Serialization |
| Testing | JUnit 4 + Robolectric + Compose UI Tests |

---

## Project Structure

```text
app/src/main/java/com/hkm/qraft/
├── MainActivity.kt              # Edge-to-edge shell, layered depth stack & deep-link share receiver
├── data/
│   ├── PresetCatalog.kt         # Curated style templates & starter QR/barcode library
│   ├── QraftBackupManager.kt    # Portable .qraft JSON backup export/import
│   └── QraftRepository.kt       # DataStore persistence & library state mutations
├── model/
│   └── QrModels.kt              # Domain models, enums & null-safe Gson sanitization
├── qr/
│   ├── BarcodeEngine.kt         # 1D/2D barcode matrix, Canvas, SVG & validation
│   ├── LogoCatalog.kt           # Built-in vector glyphs & sampled media loader
│   ├── PaletteExtractor.kt      # Logo color extraction for instant brand matching
│   ├── PrintSheetEngine.kt      # Multi-up A4 sticker grid PDF/PNG renderer
│   ├── QrCodeImporter.kt        # Multi-scale/cropped decoder & structured payload parser
│   ├── QrEngine.kt              # Stylized QR Canvas + pure vector SVG + outer frames
│   ├── QrExporter.kt            # MediaStore Pictures/Documents saver & share sheet
│   └── QrPayloadEncoder.kt      # Spec-compliant encoders (Wi-Fi, vCard, iCal, Payment…)
└── ui/
    ├── QraftViewModel.kt        # Studio undo/redo history, draft state & batch jobs
    ├── components/              # Pinned sheets, color picker, content forms, studio tabs
    ├── screens/                 # Home, Onboarding, Studio, Library Vault, Scanner & Settings
    └── theme/                   # Monochrome palette tokens, AppMotion springs & AppleGlass engine
```

---

## Building Locally

### Prerequisites
- Android Studio (Koala Feature Drop or newer recommended)
- JDK 17+
- Android SDK 36/37 (`minSdk 24`)

### Commands

```bash
# Clone the repository
git clone https://github.com/dev-hkm/Qraft.git
cd Qraft

# Build Debug APK
./gradlew assembleDebug

# Run Unit & Robolectric Compose UI Tests
./gradlew :app:testDebugUnitTest "-Pkotlin.incremental=false"
```

### Optional Release Signing

If you want to sign a release build locally, provide your keystore properties via environment variables or `local.properties` (never commit secrets):

```bash
QRAFT_KEYSTORE_PATH=/path/to/your/keystore.jks
QRAFT_KEYSTORE_PASSWORD=your_store_password
QRAFT_KEY_ALIAS=your_alias
QRAFT_KEY_PASSWORD=your_key_password
```

Then run:

```bash
./gradlew assembleRelease
```

---

## Author & License

Made by **Trần Hoàng Khánh Minh** (`dev-hkm`)

- Portfolio: [khanhminh.tech](https://khanhminh.tech/)
- GitHub: [@dev-hkm](https://github.com/dev-hkm)
- Telegram: [@hoang_khanh_minh](https://t.me/hoang_khanh_minh)

Released under the [MIT License](LICENSE).

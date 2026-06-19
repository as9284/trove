# Architecture

Trove is a fully offline Android app that indexes your screenshots and lets you
search them by the text inside them, using on-device OCR. No account, no cloud,
no `INTERNET` permission. See [`branding/BRAND.md`](../branding/BRAND.md) for the
visual identity and [`PRIVACY.md`](../PRIVACY.md) for how data is handled.

## Stack

- Kotlin, Jetpack Compose, Material 3.
- Room (FTS4) for the index and full-text search.
- WorkManager for resumable background indexing and OCR.
- DataStore for small key-value settings.
- Coil for thumbnail loading.
- PaddleOCR PP-OCRv5 models on ONNX Runtime for on-device OCR.
- AppCompat for per-app day/night (so the chosen theme drives the splash too).

minSdk 26, compileSdk/targetSdk 35, `applicationId` `com.as.trove` (code
namespace `com.astrove`, since `as` is a reserved Kotlin keyword), 64-bit ABIs
only (`arm64-v8a`, `x86_64`).

## Layers

- **Data sources:** `MediaStoreSource` (query/observe screenshots), `OcrEngine`
  with an `OnnxOcrEngine` implementation, and Room DAOs.
- **Repository:** `ScreenshotRepository` reconciles MediaStore against the Room
  index and owns the scan watermark.
- **Domain:** classification rules, entity extraction, search, album grouping.
- **UI:** Compose screens with `ViewModel`s exposing `StateFlow`. A bottom
  navigation shell hosts Home, Search, Clean up, and Settings; Gallery and
  Detail are pushed on top.

Dependencies are wired by hand in `AppContainer`, created once in `TroveApp`.

## Package map

| Package | Responsibility |
|---|---|
| `data.media` | MediaStore queries and the difference-hash used for near-duplicate detection. |
| `data.ocr` | The `OcrEngine` interface and the ONNX Runtime (PP-OCR) implementation. |
| `data.db` | Room entities, DAOs, and the FTS table. |
| `data.entity` | Regex extraction of links, emails, phones, codes, tracking numbers. |
| `data.classify` | Text-driven auto-categorization. |
| `data.prefs` | DataStore-backed settings (watermark, recent searches, charging-only, theme). |
| `data` | `ScreenshotRepository`, the single source of truth for the index. |
| `work` | WorkManager workers and the foreground OCR scheduler. |
| `ui` | Compose screens, theme, components, and navigation. |
| `di` | The manual dependency container. |

## Data model (Room)

- `screenshots`: mediaId, uri, displayName, dateAdded, width, height, mime,
  bucket, source guess, category, confidence, image hash, pinned, trashed,
  ocrStatus.
- `screenshot_text` (FTS4): mediaId, text. Backs search and snippet highlighting
  (rowid maps to mediaId).
- Category is stored on the row, so improved rules can re-run without re-OCR.

## Core flows & edge cases

| Area | Approach |
|---|---|
| Detection | MediaStore `BUCKET_DISPLAY_NAME = 'Screenshots'` or `RELATIVE_PATH LIKE '%Screenshots%'`; matches both `Pictures/Screenshots` (Pixel and most) and `DCIM/Screenshots` (Samsung, Xiaomi); case-insensitive, preferring path over the localized bucket name. |
| Permissions | `READ_MEDIA_IMAGES` (33+), `READ_MEDIA_VISUAL_USER_SELECTED` (34+), `READ_EXTERNAL_STORAGE` (maxSdk 32). Requests full access. |
| Partial access (A14+) | Detected in `onResume`. "Selected photos" shows a first-class "Trove needs all your screenshots" screen with re-request and a Settings deep link. |
| Initial backfill | Metadata import is chunked and resumable (the watermark advances per chunk), so a large library is browsable in seconds. OCR is decoupled and runs newest-first: by default only while the app is in the foreground (process-lifecycle driven), so it never grinds silently in the background. A Settings toggle switches OCR to a charging-constrained WorkManager job. A determinate banner shows "Reading N of M". |
| New screenshots | Delta scan on launch (`DATE_ADDED > watermark`) plus a periodic worker. |
| OCR pipeline | Two-stage PP-OCR on ONNX Runtime, on `Dispatchers.Default`: a DBNet detector finds text regions (boxes treated as axis-aligned, since screenshot text is upright), then a CRNN recognizer reads each crop and a CTC decoder turns logits into text. Each read is kept only if its mean per-character confidence clears a drop-score (matching PaddleOCR's default) and contains a letter or digit, so icons and lone symbols don't pollute the index. Results are assembled in reading order and written to FTS with status states and retry. A stored OCR-pipeline version triggers a one-time re-read of the whole library after the pipeline is upgraded. |
| Near-duplicate cleanup | dHash plus a same-dimensions check; when OCR text exists for both shots, the text must also be near-identical, so a sparse chat and a sparse receipt are not mistaken for each other. |
| Bulk delete | `MediaStore.createTrashRequest` (recoverable, one consent dialog per batch) on API 30+. |
| Long / HDR / webp | Aggressive downscale for OCR; `image/webp` included; Ultra HDR decodes as SDR. |
| Cloud-only / stale rows | Decode is wrapped in try/catch; the row is marked unavailable rather than crashing. |
| Memory | Two-pass decode everywhere; thumbnails via Coil; full-res bitmaps are never held. |

## Notes & decisions

- compileSdk/targetSdk is 35, kept within AGP 8.7.3's safe range. Bumping to 36
  needs AGP 8.9 or newer.
- The release splits per ABI (`arm64-v8a`, `x86_64`) so each APK carries one
  `libonnxruntime.so`. arm64-v8a is for real devices; x86_64 is for emulators.
- No `INTERNET` permission, FOSS dependencies only (ONNX Runtime is MIT, the
  PP-OCR models are Apache-2.0), no Play Services, 64-bit ABIs, plain Gradle
  (the native runtime comes from Maven, no NDK build steps).

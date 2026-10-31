# OmniConvert - Universal All-in-One File Converter Platform

OmniConvert is a professional-grade, modern Universal File Converter application built natively for Android using Jetpack Compose, Kotlin Coroutines, and local high-performance conversion engines. It allows users to convert across images, audio, video, documents, archives, and structured data with quality preservation, fine-grained encoding controls, batch queues, and instant export.

### User Review & Critical Decisions

> [!IMPORTANT]
> The application is engineered with a **100% on-device native conversion architecture**, guaranteeing absolute privacy, zero latency, offline readiness, and no reliance on third-party servers. All files remain strictly on the user's device.

- **On-Device Real Conversion Engines**:
  - **Images**: Android Bitmap, ImageDecoder, Canvas, ColorFilter, and custom encoders for PNG, JPEG, WEBP (lossy/lossless), BMP, ICO, and SVG vector/raster wrappers.
  - **Documents & Text**: Android `PdfDocument` generation (from images, HTML, formatted text), `PdfRenderer` for PDF-to-Image extraction, and bidirectional converters for JSON, XML, YAML, CSV, Markdown, HTML, and TXT.
  - **Audio & Video**: Android `MediaMetadataRetriever`, `MediaExtractor`, and `MediaMuxer` for video-to-audio track extraction (extracting AAC/M4A from MP4/MKV), raw PCM WAV header generation, volume amplification, and audio trimming.
  - **Archives**: Native `ZipInputStream`/`ZipOutputStream` and `GZIP` streams for ZIP compression, unpacking, and multi-file batch bundling.
- **Sample Files Included**: Ready-to-convert sample files (Image, Audio, Document, Data) built into the app for instant testing on emulators and physical devices.
- **Persistence**: Room Database for conversion history log and DataStore for persistent user preferences.

---

### 1. Overview & Core Concept

- **What It Does**: OmniConvert transforms files across 6 major categories (Images, Audio, Video, Documents, Archives, Structured Data) with automatic MIME/extension detection, intelligent compatible format suggestion, advanced quality controls (compression, resolution, sample rate, bitrate), batch queue processing, and single/ZIP download.
- **Target Audience**: Creatives, developers, students, and professionals who need swift, secure, and privacy-preserving file transformations on mobile without uploading sensitive files to cloud servers.
- **Key Value**: Desktop-grade conversion features packaged into an elegant mobile SaaS dashboard, eliminating file upload limits, server queues, and privacy hazards.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Home / Discovery Flow**:
   - Hero header: *"Convert Any File. Effortlessly."*
   - Universal Drag-and-Drop / File Picker zone with one-tap system file browsing.
   - Built-in **"Try with Sample File"** carousel to immediately demonstrate conversions.
   - Category Shortcuts: *Image Studio*, *Audio & Video Lab*, *Document Suite*, *Archive Pack*, *Data Transformer*.
2. **Conversion Workspace Flow**:
   - Inspect original file: MIME type, file size, dimensions/duration/page count, format badge.
   - Target format selector filtered strictly to compatible outputs with "Recommended" badges.
   - **Quality Control Panel**:
     - Presets: *Maximum Quality*, *High Quality*, *Balanced*, *Smaller File Size*, *Custom*.
     - Dynamic sliders: Quality %, Resolution scaling (25% to 200%), Aspect Ratio toggle, Bitrate (64k to 320k), Audio Channel (Mono/Stereo).
   - Real-time conversion with visual progress indicator (0% to 100%), stage details, and cancellation.
   - Success card: Result size, space saved percentage, preview card, and instant "Save to Device" / "Share" buttons.
3. **Batch Conversion Flow**:
   - Multi-file selection support.
   - Apply uniform output format or configure individual target formats per file.
   - Concurrent queue execution with queue progress summary and "Download All as ZIP" action.
4. **History & Settings Flows**:
   - Searchable, filterable log of previous conversions stored in Room with quick re-download or share.
   - Configurable default quality presets, auto-clearing cache, and dark/light theme switching.

#### Visual Identity & Theme
- **Theme**: *Sleek Pro Tech Dark* with Material 3 dynamic adaptation.
- **Palette**: Deep obsidian `#0F172A` background, Slate `#1E293B` elevated cards, Electric Cobalt `#3B82F6` primary accent, Vibrant Cyan `#06B6D4` secondary accent, and Emerald `#10B981` success tokens.
- **Typography**: Clean modern sans-serif with high contrast metric badges and monospace data chips for MIME types and resolutions.
- **Layout**: Adaptive layout supporting handheld phones, foldables, and tablets with edge-to-edge system bar compliance.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Android Conversion Engines vs. Cloud API**
  - *Chosen Approach*: Pure on-device Kotlin/Android APIs (`Bitmap`, `PdfDocument`, `PdfRenderer`, `MediaMuxer`, `ZipOutputStream`, native parsers).
  - *Why*: 100% data privacy, instant turnaround, offline reliability, and zero infrastructure costs.
- **Decision 2: Safe Memory Management for Large Files**
  - *Chosen Approach*: Streamed chunking for archives and documents, in-sample subsampling for ultra-high-resolution images, and background execution via Coroutines `Dispatchers.IO`.
  - *Why*: Prevents `OutOfMemoryError` on constrained Android devices.
- **Decision 3: Room Database for Conversion History**
  - *Chosen Approach*: Room DAO with reactive `Flow` emission.
  - *Why*: Allows instantaneous UI updates when batch items complete without blocking the main thread.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        OmniConvert App UI                              │
│  ┌──────────────┬──────────────────┬───────────────┬────────────────┐  │
│  │  Home Hub    │ Workspace Screen │  Batch Queue  │ History & Set. │  │
│  └──────┬───────┴────────┬─────────┴───────┬───────┴────────┬───────┘  │
│         │                │                 │                │          │
│  ┌──────▼────────────────▼─────────────────▼────────────────▼───────┐  │
│  │                    ConverterViewModel                            │  │
│  │   - Active Jobs Flow       - History Flow      - Settings State  │  │
│  └──────┬───────────────────────────────────────────────────┬───────┘  │
└─────────┼───────────────────────────────────────────────────┼──────────┘
          │                                                   │
┌─────────▼───────────────────────────┐   ┌───────────────────▼──────────┐
│      ConversionEngineManager        │   │       HistoryRepository      │
│  ┌───────────────────────────────┐  │   │  ┌─────────────────────────┐ │
│  │ ImageEngine (JPG,PNG,WEBP,BMP)│  │   │  │ AppDatabase (Room)      │ │
│  ├───────────────────────────────┤  │   │  │ ConversionRecordDao     │ │
│  │ DocumentEngine (PDF,TXT,MD,..)│  │   │  └─────────────────────────┘ │
│  ├───────────────────────────────┤  │   └──────────────────────────────┘
│  │ AudioVideoEngine (MP4,WAV,AAC)│  │
│  ├───────────────────────────────┤  │   ┌──────────────────────────────┐
│  │ DataEngine (JSON,XML,CSV,YAML)│  │   │     SampleFilesProvider      │
│  ├───────────────────────────────┤  │   │  (Ready-to-test assets)      │
│  │ ArchiveEngine (ZIP, TAR, GZ)  │  │   └──────────────────────────────┘
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

#### Key Architecture Components
1. **`ConversionEngineManager`**: Detects file signatures, provides compatible format lists, routes conversion tasks, and calculates estimated file sizes.
2. **`ImageEngine`**: Resizes, compresses, transforms color spaces, converts between PNG, JPG, WEBP, BMP, ICO, and SVG wrapper.
3. **`DocumentEngine`**: Generates PDFs using `PdfDocument`, extracts text, renders PDF pages to PNG/JPG with `PdfRenderer`, and translates structured text formats (Markdown <-> HTML, CSV <-> JSON, JSON <-> XML, YAML <-> JSON).
4. **`AudioVideoEngine`**: Extracts audio tracks from video files via `MediaExtractor`/`MediaMuxer`, generates 16-bit PCM WAV headers, trims duration, and adjusts volume levels.
5. **`ArchiveEngine`**: Packages multiple files into `.zip` or `.tar.gz` with selectable compression levels and extracts archive contents safely against directory traversal.
6. **`HistoryRepository` & `AppDatabase`**: Stores conversion timestamps, input/output file names, sizes, durations, and output URIs for persistent history.

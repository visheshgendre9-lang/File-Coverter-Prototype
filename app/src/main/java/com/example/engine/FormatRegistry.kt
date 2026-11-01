package com.example.engine

import com.example.model.FileCategory
import com.example.model.FileFormat

object FormatRegistry {

    // Image Formats
    val JPG = FileFormat("jpg", "image/jpeg", "JPG / JPEG", FileCategory.IMAGE, false, "Universal lossy image format")
    val PNG = FileFormat("png", "image/png", "PNG", FileCategory.IMAGE, true, "Lossless image with alpha transparency")
    val WEBP = FileFormat("webp", "image/webp", "WEBP", FileCategory.IMAGE, true, "Modern compact web image format")
    val GIF = FileFormat("gif", "image/gif", "GIF", FileCategory.IMAGE, false, "Animated or indexed palette graphic")
    val BMP = FileFormat("bmp", "image/bmp", "BMP", FileCategory.IMAGE, true, "Uncompressed raster bitmap")
    val ICO = FileFormat("ico", "image/x-icon", "ICO", FileCategory.IMAGE, true, "Windows icon format")
    val SVG = FileFormat("svg", "image/svg+xml", "SVG", FileCategory.IMAGE, true, "Scalable vector graphics")
    val HEIC = FileFormat("heic", "image/heic", "HEIC / HEIF", FileCategory.IMAGE, false, "High efficiency image container")
    val TIFF = FileFormat("tiff", "image/tiff", "TIFF", FileCategory.IMAGE, true, "Tagged Image File Format")
    val AVIF = FileFormat("avif", "image/avif", "AVIF", FileCategory.IMAGE, false, "AV1 image file format")

    // Audio Formats
    val MP3 = FileFormat("mp3", "audio/mpeg", "MP3", FileCategory.AUDIO, false, "Universal standard compressed audio")
    val WAV = FileFormat("wav", "audio/wav", "WAV", FileCategory.AUDIO, true, "Uncompressed high-fidelity PCM audio")
    val AAC = FileFormat("aac", "audio/aac", "AAC", FileCategory.AUDIO, false, "Advanced audio coding")
    val M4A = FileFormat("m4a", "audio/mp4", "M4A", FileCategory.AUDIO, false, "MPEG-4 audio container")
    val FLAC = FileFormat("flac", "audio/flac", "FLAC", FileCategory.AUDIO, true, "Free Lossless Audio Codec")
    val OGG = FileFormat("ogg", "audio/ogg", "OGG / Vorbis", FileCategory.AUDIO, false, "Open container audio format")
    val OPUS = FileFormat("opus", "audio/opus", "OPUS", FileCategory.AUDIO, false, "Ultra-low latency audio codec")
    val AIFF = FileFormat("aiff", "audio/x-aiff", "AIFF", FileCategory.AUDIO, true, "Audio Interchange File Format")

    // Video Formats
    val MP4 = FileFormat("mp4", "video/mp4", "MP4", FileCategory.VIDEO, false, "Universal digital multimedia container")
    val MKV = FileFormat("mkv", "video/x-matroska", "MKV", FileCategory.VIDEO, false, "Matroska multimedia container")
    val MOV = FileFormat("mov", "video/quicktime", "MOV", FileCategory.VIDEO, false, "Apple QuickTime video format")
    val WEBM_V = FileFormat("webm", "video/webm", "WEBM Video", FileCategory.VIDEO, false, "WebM open video format")
    val AVI = FileFormat("avi", "video/x-msvideo", "AVI", FileCategory.VIDEO, false, "Audio Video Interleave container")
    val GP3 = FileFormat("3gp", "video/3gpp", "3GP", FileCategory.VIDEO, false, "Mobile phone multimedia format")

    // Document Formats
    val PDF = FileFormat("pdf", "application/pdf", "PDF", FileCategory.DOCUMENT, true, "Portable Document Format")
    val TXT = FileFormat("txt", "text/plain", "TXT", FileCategory.DOCUMENT, true, "Plain text document")
    val HTML = FileFormat("html", "text/html", "HTML", FileCategory.DOCUMENT, true, "HyperText Markup Language")
    val DOCX = FileFormat("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX", FileCategory.DOCUMENT, false, "Microsoft Word document")
    val RTF = FileFormat("rtf", "application/rtf", "RTF", FileCategory.DOCUMENT, true, "Rich Text Format")
    val MD = FileFormat("md", "text/markdown", "Markdown", FileCategory.DOCUMENT, true, "Formatted lightweight markup")

    // Archive Formats
    val ZIP = FileFormat("zip", "application/zip", "ZIP", FileCategory.ARCHIVE, true, "Universal archive container")
    val TAR = FileFormat("tar", "application/x-tar", "TAR", FileCategory.ARCHIVE, true, "Tape archive package")
    val GZ = FileFormat("gz", "application/gzip", "GZ / TAR.GZ", FileCategory.ARCHIVE, true, "GNU zip compressed archive")
    val SEVEN_Z = FileFormat("7z", "application/x-7z-compressed", "7Z", FileCategory.ARCHIVE, true, "7-Zip high compression archive")

    // Data Formats
    val JSON = FileFormat("json", "application/json", "JSON", FileCategory.DATA, true, "JavaScript Object Notation")
    val XML = FileFormat("xml", "application/xml", "XML", FileCategory.DATA, true, "Extensible Markup Language")
    val CSV = FileFormat("csv", "text/csv", "CSV", FileCategory.DATA, true, "Comma-separated tabular data")
    val YAML = FileFormat("yaml", "application/x-yaml", "YAML", FileCategory.DATA, true, "YAML data serialization")
    val SQL = FileFormat("sql", "application/sql", "SQL", FileCategory.DATA, true, "Structured Query Language script")

    val ALL_FORMATS = listOf(
        JPG, PNG, WEBP, GIF, BMP, ICO, SVG, HEIC, TIFF, AVIF,
        MP3, WAV, AAC, M4A, FLAC, OGG, OPUS, AIFF,
        MP4, MKV, MOV, WEBM_V, AVI, GP3,
        PDF, TXT, HTML, DOCX, RTF, MD,
        ZIP, TAR, GZ, SEVEN_Z,
        JSON, XML, CSV, YAML, SQL
    )

    fun detectFormat(fileName: String, mimeType: String? = null): FileFormat {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val formatByExt = ALL_FORMATS.firstOrNull { it.extension == ext || (ext == "jpeg" && it == JPG) }
        if (formatByExt != null) return formatByExt

        if (!mimeType.isNullOrBlank()) {
            val formatByMime = ALL_FORMATS.firstOrNull { it.mimeType.equals(mimeType, ignoreCase = true) }
            if (formatByMime != null) return formatByMime
        }

        // Fallbacks based on broad mime
        return when {
            mimeType?.startsWith("image/") == true -> JPG
            mimeType?.startsWith("audio/") == true -> MP3
            mimeType?.startsWith("video/") == true -> MP4
            mimeType?.startsWith("text/") == true -> TXT
            mimeType?.contains("zip") == true -> ZIP
            mimeType?.contains("pdf") == true -> PDF
            else -> TXT
        }
    }

    fun getCompatibleOutputs(input: FileFormat): List<FileFormat> {
        return when (input.category) {
            FileCategory.IMAGE -> listOf(PNG, JPG, WEBP, BMP, ICO, PDF, DOCX, SVG)
            FileCategory.AUDIO -> listOf(WAV, MP3, AAC, M4A, OGG, ZIP)
            FileCategory.VIDEO -> listOf(
                MP4, WEBM_V,
                // Audio extractions
                MP3, WAV, AAC, M4A,
                // Frame snapshot
                JPG, PNG
            )
            FileCategory.DOCUMENT -> when (input) {
                PDF -> listOf(JPG, PNG, DOCX, TXT, HTML, ZIP)
                HTML -> listOf(PDF, TXT, MD)
                MD -> listOf(HTML, TXT, PDF)
                else -> listOf(PDF, TXT, HTML, MD, ZIP)
            }
            FileCategory.ARCHIVE -> listOf(ZIP, TAR, GZ)
            FileCategory.DATA -> when (input) {
                JSON -> listOf(XML, YAML, CSV, TXT)
                XML -> listOf(JSON, YAML, TXT)
                CSV -> listOf(JSON, XML, TXT, HTML)
                YAML -> listOf(JSON, XML, TXT)
                else -> listOf(JSON, TXT, CSV)
            }
        }.filter { it != input }
    }

    fun getRecommendedOutput(input: FileFormat): FileFormat {
        return when (input.category) {
            FileCategory.IMAGE -> if (input == PNG) WEBP else PNG
            FileCategory.AUDIO -> if (input == WAV) MP3 else WAV
            FileCategory.VIDEO -> MP3 // Extracting audio or transcode to MP4
            FileCategory.DOCUMENT -> if (input == PDF) PNG else PDF
            FileCategory.ARCHIVE -> ZIP
            FileCategory.DATA -> if (input == JSON) YAML else JSON
        }
    }
}

package com.example.model

import android.net.Uri

enum class FileCategory(val displayName: String, val iconName: String) {
    IMAGE("Images", "image"),
    AUDIO("Audio", "audiotrack"),
    VIDEO("Video", "videocam"),
    DOCUMENT("Documents", "description"),
    ARCHIVE("Archives", "folder_zip"),
    DATA("Data & Code", "data_object")
}

enum class QualityPreset(val displayName: String, val description: String) {
    MAXIMUM("Maximum Quality", "Preserves original resolution & highest fidelity. Larger file size."),
    HIGH("High Quality", "Near-lossless output with light compression."),
    BALANCED("Balanced", "Optimal compromise between clarity and space savings (Recommended)."),
    SMALLER("Smaller File Size", "Higher compression suited for quick web sharing and emailing."),
    CUSTOM("Custom Settings", "Manually fine-tune bitrate, resolution, and encoding.")
}

data class FileFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val category: FileCategory,
    val isLossless: Boolean = false,
    val description: String = ""
)

data class ConversionOptions(
    val preset: QualityPreset = QualityPreset.BALANCED,
    val qualityPercent: Int = 85,          // 1 - 100
    val scalePercent: Int = 100,           // 25 - 200%
    val customWidth: Int? = null,
    val customHeight: Int? = null,
    val maintainAspectRatio: Boolean = true,
    val compressionLevel: Int = 6,         // 1 - 9 for ZIP/GZ
    val audioBitrateKbps: Int = 192,       // 64, 128, 192, 256, 320
    val audioSampleRateHz: Int = 44100,    // 22050, 44100, 48000
    val audioChannels: Int = 2,            // 1 = Mono, 2 = Stereo
    val videoResolution: String = "1080p", // 480p, 720p, 1080p, 4K
    val extractAudioOnly: Boolean = false,
    val pdfOrientation: String = "Portrait",
    val preserveMetadata: Boolean = true,
    // Voice Enhancement & Noise Reduction
    val enhanceVoiceClarity: Boolean = false,
    val removeBackgroundNoise: Boolean = false,
    val voiceDenoiseIntensity: Float = 0.7f,
    val voiceClarityBoost: Float = 0.6f,
    val voiceLeveler: Boolean = true
)

enum class JobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ConversionJob(
    val id: String,
    val originalName: String,
    val originalUri: Uri? = null,
    val originalSizeBytes: Long = 0L,
    val inputFormat: FileFormat,
    var targetFormat: FileFormat,
    var status: JobStatus = JobStatus.QUEUED,
    var progress: Float = 0f,
    var statusMessage: String = "Ready",
    var resultUri: Uri? = null,
    var resultFile: java.io.File? = null,
    var resultSizeBytes: Long = 0L,
    var durationMs: Long = 0L,
    var errorMessage: String? = null,
    var options: ConversionOptions = ConversionOptions(),
    val timestamp: Long = System.currentTimeMillis()
)

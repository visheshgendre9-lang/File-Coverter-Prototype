package com.example.engine

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.ConversionJob
import com.example.model.FileCategory
import com.example.model.JobStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ConversionEngineManager(private val context: Context) {

    private val convertedOutputDir: File = File(context.filesDir, "converted").apply { mkdirs() }

    suspend fun processJob(
        job: ConversionJob,
        onProgress: (Float, String) -> Unit
    ): ConversionJob = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            job.status = JobStatus.PROCESSING
            job.progress = 0.05f
            onProgress(0.05f, "Preparing file...")

            val inputUri = job.originalUri ?: throw IllegalArgumentException("Missing input file URI")

            val outputFile: File = when (job.inputFormat.category) {
                FileCategory.IMAGE -> {
                    onProgress(0.2f, "Transforming image data...")
                    ImageEngine.convertImage(
                        context = context,
                        inputUri = inputUri,
                        targetFormat = job.targetFormat,
                        options = job.options,
                        outputDir = convertedOutputDir,
                        onProgress = { p -> onProgress(p, "Encoding ${job.targetFormat.extension.uppercase()}...") }
                    )
                }
                FileCategory.DOCUMENT -> {
                    onProgress(0.2f, "Rendering document...")
                    DocumentEngine.convertDocument(
                        context = context,
                        inputUri = inputUri,
                        inputFormat = job.inputFormat,
                        targetFormat = job.targetFormat,
                        options = job.options,
                        outputDir = convertedOutputDir,
                        onProgress = { p -> onProgress(p, "Formatting document...") }
                    )
                }
                FileCategory.AUDIO, FileCategory.VIDEO -> {
                    if (job.options.enhanceVoiceClarity || job.options.removeBackgroundNoise) {
                        onProgress(0.15f, "Cleaning background noise & enhancing voice...")
                        val voiceConfig = VoiceEnhanceConfig(
                            denoiseIntensity = if (job.options.removeBackgroundNoise) job.options.voiceDenoiseIntensity else 0f,
                            clarityBoost = if (job.options.enhanceVoiceClarity) job.options.voiceClarityBoost else 0f,
                            removeLowRumble = job.options.removeBackgroundNoise,
                            removeHighHiss = job.options.removeBackgroundNoise,
                            normalizeSpeech = job.options.voiceLeveler
                        )
                        VoiceEnhancerEngine.enhanceVoice(
                            context = context,
                            inputUri = inputUri,
                            config = voiceConfig,
                            outputDir = convertedOutputDir,
                            onProgress = { p, msg -> onProgress(p, msg) }
                        )
                    } else {
                        onProgress(0.2f, "Processing media stream...")
                        AudioVideoEngine.convertAudioVideo(
                            context = context,
                            inputUri = inputUri,
                            inputFormat = job.inputFormat,
                            targetFormat = job.targetFormat,
                            options = job.options,
                            outputDir = convertedOutputDir,
                            onProgress = { p -> onProgress(p, "Encoding media track...") }
                        )
                    }
                }
                FileCategory.ARCHIVE -> {
                    onProgress(0.2f, "Packaging archive...")
                    ArchiveEngine.convertArchive(
                        context = context,
                        inputUri = inputUri,
                        inputFormat = job.inputFormat,
                        targetFormat = job.targetFormat,
                        options = job.options,
                        outputDir = convertedOutputDir,
                        onProgress = { p -> onProgress(p, "Compressing archive entries...") }
                    )
                }
                FileCategory.DATA -> {
                    onProgress(0.2f, "Translating structured data...")
                    DataEngine.convertData(
                        context = context,
                        inputUri = inputUri,
                        inputFormat = job.inputFormat,
                        targetFormat = job.targetFormat,
                        outputDir = convertedOutputDir,
                        onProgress = { p -> onProgress(p, "Serializing syntax...") }
                    )
                }
            }

            val endTime = System.currentTimeMillis()
            val resultUri = safeGetUri(outputFile)

            job.status = JobStatus.COMPLETED
            job.progress = 1.0f
            job.statusMessage = "Conversion Completed"
            job.resultUri = resultUri
            job.resultFile = outputFile
            job.resultSizeBytes = outputFile.length()
            job.durationMs = endTime - startTime
            onProgress(1.0f, "Completed")
            job
        } catch (e: Exception) {
            job.status = JobStatus.FAILED
            job.errorMessage = e.message ?: "Unknown conversion error"
            job.statusMessage = "Conversion Failed"
            onProgress(0f, "Failed: ${e.localizedMessage}")
            job
        }
    }

    private fun safeGetUri(file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "com.aistudio.omniconvert.qvrp.fileprovider", file)
        } catch (e1: Exception) {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e2: Exception) {
                Uri.fromFile(file)
            }
        }
    }

    fun bundleJobsIntoZip(completedJobs: List<ConversionJob>): File? {
        val files = completedJobs.mapNotNull { job ->
            job.resultFile?.takeIf { it.exists() } ?: run {
                val path = job.resultUri?.path?.substringAfter("files_converted/")
                if (path != null) {
                    val file = File(convertedOutputDir, path)
                    if (file.exists()) file else null
                } else null
            }
        }
        if (files.isEmpty()) return null

        val zipFile = File(convertedOutputDir, "batch_bundle_${System.currentTimeMillis()}.zip")
        return ArchiveEngine.bundleFilesToZip(files, zipFile, 6)
    }

    fun clearCache() {
        convertedOutputDir.deleteRecursively()
        convertedOutputDir.mkdirs()
    }
}

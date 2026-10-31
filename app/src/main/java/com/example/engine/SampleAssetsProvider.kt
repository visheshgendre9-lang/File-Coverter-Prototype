package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SampleFileItem(
    val name: String,
    val format: String,
    val category: String,
    val sizeDisplay: String,
    val uriProvider: (Context) -> Uri
)

object SampleAssetsProvider {

    private fun safeGetUri(context: Context, file: File): Uri {
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

    fun getSampleFiles(): List<SampleFileItem> {
        return listOf(
            SampleFileItem(
                name = "sample_photo.png",
                format = "PNG",
                category = "Images",
                sizeDisplay = "~48 KB",
                uriProvider = { ctx -> getOrCreateSampleImage(ctx) }
            ),
            SampleFileItem(
                name = "project_brief.md",
                format = "Markdown",
                category = "Documents",
                sizeDisplay = "~2 KB",
                uriProvider = { ctx -> getOrCreateSampleMarkdown(ctx) }
            ),
            SampleFileItem(
                name = "analytics_data.csv",
                format = "CSV",
                category = "Data",
                sizeDisplay = "~1.5 KB",
                uriProvider = { ctx -> getOrCreateSampleCsv(ctx) }
            ),
            SampleFileItem(
                name = "tone_beep.wav",
                format = "WAV",
                category = "Audio",
                sizeDisplay = "~88 KB",
                uriProvider = { ctx -> getOrCreateSampleAudio(ctx) }
            ),
            SampleFileItem(
                name = "noisy_voice_record.wav",
                format = "MP3 / WAV",
                category = "Voice Lab",
                sizeDisplay = "~176 KB",
                uriProvider = { ctx -> getOrCreateSampleNoisyVoice(ctx) }
            )
        )
    }

    private fun getOrCreateSampleImage(context: Context): Uri {
        val file = File(context.cacheDir, "sample_photo.png")
        if (!file.exists() || file.length() == 0L) {
            val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Draw gradient background
            val paint = Paint().apply {
                color = Color.rgb(37, 99, 235) // Cobalt Blue
            }
            canvas.drawRect(0f, 0f, 600f, 400f, paint)

            paint.color = Color.rgb(6, 182, 212) // Cyan circle
            canvas.drawCircle(450f, 150f, 100f, paint)

            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.isFakeBoldText = true
            canvas.drawText("OmniConvert Sample Asset", 40f, 200f, paint)

            paint.textSize = 18f
            paint.isFakeBoldText = false
            canvas.drawText("600 x 400px • RGB High Fidelity", 40f, 240f, paint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        }
        return safeGetUri(context, file)
    }

    private fun getOrCreateSampleMarkdown(context: Context): Uri {
        val file = File(context.cacheDir, "project_brief.md")
        if (!file.exists() || file.length() == 0L) {
            val mdContent = """
                # OmniConvert Platform Specification
                
                OmniConvert delivers universal, private, and rapid file conversions.
                
                ## Features
                - Native Image processing (PNG, JPG, WEBP, BMP, SVG, ICO)
                - Professional Document rendering (PDF, TXT, HTML, Markdown)
                - High-fidelity Audio & Video transcoding
                - Zero server dependencies with 100% on-device privacy
                
                ## Performance Highlights
                - Sub-second bitmap transforms
                - Automatic memory throttling
                - Background coroutine queuing
            """.trimIndent()
            file.writeText(mdContent)
        }
        return safeGetUri(context, file)
    }

    private fun getOrCreateSampleCsv(context: Context): Uri {
        val file = File(context.cacheDir, "analytics_data.csv")
        if (!file.exists() || file.length() == 0L) {
            val csvContent = """
                id,category,format,speed_ms,status
                1,Image,WEBP,120,Completed
                2,Document,PDF,240,Completed
                3,Audio,WAV,180,Completed
                4,Archive,ZIP,310,Completed
                5,Data,JSON,45,Completed
            """.trimIndent()
            file.writeText(csvContent)
        }
        return safeGetUri(context, file)
    }

    private fun getOrCreateSampleAudio(context: Context): Uri {
        val file = File(context.cacheDir, "tone_beep.wav")
        if (!file.exists() || file.length() == 0L) {
            val sampleRate = 44100
            val durationSeconds = 1.0
            val numSamples = (durationSeconds * sampleRate).toInt()
            val freqHz = 440.0 // A4 tone
            val pcmData = ByteArray(numSamples * 2)

            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i * freqHz / sampleRate
                val sample = (Math.sin(angle) * 32767).toInt().toShort()
                val idx = i * 2
                pcmData[idx] = (sample.toInt() and 0xFF).toByte()
                pcmData[idx + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }

            val totalSize = 36 + pcmData.size
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1) // PCM
            header.putShort(1) // Mono
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2)
            header.putShort(2)
            header.putShort(16)
            header.put("data".toByteArray())
            header.putInt(pcmData.size)

            FileOutputStream(file).use { out ->
                out.write(header.array())
                out.write(pcmData)
            }
        }
        return safeGetUri(context, file)
    }

    private fun getOrCreateSampleNoisyVoice(context: Context): Uri {
        val file = File(context.cacheDir, "noisy_voice_record.wav")
        if (!file.exists() || file.length() == 0L) {
            val sampleRate = 44100
            val durationSeconds = 2.0
            val numSamples = (durationSeconds * sampleRate).toInt()
            val pcmData = ByteArray(numSamples * 2)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Simulated voice formant modulation (vowel cadence)
                val voiceFundamental = 160.0 // 160Hz pitch
                val speechEnvelope = (Math.sin(2.0 * Math.PI * 1.5 * t).coerceAtLeast(0.0)) // rhythm
                val voiceSignal = (Math.sin(2.0 * Math.PI * voiceFundamental * t) * 0.5 +
                        Math.sin(2.0 * Math.PI * voiceFundamental * 2.0 * t) * 0.3 +
                        Math.sin(2.0 * Math.PI * 2500.0 * t) * 0.2) * speechEnvelope * 18000.0

                // Background noise: 60Hz hum + white noise hiss
                val acHum = Math.sin(2.0 * Math.PI * 60.0 * t) * 2500.0
                val whiteNoise = (Math.random() - 0.5) * 2200.0

                val combined = (voiceSignal + acHum + whiteNoise).toInt().coerceIn(-32768, 32767).toShort()
                val idx = i * 2
                pcmData[idx] = (combined.toInt() and 0xFF).toByte()
                pcmData[idx + 1] = ((combined.toInt() shr 8) and 0xFF).toByte()
            }

            val totalSize = 36 + pcmData.size
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1)
            header.putShort(1)
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2)
            header.putShort(2)
            header.putShort(16)
            header.put("data".toByteArray())
            header.putInt(pcmData.size)

            FileOutputStream(file).use { out ->
                out.write(header.array())
                out.write(pcmData)
            }
        }
        return safeGetUri(context, file)
    }
}

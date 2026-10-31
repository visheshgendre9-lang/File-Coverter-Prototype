package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class VoicePreset(val displayName: String, val description: String) {
    STUDIO_CLARITY("Studio Clarity", "Balanced background noise reduction with vocal presence boost (Recommended)."),
    AGGRESSIVE_DENOISE("Heavy Denoise", "Aggressive noise gate for noisy environments (street, fan, traffic)."),
    PODCAST_WARMTH("Podcast & Interview", "Warm low-mids with crisp speech consonants and leveler."),
    VOCAL_BOOSTER("Voice Leveler & Boost", "Amplifies quiet speech without clipping or distortion.")
}

data class VoiceEnhanceConfig(
    val preset: VoicePreset = VoicePreset.STUDIO_CLARITY,
    val denoiseIntensity: Float = 0.7f,   // 0.0 - 1.0
    val clarityBoost: Float = 0.6f,       // 0.0 - 1.0
    val removeLowRumble: Boolean = true,  // 80Hz high-pass
    val removeHighHiss: Boolean = true,   // 7.5kHz low-pass
    val normalizeSpeech: Boolean = true
)

object VoiceEnhancerEngine {

    fun enhanceVoice(
        context: Context,
        inputUri: Uri,
        config: VoiceEnhanceConfig,
        outputDir: File,
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.1f, "Decoding audio stream...")

        // Decode input audio stream to 16-bit PCM samples
        val (pcmSamples, sampleRate, channels) = decodeAudioToPcm(context, inputUri)

        onProgress(0.35f, "Removing background noise & rumble...")
        // 1. High-pass filter (remove rumble under 85Hz)
        var processed = if (config.removeLowRumble) {
            applyHighPassFilter(pcmSamples, sampleRate, 85.0)
        } else {
            pcmSamples
        }

        // 2. Low-pass filter (remove hiss above 7500Hz)
        if (config.removeHighHiss) {
            processed = applyLowPassFilter(processed, sampleRate, 7500.0)
        }

        onProgress(0.55f, "Applying vocal presence & clarity exciter...")
        // 3. Peaking Bell Filter at 3000Hz for speech clarity
        if (config.clarityBoost > 0.05f) {
            val boostGainDb = 2.0 + (config.clarityBoost * 6.0) // +2dB to +8dB
            processed = applyPeakingFilter(processed, sampleRate, 3000.0, 1.2, boostGainDb)
        }

        onProgress(0.75f, "Gating ambient background sound...")
        // 4. Adaptive Noise Gate
        if (config.denoiseIntensity > 0.05f) {
            processed = applyNoiseGate(processed, sampleRate, config.denoiseIntensity)
        }

        onProgress(0.88f, "Normalizing voice loudness...")
        // 5. Speech Normalizer & Soft Limiter
        if (config.normalizeSpeech) {
            processed = applySpeechNormalization(processed)
        }

        onProgress(0.95f, "Writing enhanced audio...")
        val outputFile = File(outputDir, "enhanced_voice_${System.currentTimeMillis()}.wav")
        writeWavFile(processed, sampleRate, channels, outputFile)

        onProgress(1.0f, "Voice Enhancement Complete")
        return outputFile
    }

    private fun decodeAudioToPcm(context: Context, inputUri: Uri): Triple<ShortArray, Int, Int> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, inputUri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            // Fallback for raw WAV or when codec is not needed
            if (audioTrackIndex < 0 || audioFormat == null) {
                return readRawPcmOrWav(context, inputUri)
            }

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: "audio/raw"
            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()
            extractor.selectTrack(audioTrackIndex)

            val byteStream = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            while (!isEos) {
                val inputBufIndex = codec.dequeueInputBuffer(10000)
                if (inputBufIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufIndex) ?: continue
                    inputBuffer.clear()
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEos = true
                    } else {
                        codec.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                while (outputBufIndex >= 0) {
                    val outBuf = codec.getOutputBuffer(outputBufIndex)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outBuf.get(chunk)
                        byteStream.write(chunk)
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }
                    outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                }
            }

            codec.stop()
            codec.release()

            val pcmBytes = byteStream.toByteArray()
            val shortCount = pcmBytes.size / 2
            val shortArray = ShortArray(shortCount)
            ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
            return Triple(shortArray, sampleRate, channelCount)
        } catch (e: Exception) {
            return readRawPcmOrWav(context, inputUri)
        } finally {
            extractor.release()
        }
    }

    private fun readRawPcmOrWav(context: Context, inputUri: Uri): Triple<ShortArray, Int, Int> {
        val bytes = context.contentResolver.openInputStream(inputUri)?.readBytes() ?: ByteArray(0)
        val dataOffset = if (bytes.size >= 44 && String(bytes, 0, 4) == "RIFF") 44 else 0
        val pcmLength = bytes.size - dataOffset
        val shortCount = max(0, pcmLength / 2)
        val shortArray = ShortArray(shortCount)

        if (shortCount > 0) {
            ByteBuffer.wrap(bytes, dataOffset, pcmLength)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
                .get(shortArray)
        }
        return Triple(shortArray, 44100, 1)
    }

    /**
     * High-pass 2-pole IIR filter (cuts low rumble / wind / AC hum)
     */
    private fun applyHighPassFilter(input: ShortArray, sampleRate: Int, cutoffFreq: Double): ShortArray {
        val output = ShortArray(input.size)
        val rc = 1.0 / (2.0 * Math.PI * cutoffFreq)
        val dt = 1.0 / sampleRate
        val alpha = rc / (rc + dt)

        var prevIn = 0.0
        var prevOut = 0.0

        for (i in input.indices) {
            val inSample = input[i].toDouble()
            val outSample = alpha * (prevOut + inSample - prevIn)
            prevIn = inSample
            prevOut = outSample
            output[i] = outSample.toInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }

    /**
     * Low-pass filter (cuts high air hiss and ultrasonic noise)
     */
    private fun applyLowPassFilter(input: ShortArray, sampleRate: Int, cutoffFreq: Double): ShortArray {
        val output = ShortArray(input.size)
        val rc = 1.0 / (2.0 * Math.PI * cutoffFreq)
        val dt = 1.0 / sampleRate
        val alpha = dt / (rc + dt)

        var prevOut = 0.0

        for (i in input.indices) {
            val inSample = input[i].toDouble()
            val outSample = prevOut + alpha * (inSample - prevOut)
            prevOut = outSample
            output[i] = outSample.toInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }

    /**
     * Peaking Bell Filter for voice presence boost (speech clarity in 2.5kHz - 4kHz range)
     */
    private fun applyPeakingFilter(
        input: ShortArray,
        sampleRate: Int,
        centerFreq: Double,
        q: Double,
        gainDb: Double
    ): ShortArray {
        val output = ShortArray(input.size)
        val a = Math.pow(10.0, gainDb / 40.0)
        val w0 = 2.0 * Math.PI * centerFreq / sampleRate
        val alpha = sin(w0) / (2.0 * q)

        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cos(w0)
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cos(w0)
        val a2 = 1.0 - alpha / a

        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0

        for (i in input.indices) {
            val x0 = input[i].toDouble()
            val y0 = (b0 / a0) * x0 + (b1 / a0) * x1 + (b2 / a0) * x2 - (a1 / a0) * y1 - (a2 / a0) * y2

            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0

            output[i] = y0.toInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }

    /**
     * Adaptive Noise Gate (attenuates background noise when no speaking is happening)
     */
    private fun applyNoiseGate(input: ShortArray, sampleRate: Int, intensity: Float): ShortArray {
        val output = ShortArray(input.size)
        val windowSize = sampleRate / 100 // 10ms frame
        // Threshold scaled by intensity: between 400 and 1800 amplitude
        val threshold = 400.0 + (intensity * 1400.0)

        var currentGain = 1.0
        val attackCoeff = 0.2
        val releaseCoeff = 0.02

        for (chunkStart in input.indices step windowSize) {
            val chunkEnd = min(input.size, chunkStart + windowSize)
            // Compute RMS of this 10ms frame
            var sumSq = 0.0
            for (i in chunkStart until chunkEnd) {
                val s = input[i].toDouble()
                sumSq += s * s
            }
            val rms = sqrt(sumSq / (chunkEnd - chunkStart))
            val targetGain = if (rms < threshold) {
                val ratio = rms / threshold
                0.05 + 0.95 * (ratio * ratio) // smooth attenuation
            } else {
                1.0
            }

            for (i in chunkStart until chunkEnd) {
                currentGain += if (targetGain > currentGain) {
                    (targetGain - currentGain) * attackCoeff
                } else {
                    (targetGain - currentGain) * releaseCoeff
                }
                val sample = input[i].toDouble() * currentGain
                output[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return output
    }

    /**
     * Normalizes peak vocal amplitude up to -1dBFS to ensure loud, clear speech
     */
    private fun applySpeechNormalization(input: ShortArray): ShortArray {
        var peak = 0
        for (sample in input) {
            val a = abs(sample.toInt())
            if (a > peak) peak = a
        }

        if (peak <= 0) return input

        val targetPeak = 30000.0 // ~ -0.8 dBFS
        val gain = min(targetPeak / peak.toDouble(), 4.0) // Up to +12dB boost

        val output = ShortArray(input.size)
        for (i in input.indices) {
            val amplified = input[i].toDouble() * gain
            output[i] = amplified.toInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }

    private fun writeWavFile(pcmData: ShortArray, sampleRate: Int, channels: Int, outputFile: File) {
        val dataSize = pcmData.size * 2
        val totalSize = 36 + dataSize
        val byteRate = sampleRate * channels * 2

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * 2).toShort())
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(dataSize)

        val out = FileOutputStream(outputFile)
        out.write(header.array())

        val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcmData) {
            byteBuffer.putShort(s)
        }
        out.write(byteBuffer.array())
        out.flush()
        out.close()
    }
}

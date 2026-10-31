package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.ConversionOptions
import com.example.model.FileFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioVideoEngine {

    fun convertAudioVideo(
        context: Context,
        inputUri: Uri,
        inputFormat: FileFormat,
        targetFormat: FileFormat,
        options: ConversionOptions,
        outputDir: File,
        onProgress: (Float) -> Unit
    ): File {
        onProgress(0.15f)

        val outputFile = File(outputDir, "media_${System.currentTimeMillis()}.${targetFormat.extension}")

        // 1. Video frame extraction to JPG / PNG
        if (targetFormat.extension == "jpg" || targetFormat.extension == "png") {
            extractVideoFrame(context, inputUri, outputFile, targetFormat.extension, options, onProgress)
            onProgress(1.0f)
            return outputFile
        }

        // 2. Video to Audio track extraction (M4A / AAC / MP4 Audio)
        if (inputFormat.category.name == "VIDEO" && (targetFormat.extension == "m4a" || targetFormat.extension == "aac" || targetFormat.extension == "mp3")) {
            val success = extractAudioTrack(context, inputUri, outputFile, onProgress)
            if (success) {
                onProgress(1.0f)
                return outputFile
            }
        }

        // 3. Audio format generation / AAC / WAV synthesis
        if (targetFormat.extension == "m4a" || targetFormat.extension == "aac") {
            val rawBytes = context.contentResolver.openInputStream(inputUri)?.readBytes() ?: ByteArray(0)
            val pcmOffset = if (rawBytes.size >= 44 && String(rawBytes, 0, 4) == "RIFF") 44 else 0
            val pcmData = if (pcmOffset < rawBytes.size) rawBytes.copyOfRange(pcmOffset, rawBytes.size) else rawBytes
            val success = encodePcmToAacM4a(pcmData, options.audioSampleRateHz, options.audioChannels, options.audioBitrateKbps, outputFile)
            if (!success) {
                generateWavAudio(context, inputUri, outputFile, options, onProgress)
            }
        } else if (targetFormat.extension == "wav") {
            generateWavAudio(context, inputUri, outputFile, options, onProgress)
        } else {
            // Passthrough / Remux with selected parameters
            val success = extractAudioTrack(context, inputUri, outputFile, onProgress)
            if (!success) {
                generateWavAudio(context, inputUri, outputFile, options, onProgress)
            }
        }

        onProgress(1.0f)
        return outputFile
    }

    private fun extractVideoFrame(
        context: Context,
        videoUri: Uri,
        outputFile: File,
        imageExt: String,
        options: ConversionOptions,
        onProgress: (Float) -> Unit
    ) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            onProgress(0.5f)
            val bitmap = retriever.getFrameAtTime(1000000) // 1 second in
                ?: retriever.frameAtTime
                ?: throw IllegalStateException("Could not extract frame from video")

            val outStream = FileOutputStream(outputFile)
            if (imageExt == "jpg" || imageExt == "jpeg") {
                bitmap.compress(Bitmap.CompressFormat.JPEG, options.qualityPercent, outStream)
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            }
            outStream.flush()
            outStream.close()
            bitmap.recycle()
        } finally {
            retriever.release()
        }
    }

    private fun extractAudioTrack(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

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

            if (audioTrackIndex < 0 || audioFormat == null) {
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val writeTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                64 * 1024
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var count = 0
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo)
                extractor.advance()
                count++
                if (count % 100 == 0) {
                    onProgress(0.3f + (0.5f * (count / (count + 100f))))
                }
            }
            return true
        } catch (e: Exception) {
            outputFile.delete()
            return false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
            extractor.release()
        }
    }

    private fun generateWavAudio(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: ConversionOptions,
        onProgress: (Float) -> Unit
    ) {
        val inputStream = context.contentResolver.openInputStream(inputUri)
            ?: throw IllegalArgumentException("Could not read audio stream")
        val rawBytes = inputStream.readBytes()
        inputStream.close()
        onProgress(0.5f)

        val sampleRate = options.audioSampleRateHz
        val channels = options.audioChannels
        val byteRate = sampleRate * channels * 2
        val dataSize = rawBytes.size
        val totalSize = 36 + dataSize

        val out = FileOutputStream(outputFile)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        header.put("RIFF".toByteArray())
        header.putInt(totalSize)
        header.put("WAVE".toByteArray())

        // fmt chunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * 2).toShort()) // BlockAlign
        header.putShort(16) // BitsPerSample

        // data chunk
        header.put("data".toByteArray())
        header.putInt(dataSize)

        out.write(header.array())
        out.write(rawBytes)
        out.flush()
        out.close()
    }

    fun encodePcmToAacM4a(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitrateKbps: Int,
        outputFile: File
    ): Boolean {
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        try {
            val format = MediaFormat.createAudioFormat("audio/mp4a-latm", sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            encoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            var offset = 0
            val chunkSize = 4096
            var inputEos = false

            while (true) {
                if (!inputEos) {
                    val inputBufIdx = encoder.dequeueInputBuffer(10000)
                    if (inputBufIdx >= 0) {
                        val inputBuf = encoder.getInputBuffer(inputBufIdx)
                        inputBuf?.clear()
                        if (offset >= pcmData.size) {
                            encoder.queueInputBuffer(inputBufIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        } else {
                            val bytesToCopy = minOf(chunkSize, pcmData.size - offset)
                            inputBuf?.put(pcmData, offset, bytesToCopy)
                            val ptsUs = (offset / (2.0 * channels * sampleRate) * 1_000_000).toLong()
                            encoder.queueInputBuffer(inputBufIdx, 0, bytesToCopy, ptsUs, 0)
                            offset += bytesToCopy
                        }
                    }
                }

                val outBufIdx = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outBufIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (outBufIdx >= 0) {
                    val outBuf = encoder.getOutputBuffer(outBufIdx)
                    if (outBuf != null && bufferInfo.size > 0 && muxerStarted) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outBufIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
            return true
        } catch (e: Exception) {
            return false
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
            } catch (ignored: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }
}

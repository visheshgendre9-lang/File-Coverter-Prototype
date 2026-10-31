package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.example.model.ConversionOptions
import com.example.model.FileFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImageEngine {

    fun convertImage(
        context: Context,
        inputUri: Uri,
        targetFormat: FileFormat,
        options: ConversionOptions,
        outputDir: File,
        onProgress: (Float) -> Unit
    ): File {
        onProgress(0.15f)

        val inputStream: InputStream = context.contentResolver.openInputStream(inputUri)
            ?: throw IllegalArgumentException("Could not read input image")

        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalStateException("Failed to decode image data into bitmap")
        inputStream.close()

        onProgress(0.40f)

        // Process resizing / scaling
        val scaledBitmap = applyTransformations(originalBitmap, options)

        onProgress(0.65f)

        val outputFile = File(outputDir, "converted_${System.currentTimeMillis()}.${targetFormat.extension}")
        val outStream = FileOutputStream(outputFile)

        try {
            when (targetFormat.extension.lowercase()) {
                "jpg", "jpeg" -> {
                    // JPEG does not support alpha - draw on white background
                    val opaqueBitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(opaqueBitmap)
                    canvas.drawColor(Color.WHITE)
                    canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                    opaqueBitmap.compress(Bitmap.CompressFormat.JPEG, options.qualityPercent.coerceIn(1, 100), outStream)
                    opaqueBitmap.recycle()
                }
                "png" -> {
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                }
                "webp" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (options.qualityPercent >= 98) {
                            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, outStream)
                        } else {
                            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, options.qualityPercent.coerceIn(1, 100), outStream)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        scaledBitmap.compress(Bitmap.CompressFormat.WEBP, options.qualityPercent.coerceIn(1, 100), outStream)
                    }
                }
                "bmp" -> {
                    writeBmpFile(scaledBitmap, outStream)
                }
                "ico" -> {
                    writeIcoFile(scaledBitmap, outStream)
                }
                "svg" -> {
                    writeSvgFile(scaledBitmap, outStream)
                }
                "pdf" -> {
                    writePdfFromImage(scaledBitmap, outStream)
                }
                else -> {
                    // Default to PNG
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                }
            }
        } finally {
            outStream.flush()
            outStream.close()
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()
        }

        onProgress(1.0f)
        return outputFile
    }

    private fun applyTransformations(bitmap: Bitmap, options: ConversionOptions): Bitmap {
        val baseScale = if (options.targetLongSidePx > 0) {
            // "At least 4K": never go below the original size, and enlarge by 4x at most.
            val longSide = maxOf(bitmap.width, bitmap.height).toFloat()
            (options.targetLongSidePx / longSide).coerceIn(1f, 4f)
        } else {
            options.scalePercent / 100f
        }
        var targetW = (bitmap.width * baseScale).toInt().coerceAtLeast(1)
        var targetH = (bitmap.height * baseScale).toInt().coerceAtLeast(1)

        if (options.customWidth != null && options.customWidth > 0) {
            targetW = options.customWidth
            if (options.maintainAspectRatio) {
                val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
                targetH = (targetW * ratio).toInt().coerceAtLeast(1)
            }
        } else if (options.customHeight != null && options.customHeight > 0) {
            targetH = options.customHeight
            if (options.maintainAspectRatio) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                targetW = (targetH * ratio).toInt().coerceAtLeast(1)
            }
        }

        return if (targetW != bitmap.width || targetH != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }
    }

    /**
     * Encodes a standard 24-bit uncompressed Windows BMP file
     */
    private fun writeBmpFile(bitmap: Bitmap, output: FileOutputStream) {
        val width = bitmap.width
        val height = bitmap.height
        val rowSize = (24 * width + 31) / 32 * 4
        val imageSize = rowSize * height
        val fileSize = 54 + imageSize

        val header = ByteBuffer.allocate(54).order(ByteOrder.LITTLE_ENDIAN)
        // BITMAPFILEHEADER (14 bytes)
        header.put('B'.code.toByte())
        header.put('M'.code.toByte())
        header.putInt(fileSize)
        header.putShort(0) // reserved 1
        header.putShort(0) // reserved 2
        header.putInt(54)  // offset to pixel array

        // BITMAPINFOHEADER (40 bytes)
        header.putInt(40)   // header size
        header.putInt(width)
        header.putInt(height) // positive = bottom-up
        header.putShort(1)  // color planes
        header.putShort(24) // 24-bit
        header.putInt(0)    // BI_RGB uncompressed
        header.putInt(imageSize)
        header.putInt(2835) // 72 DPI horizontal (pixels/meter)
        header.putInt(2835) // 72 DPI vertical
        header.putInt(0)
        header.putInt(0)

        output.write(header.array())

        val pixels = IntArray(width)
        val row = ByteArray(rowSize)

        // BMP is stored bottom-to-top
        for (y in height - 1 downTo 0) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            var idx = 0
            for (x in 0 until width) {
                val color = pixels[x]
                row[idx++] = (color and 0xFF).toByte()         // Blue
                row[idx++] = ((color shr 8) and 0xFF).toByte()  // Green
                row[idx++] = ((color shr 16) and 0xFF).toByte() // Red
            }
            while (idx < rowSize) {
                row[idx++] = 0
            }
            output.write(row)
        }
    }

    /**
     * Encodes a valid ICO file container wrapping PNG data
     */
    private fun writeIcoFile(bitmap: Bitmap, output: FileOutputStream) {
        val icoBitmap = if (bitmap.width > 256 || bitmap.height > 256) {
            Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        } else {
            bitmap
        }

        val pngBytes = ByteArrayOutputStream()
        icoBitmap.compress(Bitmap.CompressFormat.PNG, 100, pngBytes)
        val data = pngBytes.toByteArray()

        val header = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
        // ICONDIR
        header.putShort(0) // Reserved
        header.putShort(1) // Type 1 = Icon
        header.putShort(1) // 1 Image

        // ICONDIRENTRY
        val w = if (icoBitmap.width >= 256) 0 else icoBitmap.width.toByte()
        val h = if (icoBitmap.height >= 256) 0 else icoBitmap.height.toByte()
        header.put(w.toByte())
        header.put(h.toByte())
        header.put(0) // Color count (0 = >=8bpp)
        header.put(0) // Reserved
        header.putShort(1) // Color planes
        header.putShort(32) // Bits per pixel
        header.putInt(data.size)
        header.putInt(22) // Offset to image data

        output.write(header.array())
        output.write(data)

        if (icoBitmap != bitmap) {
            icoBitmap.recycle()
        }
    }

    /**
     * Encodes scalable SVG with embedded base64 raster and vector metadata
     */
    private fun writeSvgFile(bitmap: Bitmap, output: FileOutputStream) {
        val pngBytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngBytes)
        val base64 = Base64.encodeToString(pngBytes.toByteArray(), Base64.NO_WRAP)

        val svgContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
                 width="${bitmap.width}" height="${bitmap.height}" viewBox="0 0 ${bitmap.width} ${bitmap.height}">
                <desc>Converted with OmniConvert Platform</desc>
                <image width="${bitmap.width}" height="${bitmap.height}" xlink:href="data:image/png;base64,$base64"/>
            </svg>
        """.trimIndent()

        output.write(svgContent.toByteArray(Charsets.UTF_8))
    }

    /**
     * Creates standard PDF page from bitmap image
     */
    private fun writePdfFromImage(bitmap: Bitmap, output: FileOutputStream) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        page.canvas.drawBitmap(bitmap, 0f, 0f, paint)
        pdfDocument.finishPage(page)

        pdfDocument.writeTo(output)
        pdfDocument.close()
    }
}

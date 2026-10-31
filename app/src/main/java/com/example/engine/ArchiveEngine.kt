package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.model.ConversionOptions
import com.example.model.FileFormat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ArchiveEngine {

    fun convertArchive(
        context: Context,
        inputUri: Uri,
        inputFormat: FileFormat,
        targetFormat: FileFormat,
        options: ConversionOptions,
        outputDir: File,
        onProgress: (Float) -> Unit
    ): File {
        onProgress(0.2f)

        // If target is ZIP or GZ, repack or extract
        val outputFile = File(outputDir, "archive_${System.currentTimeMillis()}.${targetFormat.extension}")

        if (inputFormat.extension == "zip") {
            // Extract and repackage into target
            val tempExtractDir = File(outputDir, "unpacked_${System.currentTimeMillis()}").apply { mkdirs() }
            extractZipSafely(context, inputUri, tempExtractDir)
            onProgress(0.6f)

            val extractedFiles = tempExtractDir.listFiles()?.toList() ?: emptyList()
            createZipArchive(extractedFiles, outputFile, options.compressionLevel)
            tempExtractDir.deleteRecursively()
        } else {
            // Wrap the input file into the target archive format
            val tempInputFile = File(outputDir, "temp_src_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(tempInputFile).use { output ->
                    input.copyTo(output)
                }
            }
            onProgress(0.6f)

            if (targetFormat.extension == "gz") {
                createGzipFile(tempInputFile, outputFile)
            } else {
                createZipArchive(listOf(tempInputFile), outputFile, options.compressionLevel)
            }
            tempInputFile.delete()
        }

        onProgress(1.0f)
        return outputFile
    }

    fun bundleFilesToZip(files: List<File>, zipFile: File, compressionLevel: Int = 6): File {
        return createZipArchive(files, zipFile, compressionLevel)
    }

    private fun createZipArchive(files: List<File>, zipFile: File, compressionLevel: Int): File {
        val zipOut = ZipOutputStream(FileOutputStream(zipFile))
        zipOut.setLevel(compressionLevel.coerceIn(0, 9))

        for (file in files) {
            if (!file.exists()) continue
            val entry = ZipEntry(file.name)
            zipOut.putNextEntry(entry)
            FileInputStream(file).use { it.copyTo(zipOut) }
            zipOut.closeEntry()
        }
        zipOut.close()
        return zipFile
    }

    private fun createGzipFile(source: File, target: File): File {
        FileInputStream(source).use { inStream ->
            GZIPOutputStream(FileOutputStream(target)).use { gzOut ->
                inStream.copyTo(gzOut)
            }
        }
        return target
    }

    private fun extractZipSafely(context: Context, zipUri: Uri, destinationDir: File) {
        val inputStream = context.contentResolver.openInputStream(zipUri) ?: return
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry

        while (entry != null) {
            val file = File(destinationDir, entry.name)
            // Guard against Zip Slip vulnerability
            val canonicalPath = file.canonicalPath
            if (!canonicalPath.startsWith(destinationDir.canonicalPath)) {
                throw SecurityException("Zip entry is outside of the target dir: ${entry.name}")
            }

            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { out ->
                    zipIn.copyTo(out)
                }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
    }
}

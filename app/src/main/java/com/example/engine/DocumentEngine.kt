package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.model.ConversionOptions
import com.example.model.FileFormat
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object DocumentEngine {

    fun convertDocument(
        context: Context,
        inputUri: Uri,
        inputFormat: FileFormat,
        targetFormat: FileFormat,
        options: ConversionOptions,
        outputDir: File,
        onProgress: (Float) -> Unit
    ): File {
        onProgress(0.2f)

        val outputFile = File(outputDir, "doc_${System.currentTimeMillis()}.${targetFormat.extension}")

        when {
            // PDF -> Image (Render first page to PNG/JPG)
            inputFormat.extension == "pdf" && (targetFormat.extension == "png" || targetFormat.extension == "jpg") -> {
                renderPdfToImage(context, inputUri, outputFile, targetFormat.extension, options, onProgress)
            }

            // PDF -> DOCX (each page as a picture)
            inputFormat.extension == "pdf" && targetFormat.extension == "docx" -> {
                convertPdfToDocx(context, inputUri, outputFile, options, onProgress)
            }

            // PDF -> TXT text extraction
            inputFormat.extension == "pdf" && targetFormat.extension == "txt" -> {
                extractTextFromPdf(context, inputUri, outputFile, onProgress)
            }

            // DOCX -> TXT, HTML, PDF
            inputFormat.extension == "docx" -> {
                convertDocxDocument(context, inputUri, targetFormat, outputFile, options, onProgress)
            }

            // XLSX -> CSV, JSON, TXT
            inputFormat.extension == "xlsx" -> {
                convertXlsxDocument(context, inputUri, targetFormat, outputFile, onProgress)
            }

            // Text/Markdown/HTML -> PDF
            targetFormat.extension == "pdf" -> {
                renderTextOrHtmlToPdf(context, inputUri, outputFile, options, onProgress)
            }

            // Markdown -> HTML
            inputFormat.extension == "md" && targetFormat.extension == "html" -> {
                convertMarkdownToHtml(context, inputUri, outputFile, onProgress)
            }

            // HTML -> Markdown or TXT
            inputFormat.extension == "html" && (targetFormat.extension == "md" || targetFormat.extension == "txt") -> {
                convertHtmlToTextOrMd(context, inputUri, outputFile, targetFormat.extension == "md", onProgress)
            }

            // Default Text Transcoding (e.g. TXT -> HTML / MD / RTF)
            else -> {
                transcodeTextDocument(context, inputUri, inputFormat, targetFormat, outputFile, onProgress)
            }
        }

        onProgress(1.0f)
        return outputFile
    }

    /**
     * How much to enlarge a PDF page (measured in points, 1/72 inch) when drawing it as a picture.
     * Either fit the long side to a target size (2K = 2560 px, 4K = 3840 px), or render at the
     * requested DPI and apply the optional scale slider.
     */
    private fun pdfRenderFactor(pageWidth: Int, pageHeight: Int, options: ConversionOptions): Float {
        var factor = if (options.targetLongSidePx > 0) {
            options.targetLongSidePx / maxOf(pageWidth, pageHeight).toFloat()
        } else {
            (options.pdfRenderDpi / 72f) * (options.scalePercent / 100f)
        }

        // Safety cap (~36 megapixels for 8K) so very large pages cannot run the phone out of memory.
        val maxPixels = 36_000_000f
        val requestedPixels = pageWidth.toFloat() * factor * pageHeight.toFloat() * factor
        if (requestedPixels > maxPixels) {
            factor *= kotlin.math.sqrt(maxPixels / requestedPixels)
        }
        return factor
    }

    /**
     * PDF -> DOCX. Every PDF page is drawn as a picture on a Word page of the same size. The
     * result looks exactly like the PDF but its text is not editable (that needs text extraction
     * or OCR, which Android does not provide).
     */
    private fun convertPdfToDocx(
        context: Context,
        pdfUri: Uri,
        outputFile: File,
        options: ConversionOptions,
        onProgress: (Float) -> Unit
    ) {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
            ?: throw IllegalArgumentException("Cannot open PDF file descriptor")

        val renderer = PdfRenderer(pfd)
        try {
            val pageCount = renderer.pageCount
            if (pageCount == 0) throw IllegalStateException("Empty PDF document")

            FileOutputStream(outputFile).buffered().use { fileOut ->
                DocxImageDocument(fileOut).use { docx ->
                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        try {
                            val factor = pdfRenderFactor(page.width, page.height, options)
                            val width = (page.width * factor).toInt().coerceAtLeast(100)
                            val height = (page.height * factor).toInt().coerceAtLeast(100)

                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE) // PDF backgrounds are white
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val jpeg = java.io.ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, options.qualityPercent.coerceIn(1, 100), jpeg)
                            bitmap.recycle()

                            // PDF page sizes are in points; Word pages are in twips (1 pt = 20 twips).
                            docx.addPage(
                                imageBytes = jpeg.toByteArray(),
                                extension = "jpeg",
                                imageWidthPx = width,
                                imageHeightPx = height,
                                pageWidthTwips = page.width * 20,
                                pageHeightTwips = page.height * 20,
                                marginTwips = 0
                            )
                        } finally {
                            page.close()
                        }
                        onProgress(0.2f + 0.75f * (i + 1) / pageCount)
                    }
                }
            }
        } finally {
            renderer.close()
            pfd.close()
        }
    }

    private fun renderPdfToImage(
        context: Context,
        pdfUri: Uri,
        outputFile: File,
        imageExt: String,
        options: ConversionOptions,
        onProgress: (Float) -> Unit
    ) {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
            ?: throw IllegalArgumentException("Cannot open PDF file descriptor")

        val renderer = PdfRenderer(pfd)
        try {
            if (renderer.pageCount == 0) throw IllegalStateException("Empty PDF document")
            val page = renderer.openPage(0) // Render first page
            onProgress(0.5f)

            val factor = pdfRenderFactor(page.width, page.height, options)

            val width = (page.width * factor).toInt().coerceAtLeast(100)
            val height = (page.height * factor).toInt().coerceAtLeast(100)

            val rawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(rawBitmap)
            canvas.drawColor(Color.WHITE) // PDF backgrounds are white

            page.render(rawBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Apply detail clarity enhancement for high-res / 8K rendering if enabled
            val bitmap = if (options.upscaleSharpen && (options.targetLongSidePx >= 3840 || width >= 3840)) {
                val enhanced = ImageEngine.enhanceUpscaledDetails(rawBitmap, 0.3f)
                if (enhanced != rawBitmap) {
                    rawBitmap.recycle()
                }
                enhanced
            } else {
                rawBitmap
            }

            onProgress(0.8f)
            val out = FileOutputStream(outputFile)
            if (imageExt == "jpg" || imageExt == "jpeg") {
                bitmap.compress(Bitmap.CompressFormat.JPEG, options.qualityPercent, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            out.flush()
            out.close()
            bitmap.recycle()
        } finally {
            renderer.close()
            pfd.close()
        }
    }

    private fun renderTextOrHtmlToPdf(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: ConversionOptions,
        onProgress: (Float) -> Unit
    ) {
        val inputStream = context.contentResolver.openInputStream(inputUri)
            ?: throw IllegalArgumentException("Cannot read source document")
        val content = inputStream.bufferedReader().use { it.readText() }

        onProgress(0.4f)

        val isLandscape = options.pdfOrientation.equals("Landscape", ignoreCase = true)
        val pageWidth = if (isLandscape) 842 else 595 // A4 standard pt
        val pageHeight = if (isLandscape) 595 else 842

        val pdfDocument = PdfDocument()
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(37, 99, 235) // Cobalt Blue
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val lines = content.lines()
        val margin = 48f
        val lineHeight = 18f
        val maxLinesPerPage = ((pageHeight - (margin * 2)) / lineHeight).toInt()

        var currentPageNumber = 1
        var lineIndex = 0

        while (lineIndex < lines.size || lineIndex == 0) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var currentY = margin

            // Draw header on first page
            if (currentPageNumber == 1) {
                canvas.drawText("OmniConvert Document Export", margin, currentY, headerPaint)
                currentY += 24f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
                currentY += 20f
            }

            var linesInThisPage = 0
            while (lineIndex < lines.size && linesInThisPage < maxLinesPerPage) {
                val rawLine = lines[lineIndex]
                // Strip rudimentary HTML if source is html
                val cleanLine = rawLine.replace(Regex("<[^>]*>"), "")
                canvas.drawText(cleanLine.take(90), margin, currentY, paint)
                currentY += lineHeight
                lineIndex++
                linesInThisPage++
            }

            pdfDocument.finishPage(page)
            currentPageNumber++
            if (lineIndex >= lines.size) break
        }

        onProgress(0.85f)
        val out = FileOutputStream(outputFile)
        pdfDocument.writeTo(out)
        out.flush()
        out.close()
        pdfDocument.close()
    }

    private fun convertMarkdownToHtml(context: Context, inputUri: Uri, outputFile: File, onProgress: (Float) -> Unit) {
        val text = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText() } ?: ""
        onProgress(0.5f)

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>Converted Document</title>\n")
        htmlBuilder.append("<style>body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;line-height:1.6;padding:24px;max-width:800px;margin:auto;color:#1e293b;}</style>\n</head>\n<body>\n")

        for (line in text.lines()) {
            when {
                line.startsWith("# ") -> htmlBuilder.append("<h1>").append(escapeHtml(line.drop(2))).append("</h1>\n")
                line.startsWith("## ") -> htmlBuilder.append("<h2>").append(escapeHtml(line.drop(3))).append("</h2>\n")
                line.startsWith("### ") -> htmlBuilder.append("<h3>").append(escapeHtml(line.drop(4))).append("</h3>\n")
                line.startsWith("- ") -> htmlBuilder.append("<li>").append(escapeHtml(line.drop(2))).append("</li>\n")
                line.isBlank() -> htmlBuilder.append("<br/>\n")
                else -> htmlBuilder.append("<p>").append(escapeHtml(line)).append("</p>\n")
            }
        }
        htmlBuilder.append("</body>\n</html>")

        outputFile.writeText(htmlBuilder.toString())
    }

    private fun convertHtmlToTextOrMd(context: Context, inputUri: Uri, outputFile: File, toMd: Boolean, onProgress: (Float) -> Unit) {
        val html = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText() } ?: ""
        onProgress(0.5f)

        var converted = html
            .replace(Regex("<style[\\s\\S]*?</style>"), "")
            .replace(Regex("<script[\\s\\S]*?</script>"), "")
            .replace("<h1>", if (toMd) "\n# " else "\n")
            .replace("<h2>", if (toMd) "\n## " else "\n")
            .replace("<h3>", if (toMd) "\n### " else "\n")
            .replace("<li>", if (toMd) "\n* " else "\n• ")
            .replace(Regex("</h[1-6]>"), "\n")
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace("</p>", "\n\n")
            .replace(Regex("<[^>]*>"), "")
            .trim()

        outputFile.writeText(converted)
    }

    private fun transcodeTextDocument(
        context: Context,
        inputUri: Uri,
        inputFormat: FileFormat,
        targetFormat: FileFormat,
        outputFile: File,
        onProgress: (Float) -> Unit
    ) {
        val content = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText() } ?: ""
        onProgress(0.5f)

        when (targetFormat.extension) {
            "html" -> {
                val escaped = escapeHtml(content).replace("\n", "<br/>\n")
                val html = "<!DOCTYPE html><html><body><pre style=\"font-family:sans-serif;\">$escaped</pre></body></html>"
                outputFile.writeText(html)
            }
            "rtf" -> {
                val rtf = "{\\rtf1\\ansi\\deff0 {\\fonttbl {\\f0 Arial;}}\\f0\\fs24 " +
                        content.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}").replace("\n", "\\par ") +
                        "}"
                outputFile.writeText(rtf)
            }
            else -> {
                outputFile.writeText(content)
            }
        }
    }

    private fun extractTextFromPdf(context: Context, inputUri: Uri, outputFile: File, onProgress: (Float) -> Unit) {
        val inputStream = context.contentResolver.openInputStream(inputUri) ?: return
        val raw = inputStream.bufferedReader(Charsets.ISO_8859_1).use { it.readText() }
        onProgress(0.5f)

        val sb = StringBuilder()
        // Extract literal PDF string text blocks
        val tjRegex = Regex("""\(([^()]+)\)\s*Tj""")
        val tjMatches = tjRegex.findAll(raw)
        for (m in tjMatches) {
            sb.append(m.groupValues[1]).append(" ")
        }

        // If tjMatches is empty, parse printable text streams
        if (sb.isBlank()) {
            val streamRegex = Regex("""stream[\r\n]+([\s\S]*?)[\r\n]+endstream""")
            for (match in streamRegex.findAll(raw)) {
                val streamContent = match.groupValues[1]
                val words = streamContent.split(Regex("[^a-zA-Z0-9.,!?'\" -]+")).filter { it.length > 2 }
                if (words.size > 5) {
                    sb.append(words.joinToString(" ")).append("\n")
                }
            }
        }

        val extracted = if (sb.isNotBlank()) sb.toString().trim() else "PDF Document Content"
        outputFile.writeText(extracted)
    }

    private fun convertDocxDocument(
        context: Context,
        inputUri: Uri,
        targetFormat: FileFormat,
        outputFile: File,
        options: ConversionOptions,
        onProgress: (Float) -> Unit
    ) {
        val inputStream = context.contentResolver.openInputStream(inputUri) ?: return
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry
        var docXml = ""

        while (entry != null) {
            if (entry.name == "word/document.xml") {
                docXml = zipIn.bufferedReader().readText()
                break
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
        onProgress(0.6f)

        // Parse paragraphs and text runs
        val sb = StringBuilder()
        val pRegex = Regex("""<w:p(?: [^>]*)?>([\s\S]*?)</w:p>""")
        val tRegex = Regex("""<w:t(?: [^>]*)?>([^<]*)</w:t>""")

        for (pMatch in pRegex.findAll(docXml)) {
            val pContent = pMatch.groupValues[1]
            val pText = StringBuilder()
            for (tMatch in tRegex.findAll(pContent)) {
                pText.append(tMatch.groupValues[1])
            }
            if (pText.isNotBlank()) {
                sb.append(pText.toString()).append("\n")
            }
        }

        val text = if (sb.isNotBlank()) sb.toString().trim() else "Extracted Word Document"

        when (targetFormat.extension) {
            "html" -> {
                val html = "<!DOCTYPE html><html><body>" +
                        text.lines().joinToString("") { "<p>${escapeHtml(it)}</p>" } +
                        "</body></html>"
                outputFile.writeText(html)
            }
            "pdf" -> {
                val tempTextFile = File(outputFile.parentFile, "temp_${System.currentTimeMillis()}.txt")
                tempTextFile.writeText(text)
                val tempUri = Uri.fromFile(tempTextFile)
                renderTextOrHtmlToPdf(context, tempUri, outputFile, options, onProgress)
                tempTextFile.delete()
            }
            else -> {
                outputFile.writeText(text)
            }
        }
    }

    private fun convertXlsxDocument(
        context: Context,
        inputUri: Uri,
        targetFormat: FileFormat,
        outputFile: File,
        onProgress: (Float) -> Unit
    ) {
        val inputStream = context.contentResolver.openInputStream(inputUri) ?: return
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry
        val sharedStrings = mutableListOf<String>()
        var sheetXml = ""

        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml") {
                val xml = zipIn.bufferedReader().readText()
                val tRegex = Regex("""<t(?: [^>]*)?>([^<]*)</t>""")
                for (m in tRegex.findAll(xml)) {
                    sharedStrings.add(m.groupValues[1])
                }
            } else if (entry.name == "xl/worksheets/sheet1.xml") {
                sheetXml = zipIn.bufferedReader().readText()
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        zipIn.close()
        onProgress(0.6f)

        // Parse rows and cell values
        val csvBuilder = StringBuilder()
        val rowRegex = Regex("""<row(?: [^>]*)?>([\s\S]*?)</row>""")
        val cellRegex = Regex("""<c(?: [^>]*)?(?: t="([s])")?[^>]*><v>([^<]*)</v></c>""")

        for (rMatch in rowRegex.findAll(sheetXml)) {
            val rContent = rMatch.groupValues[1]
            val rowValues = mutableListOf<String>()
            for (cMatch in cellRegex.findAll(rContent)) {
                val isShared = cMatch.groupValues[1] == "s"
                val rawVal = cMatch.groupValues[2]
                val cellText = if (isShared) {
                    val idx = rawVal.toIntOrNull() ?: 0
                    sharedStrings.getOrNull(idx) ?: rawVal
                } else {
                    rawVal
                }
                rowValues.add(cellText)
            }
            if (rowValues.isNotEmpty()) {
                csvBuilder.append(rowValues.joinToString(",") { "\"$it\"" }).append("\n")
            }
        }

        val csvContent = if (csvBuilder.isNotBlank()) csvBuilder.toString() else "Column1,Column2\nValue1,Value2"

        when (targetFormat.extension) {
            "json" -> {
                val json = DataEngine.convertData(context, inputUri, FormatRegistry.CSV, FormatRegistry.JSON, outputFile.parentFile) { }
                json.copyTo(outputFile, overwrite = true)
                json.delete()
            }
            else -> {
                outputFile.writeText(csvContent)
            }
        }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}

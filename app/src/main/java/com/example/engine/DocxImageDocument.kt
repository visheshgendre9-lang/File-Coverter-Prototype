package com.example.engine

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal .docx writer where every page holds one picture.
 *
 * A .docx file is a zip of XML files, so no extra library is needed. This class deliberately uses
 * no Android classes: callers pass already-encoded JPEG/PNG bytes, which keeps it testable on a
 * plain JVM.
 *
 * Usage:
 *   DocxImageDocument(outputStream).use { doc ->
 *       doc.addPage(jpegBytes, "jpeg", widthPx, heightPx, pageWidthTwips, pageHeightTwips, marginTwips)
 *   }
 *
 * Units: 1 point = 20 twips, 1 inch = 1440 twips, 1 twip = 635 EMU.
 */
class DocxImageDocument(output: OutputStream) : java.io.Closeable {

    private val zip = ZipOutputStream(output)
    private val relationships = StringBuilder()
    private val body = StringBuilder()
    private var pageCount = 0
    private var finished = false

    // The newest page is held back: every page except the last carries its section break inside
    // its own paragraph, while the last page's section settings go at the end of the body.
    private var pendingRun: String? = null
    private var pendingSection: String? = null

    init {
        // [Content_Types].xml must be the first entry in the package.
        writeEntry("[Content_Types].xml", CONTENT_TYPES.toByteArray(Charsets.UTF_8))
        writeEntry("_rels/.rels", ROOT_RELS.toByteArray(Charsets.UTF_8))
    }

    /**
     * Adds one page that shows the picture.
     *
     * @param imageBytes encoded image data
     * @param extension "jpeg" or "png"
     * @param imageWidthPx / imageHeightPx pixel size of the picture (only the ratio matters)
     * @param pageWidthTwips / pageHeightTwips page size
     * @param marginTwips same margin on all four sides (0 = picture fills the page)
     */
    fun addPage(
        imageBytes: ByteArray,
        extension: String,
        imageWidthPx: Int,
        imageHeightPx: Int,
        pageWidthTwips: Int,
        pageHeightTwips: Int,
        marginTwips: Int
    ) {
        check(!finished) { "Document already finished" }
        require(imageWidthPx > 0 && imageHeightPx > 0) { "Image size must be positive" }
        val ext = extension.lowercase()
        require(ext == "jpeg" || ext == "png") { "Only jpeg and png pictures are supported" }

        // Word rejects pages larger than 22 inches; shrink very large pages proportionally.
        var pageW = pageWidthTwips.toDouble()
        var pageH = pageHeightTwips.toDouble()
        val largest = maxOf(pageW, pageH)
        if (largest > MAX_PAGE_TWIPS) {
            val shrink = MAX_PAGE_TWIPS / largest
            pageW *= shrink
            pageH *= shrink
        }
        val margin = marginTwips.coerceAtLeast(0).coerceAtMost((minOf(pageW, pageH) / 4).toInt())

        pageCount++
        val index = pageCount
        val mediaName = "image$index.$ext"

        // Flush the previous page now that we know it is not the last one.
        flushPending(isLast = false)

        writeEntry("word/media/$mediaName", imageBytes)
        relationships.append(
            "<Relationship Id=\"rId$index\" " +
                "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" " +
                "Target=\"media/$mediaName\"/>"
        )

        // Fit the picture inside the page's content area, keeping its shape. A 1% safety gap
        // stops Word from pushing the paragraph mark onto an extra blank page.
        val contentW = (pageW - 2.0 * margin) * EMU_PER_TWIP
        val contentH = (pageH - 2.0 * margin) * EMU_PER_TWIP * 0.99
        val scale = minOf(contentW / imageWidthPx, contentH / imageHeightPx)
        val cx = (imageWidthPx * scale).toLong().coerceAtLeast(1L)
        val cy = (imageHeightPx * scale).toLong().coerceAtLeast(1L)

        pendingRun = drawingRun(index, mediaName, cx, cy)
        pendingSection = sectionProperties(pageW.toInt(), pageH.toInt(), margin)
    }

    /** Writes the document body and closes the file. Calling it twice is harmless. */
    override fun close() {
        if (finished) return
        finished = true
        try {
            require(pageCount > 0) { "A document needs at least one page" }
            flushPending(isLast = true)
            writeEntry("word/_rels/document.xml.rels", documentRels().toByteArray(Charsets.UTF_8))
            writeEntry("word/document.xml", documentXml().toByteArray(Charsets.UTF_8))
        } finally {
            zip.close()
        }
    }

    // ---------------------------------------------------------------------------------------

    private var lastSection: String = ""

    private fun flushPending(isLast: Boolean) {
        val run = pendingRun ?: return
        val section = pendingSection ?: ""
        if (isLast) {
            // The last page: its paragraph has no section break, its settings close the body.
            body.append("<w:p><w:pPr>$PARAGRAPH_LAYOUT</w:pPr>$run</w:p>")
            lastSection = section
        } else {
            body.append("<w:p><w:pPr>$PARAGRAPH_LAYOUT$section</w:pPr>$run</w:p>")
        }
        pendingRun = null
        pendingSection = null
    }

    private fun drawingRun(id: Int, mediaName: String, cx: Long, cy: Long): String =
        "<w:r><w:drawing>" +
            "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
            "<wp:extent cx=\"$cx\" cy=\"$cy\"/>" +
            "<wp:docPr id=\"$id\" name=\"Picture $id\"/>" +
            "<wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect=\"1\"/></wp:cNvGraphicFramePr>" +
            "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
            "<pic:pic>" +
            "<pic:nvPicPr><pic:cNvPr id=\"$id\" name=\"$mediaName\"/><pic:cNvPicPr/></pic:nvPicPr>" +
            "<pic:blipFill><a:blip r:embed=\"rId$id\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>" +
            "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"$cx\" cy=\"$cy\"/></a:xfrm>" +
            "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>" +
            "</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"

    private fun sectionProperties(pageW: Int, pageH: Int, margin: Int): String =
        "<w:sectPr><w:pgSz w:w=\"$pageW\" w:h=\"$pageH\"/>" +
            "<w:pgMar w:top=\"$margin\" w:right=\"$margin\" w:bottom=\"$margin\" w:left=\"$margin\" " +
            "w:header=\"0\" w:footer=\"0\" w:gutter=\"0\"/></w:sectPr>"

    private fun documentXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
            "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" " +
            "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" " +
            "xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
            "<w:body>$body$lastSection</w:body></w:document>"

    private fun documentRels(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "$relationships</Relationships>"

    private fun writeEntry(name: String, data: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(data)
        zip.closeEntry()
    }

    private companion object {
        const val EMU_PER_TWIP = 635.0
        const val MAX_PAGE_TWIPS = 31000.0

        // No spacing around the paragraph, picture centred.
        const val PARAGRAPH_LAYOUT =
            "<w:spacing w:before=\"0\" w:after=\"0\" w:line=\"240\" w:lineRule=\"auto\"/><w:jc w:val=\"center\"/>"

        const val CONTENT_TYPES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>" +
                "<Default Extension=\"png\" ContentType=\"image/png\"/>" +
                "<Override PartName=\"/word/document.xml\" " +
                "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                "</Types>"

        const val ROOT_RELS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" " +
                "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" " +
                "Target=\"word/document.xml\"/></Relationships>"
    }
}

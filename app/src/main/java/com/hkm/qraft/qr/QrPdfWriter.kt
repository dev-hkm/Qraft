package com.hkm.qraft.qr

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Lightweight, zero-dependency ISO 32000 (PDF 1.4) writer embedding 300 DPI
 * Baseline JPEG (/DCTDecode) A4 pages so single-page, multi-QR print sheets,
 * and multi-page batch PDFs stay compact, OOM-safe, and platform-independent.
 */
object QrPdfWriter {

    const val A4_WIDTH_PX = 2480
    const val A4_HEIGHT_PX = 3508
    private const val A4_WIDTH_PT = 595
    private const val A4_HEIGHT_PT = 842

    fun writeSinglePage(renderPage: () -> Bitmap): ByteArray =
        writePages(pageCount = 1) { renderPage() }

    fun writePages(pageCount: Int, renderPage: (pageIndex: Int) -> Bitmap): ByteArray {
        require(pageCount > 0) { "PDF must contain at least 1 page" }

        val out = ByteArrayOutputStream(64 * 1024)
        val offsets = mutableListOf<Int>()

        fun writeAscii(text: String) {
            out.write(text.toByteArray(Charsets.US_ASCII))
        }

        fun beginObject(objId: Int) {
            while (offsets.size < objId) offsets.add(0)
            offsets[objId - 1] = out.size()
            writeAscii("$objId 0 obj\n")
        }

        fun endObject() {
            writeAscii("\nendobj\n")
        }

        writeAscii("%PDF-1.4\n")
        out.write(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        // Obj 1: Catalog, Obj 2: Pages
        beginObject(1)
        writeAscii("<< /Type /Catalog /Pages 2 0 R >>")
        endObject()

        // Reserve space indices: each page uses 3 objects: Page (p), Content (c), Image XObject (i)
        // Page objects start at objId = 3
        val pageObjIds = (0 until pageCount).map { 3 + it * 3 }

        beginObject(2)
        val kids = pageObjIds.joinToString(" ") { "$it 0 R" }
        writeAscii("<< /Type /Pages /Count $pageCount /Kids [$kids] >>")
        endObject()

        for (index in 0 until pageCount) {
            val pageObjId = 3 + index * 3
            val contentObjId = pageObjId + 1
            val imageObjId = pageObjId + 2
            val imName = "Im$index"

            val bmp = renderPage(index)
            val jpegBaos = ByteArrayOutputStream()
            val w = bmp.width
            val h = bmp.height
            try {
                bmp.compress(Bitmap.CompressFormat.JPEG, 93, jpegBaos)
            } finally {
                bmp.recycle()
            }
            val jpegBytes = jpegBaos.toByteArray()

            // Page dictionary
            beginObject(pageObjId)
            writeAscii(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $A4_WIDTH_PT $A4_HEIGHT_PT] " +
                    "/Resources << /XObject << /$imName $imageObjId 0 R >> >> " +
                    "/Contents $contentObjId 0 R >>"
            )
            endObject()

            // Page content stream drawing full A4 image
            val streamCmd = "q\n$A4_WIDTH_PT 0 0 $A4_HEIGHT_PT 0 0 cm\n/$imName Do\nQ\n"
            val streamBytes = streamCmd.toByteArray(Charsets.US_ASCII)
            beginObject(contentObjId)
            writeAscii("<< /Length ${streamBytes.size} >>\nstream\n")
            out.write(streamBytes)
            writeAscii("endstream")
            endObject()

            // DCTDecode JPEG image XObject
            beginObject(imageObjId)
            writeAscii(
                "<< /Type /XObject /Subtype /Image /Width $w /Height $h " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode " +
                    "/Length ${jpegBytes.size} >>\nstream\n"
            )
            out.write(jpegBytes)
            writeAscii("\nendstream")
            endObject()
        }

        val totalObjects = 2 + pageCount * 3
        val xrefOffset = out.size()
        writeAscii("xref\n")
        writeAscii("0 ${totalObjects + 1}\n")
        writeAscii("0000000000 65535 f \n")
        for (i in 0 until totalObjects) {
            writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        writeAscii("trailer\n<< /Size ${totalObjects + 1} /Root 1 0 R >>\n")
        writeAscii("startxref\n$xrefOffset\n%%EOF\n")

        return out.toByteArray()
    }
}

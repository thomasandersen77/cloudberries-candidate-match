package no.cloudberries.candidatematch.utils

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.InputStream

object PdfUtils {
    fun extractText(inputStream: InputStream): String {
        PDDocument.load(inputStream).use {
            return PDFTextStripper()
                .getText(it)
                .replace("\n", " ")
        }
    }
}


package com.gowtham.pocketaudiobook.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

sealed class PdfExtractionResult {
    data class Success(
        val title: String,
        val text: String,
        val pageCount: Int
    ) : PdfExtractionResult()

    data class Error(val message: String) : PdfExtractionResult()
}

object PdfExtractor {

    @Volatile
    private var isLoaderInitialized = false

    private fun ensureLoaderInitialized(context: Context) {
        if (!isLoaderInitialized) {
            synchronized(this) {
                if (!isLoaderInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    isLoaderInitialized = true
                }
            }
        }
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri): PdfExtractionResult =
        withContext(Dispatchers.IO) {
            ensureLoaderInitialized(context)

            var fileName = getFileName(context, uri) ?: "Imported PDF"
            if (fileName.endsWith(".pdf", ignoreCase = true)) {
                fileName = fileName.substring(0, fileName.length - 4)
            }

            var inputStream: InputStream? = null
            var document: PDDocument? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext PdfExtractionResult.Error("Failed to open file stream.")

                document = PDDocument.load(inputStream)

                if (document.isEncrypted) {
                    return@withContext PdfExtractionResult.Error("Encrypted or password-protected PDFs are not supported.")
                }

                val pageCount = document.numberOfPages
                val stripper = PDFTextStripper()
                val rawText = stripper.getText(document)

                val cleanedText = rawText?.trim() ?: ""
                if (cleanedText.isBlank()) {
                    return@withContext PdfExtractionResult.Error(
                        "No selectable text found in this PDF. Scanned PDFs or image-only documents without OCR are not supported."
                    )
                }

                PdfExtractionResult.Success(
                    title = fileName,
                    text = cleanedText,
                    pageCount = pageCount
                )
            } catch (e: Exception) {
                PdfExtractionResult.Error(
                    "Error parsing PDF: ${e.localizedMessage ?: "Unknown error"}"
                )
            } finally {
                try {
                    document?.close()
                } catch (_: Exception) {}
                try {
                    inputStream?.close()
                } catch (_: Exception) {}
            }
        }

    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

package com.ghostgramlabs.pettibox.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ghostgramlabs.pettibox.data.repository.SaveRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Extracts text from a PDF by rasterizing each page (PdfRenderer is built
 * into Android since API 21) and running ML Kit OCR on the bitmap. We cap
 * pages and resolution to keep memory bounded — if a PDF is born-digital,
 * the OCR'd output is still searchable even if not perfect.
 */
@HiltWorker
class PdfTextWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val repository: SaveRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        if (itemId <= 0L) return Result.failure()

        return runCatching {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val pdfText = StringBuilder()

            val pfd: ParcelFileDescriptor = ctx.contentResolver
                .openFileDescriptor(Uri.parse(uriStr), "r")
                ?: return@runCatching Result.success()

            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { pdf ->
                    val pageCount = minOf(pdf.pageCount, MAX_PAGES)
                    for (i in 0 until pageCount) {
                        pdf.openPage(i).use { page ->
                            val scale = TARGET_WIDTH.toFloat() / page.width.coerceAtLeast(1)
                            val w = (page.width * scale).toInt().coerceAtLeast(1)
                            val h = (page.height * scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val text = suspendCancellableCoroutine<String> { cont ->
                                recognizer.process(InputImage.fromBitmap(bmp, 0))
                                    .addOnSuccessListener { cont.resume(it.text) }
                                    .addOnFailureListener { cont.resumeWithException(it) }
                            }
                            bmp.recycle()
                            if (text.isNotBlank()) {
                                pdfText.append(text).append("\n\n")
                            }
                        }
                    }
                }
            }

            if (pdfText.isNotBlank()) {
                repository.setOcrText(itemId, pdfText.toString().trim())
            }
            Result.success()
        }.getOrElse { Result.success() }
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val KEY_URI = "uri"
        private const val MAX_PAGES = 30
        private const val TARGET_WIDTH = 1400

        fun enqueue(ctx: Context, itemId: Long, uri: String) {
            val req = OneTimeWorkRequestBuilder<PdfTextWorker>()
                .addTag(OcrWorkTags.TEXT_INDEXING)
                .setInputData(workDataOf(KEY_ITEM_ID to itemId, KEY_URI to uri))
                .build()
            WorkManager.getInstance(ctx).enqueue(req)
        }
    }
}

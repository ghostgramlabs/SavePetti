package com.savepetti.data.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies a shared content:// Uri (typically temporary, granted by another app)
 * into our own [Context.getFilesDir] so the file survives revoked URI
 * permissions and process death. Returns a stable file:// URI we own.
 *
 * Without this, every screenshot saved from WhatsApp/Instagram becomes a
 * broken thumbnail once the source app revokes the grant (often within a day),
 * and OCR can never re-read the image.
 */
@Singleton
class AttachmentStore @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val baseDir: File by lazy {
        File(ctx.filesDir, "attachments").apply { mkdirs() }
    }

    suspend fun ingest(source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ext = guessExtension(source) ?: "bin"
            val target = File(baseDir, "${UUID.randomUUID()}.$ext")
            ctx.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@runCatching null
            target.toUri().toString()
        }.getOrNull()
    }

    /**
     * Best-effort cleanup of files we own. Anything not under [baseDir] is
     * ignored — we never touch URIs outside our sandbox.
     */
    suspend fun deleteByUris(uris: List<String>) = withContext(Dispatchers.IO) {
        for (s in uris) {
            runCatching {
                val uri = Uri.parse(s)
                if (uri.scheme != "file") return@runCatching
                val path = uri.path ?: return@runCatching
                val file = File(path)
                if (file.exists() && file.parentFile?.canonicalPath == baseDir.canonicalPath) {
                    file.delete()
                }
            }
        }
    }

    private fun guessExtension(uri: Uri): String? {
        val resolver: ContentResolver = ctx.contentResolver
        val mime = resolver.getType(uri)
        if (mime != null) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            if (!ext.isNullOrBlank()) return ext
        }
        return uri.lastPathSegment?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
    }
}

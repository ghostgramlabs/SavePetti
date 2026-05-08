package com.savepetti.ui.screens.save

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable

data class IncomingShare(
    val text: String? = null,
    val urls: List<String> = emptyList(),
    val imageUris: List<Uri> = emptyList(),
    val fileUris: List<Uri> = emptyList(),
    val mimeType: String? = null
) {
    val hasAnything: Boolean
        get() = !text.isNullOrBlank() || urls.isNotEmpty() || imageUris.isNotEmpty() || fileUris.isNotEmpty()

    companion object {
        fun from(intent: Intent?): IncomingShare {
            if (intent == null) return IncomingShare()
            val mime = intent.type
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val combinedText = listOfNotNull(subject, text).joinToString("\n").ifBlank { null }
            val urls = combinedText?.let { extractUrls(it) }.orEmpty()

            val streamUris: List<Uri> = when (intent.action) {
                Intent.ACTION_SEND -> listOfNotNull(intent.parcelable<Uri>(Intent.EXTRA_STREAM))
                Intent.ACTION_SEND_MULTIPLE ->
                    intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM).orEmpty()
                else -> emptyList()
            }
            val imageUris = if (mime?.startsWith("image/") == true) streamUris else emptyList()
            val fileUris = if (mime?.startsWith("image/") != true) streamUris else emptyList()

            return IncomingShare(
                text = combinedText,
                urls = urls,
                imageUris = imageUris,
                fileUris = fileUris,
                mimeType = mime
            )
        }

        private fun extractUrls(s: String): List<String> {
            val regex = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", RegexOption.IGNORE_CASE)
            return regex.findAll(s).map { it.value }.toList()
        }

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        private inline fun <reified T : Parcelable> Intent.parcelable(name: String): T? =
            if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(name, T::class.java)
            else getParcelableExtra(name) as? T

        @Suppress("DEPRECATION")
        private inline fun <reified T : Parcelable> Intent.parcelableArrayList(name: String): ArrayList<T>? =
            if (Build.VERSION.SDK_INT >= 33) getParcelableArrayListExtra(name, T::class.java)
            else getParcelableArrayListExtra<T>(name)
    }
}

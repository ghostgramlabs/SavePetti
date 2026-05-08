package com.savepetti.domain.model

enum class ContentType {
    LINK, IMAGE, TEXT, PDF, FILE, NOTE;

    companion object {
        fun fromMime(mime: String?): ContentType = when {
            mime == null -> NOTE
            mime.startsWith("image/") -> IMAGE
            mime == "application/pdf" -> PDF
            mime.startsWith("text/") -> TEXT
            else -> FILE
        }
    }
}

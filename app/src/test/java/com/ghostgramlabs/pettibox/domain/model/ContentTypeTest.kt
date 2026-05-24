package com.ghostgramlabs.pettibox.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentTypeTest {
    @Test
    fun fromMimeMapsSaveTypes() {
        assertEquals(ContentType.IMAGE, ContentType.fromMime("image/png"))
        assertEquals(ContentType.PDF, ContentType.fromMime("application/pdf"))
        assertEquals(ContentType.TEXT, ContentType.fromMime("text/plain"))
        assertEquals(ContentType.FILE, ContentType.fromMime("application/zip"))
        assertEquals(ContentType.NOTE, ContentType.fromMime(null))
    }
}

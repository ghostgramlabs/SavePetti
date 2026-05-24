package com.ghostgramlabs.pettibox.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQuerySanitizerTest {
    @Test
    fun sanitizeFtsQueryKeepsUsefulTokensAndPrefixesThem() {
        assertEquals(
            "recipe* pdf*",
            SearchQuerySanitizer.sanitizeFtsQuery(" recipe: pdf! ")
        )
    }

    @Test
    fun sanitizeFtsQueryDropsOneLetterNoise() {
        assertEquals(
            "AI* notes*",
            SearchQuerySanitizer.sanitizeFtsQuery("a AI & notes")
        )
    }
}

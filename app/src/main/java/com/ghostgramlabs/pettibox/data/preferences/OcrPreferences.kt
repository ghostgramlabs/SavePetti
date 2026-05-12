package com.ghostgramlabs.pettibox.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ocrDataStore by preferencesDataStore(name = "ocr_preferences")

@Singleton
class OcrPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val autoScanKey = booleanPreferencesKey("auto_scan_images_and_pdfs")
    private val pdfPageLimitKey = intPreferencesKey("pdf_page_limit")

    val autoScan: Flow<Boolean> = context.ocrDataStore.data.map { prefs ->
        prefs[autoScanKey] ?: true
    }

    val pdfPageLimit: Flow<Int> = context.ocrDataStore.data.map { prefs ->
        prefs[pdfPageLimitKey]?.coerceIn(MIN_PDF_PAGES, MAX_PDF_PAGES) ?: DEFAULT_PDF_PAGES
    }

    suspend fun setAutoScan(enabled: Boolean) {
        context.ocrDataStore.edit { prefs ->
            prefs[autoScanKey] = enabled
        }
    }

    suspend fun setPdfPageLimit(limit: Int) {
        context.ocrDataStore.edit { prefs ->
            prefs[pdfPageLimitKey] = limit.coerceIn(MIN_PDF_PAGES, MAX_PDF_PAGES)
        }
    }

    companion object {
        const val MIN_PDF_PAGES = 10
        const val DEFAULT_PDF_PAGES = 30
        const val MAX_PDF_PAGES = 50
        val PDF_PAGE_LIMIT_OPTIONS = listOf(10, 30, 50)
    }
}

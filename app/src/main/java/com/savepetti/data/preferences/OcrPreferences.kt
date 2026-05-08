package com.savepetti.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    val autoScan: Flow<Boolean> = context.ocrDataStore.data.map { prefs ->
        prefs[autoScanKey] ?: true
    }

    suspend fun setAutoScan(enabled: Boolean) {
        context.ocrDataStore.edit { prefs ->
            prefs[autoScanKey] = enabled
        }
    }
}

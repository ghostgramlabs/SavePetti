package com.ghostgramlabs.pettibox.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupDataStore by preferencesDataStore(name = "backup_preferences")

data class LocalBackupStatus(
    val enabled: Boolean,
    val lastBackupAt: Long,
    val lastBackupName: String
)

@Singleton
class BackupPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val autoLocalBackupKey = booleanPreferencesKey("auto_local_backup")
    private val lastBackupAtKey = longPreferencesKey("last_local_backup_at")
    private val lastBackupNameKey = stringPreferencesKey("last_local_backup_name")

    val status: Flow<LocalBackupStatus> = context.backupDataStore.data.map { prefs ->
        LocalBackupStatus(
            enabled = prefs[autoLocalBackupKey] ?: false,
            lastBackupAt = prefs[lastBackupAtKey] ?: 0L,
            lastBackupName = prefs[lastBackupNameKey].orEmpty()
        )
    }

    suspend fun setAutoLocalBackup(enabled: Boolean) {
        context.backupDataStore.edit { prefs ->
            prefs[autoLocalBackupKey] = enabled
        }
    }

    suspend fun recordLocalBackup(fileName: String, timestamp: Long = System.currentTimeMillis()) {
        context.backupDataStore.edit { prefs ->
            prefs[lastBackupAtKey] = timestamp
            prefs[lastBackupNameKey] = fileName
        }
    }
}

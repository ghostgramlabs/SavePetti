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
    val lastBackupName: String,
    val folderUri: String,
    val lastCopyFailedAt: Long
)

data class DriveBackupStatus(
    val enabled: Boolean,
    /** Google account the backups upload to — "" until the first about-lookup succeeds. */
    val accountEmail: String,
    val lastBackupAt: Long,
    val lastBackupName: String,
    /** Set when a background upload found consent revoked/expired — the user must reconnect. */
    val needsReconnectAt: Long,
    /** Set when the last upload failed for a transient reason (offline, Drive error). */
    val lastFailedAt: Long
)

@Singleton
class BackupPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val autoLocalBackupKey = booleanPreferencesKey("auto_local_backup")
    private val lastBackupAtKey = longPreferencesKey("last_local_backup_at")
    private val lastBackupNameKey = stringPreferencesKey("last_local_backup_name")
    private val backupFolderUriKey = stringPreferencesKey("local_backup_folder_uri")
    // Set when the worker successfully wrote a local zip but copying it to
    // the user's picked SAF folder failed (often: tree permission revoked
    // by the system after a while). Settings reads this to show a small
    // "couldn't copy to picked folder" hint.
    private val lastCopyFailedAtKey = longPreferencesKey("last_local_backup_copy_failed_at")

    val status: Flow<LocalBackupStatus> = context.backupDataStore.data.map { prefs ->
        LocalBackupStatus(
            enabled = prefs[autoLocalBackupKey] ?: false,
            lastBackupAt = prefs[lastBackupAtKey] ?: 0L,
            lastBackupName = prefs[lastBackupNameKey].orEmpty(),
            folderUri = prefs[backupFolderUriKey].orEmpty(),
            lastCopyFailedAt = prefs[lastCopyFailedAtKey] ?: 0L
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

    suspend fun recordCopyFailure(timestamp: Long = System.currentTimeMillis()) {
        context.backupDataStore.edit { prefs ->
            prefs[lastCopyFailedAtKey] = timestamp
        }
    }

    suspend fun setBackupFolder(uri: String) {
        context.backupDataStore.edit { prefs ->
            prefs[backupFolderUriKey] = uri
            prefs.remove(lastCopyFailedAtKey)
        }
    }

    suspend fun clearCopyFailure() {
        context.backupDataStore.edit { prefs ->
            prefs.remove(lastCopyFailedAtKey)
        }
    }

    // ── Google Drive backup ───────────────────────────────────────────────

    private val driveEnabledKey = booleanPreferencesKey("drive_backup_enabled")
    private val driveAccountEmailKey = stringPreferencesKey("drive_account_email")
    private val lastDriveBackupAtKey = longPreferencesKey("last_drive_backup_at")
    private val lastDriveBackupNameKey = stringPreferencesKey("last_drive_backup_name")
    private val driveNeedsReconnectAtKey = longPreferencesKey("drive_needs_reconnect_at")
    private val lastDriveFailedAtKey = longPreferencesKey("last_drive_backup_failed_at")

    val driveStatus: Flow<DriveBackupStatus> = context.backupDataStore.data.map { prefs ->
        DriveBackupStatus(
            enabled = prefs[driveEnabledKey] ?: false,
            accountEmail = prefs[driveAccountEmailKey].orEmpty(),
            lastBackupAt = prefs[lastDriveBackupAtKey] ?: 0L,
            lastBackupName = prefs[lastDriveBackupNameKey].orEmpty(),
            needsReconnectAt = prefs[driveNeedsReconnectAtKey] ?: 0L,
            lastFailedAt = prefs[lastDriveFailedAtKey] ?: 0L
        )
    }

    suspend fun setDriveBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { prefs ->
            prefs[driveEnabledKey] = enabled
            prefs.remove(driveNeedsReconnectAtKey)
            prefs.remove(lastDriveFailedAtKey)
            if (!enabled) prefs.remove(driveAccountEmailKey)
        }
    }

    suspend fun recordDriveAccount(email: String) {
        context.backupDataStore.edit { prefs ->
            prefs[driveAccountEmailKey] = email
        }
    }

    suspend fun recordDriveBackup(fileName: String, timestamp: Long = System.currentTimeMillis()) {
        context.backupDataStore.edit { prefs ->
            prefs[lastDriveBackupAtKey] = timestamp
            prefs[lastDriveBackupNameKey] = fileName
            prefs.remove(driveNeedsReconnectAtKey)
            prefs.remove(lastDriveFailedAtKey)
        }
    }

    suspend fun recordDriveNeedsReconnect(timestamp: Long = System.currentTimeMillis()) {
        context.backupDataStore.edit { prefs ->
            prefs[driveNeedsReconnectAtKey] = timestamp
        }
    }

    suspend fun recordDriveFailure(timestamp: Long = System.currentTimeMillis()) {
        context.backupDataStore.edit { prefs ->
            prefs[lastDriveFailedAtKey] = timestamp
        }
    }
}

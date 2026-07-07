package com.ghostgramlabs.pettibox.data.drive

import com.ghostgramlabs.pettibox.data.preferences.BackupPreferences
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates Drive backups for the two callers that need them — the
 * nightly [com.ghostgramlabs.pettibox.data.backup.LocalBackupWorker] and
 * the Settings buttons — so the token/outcome/preferences bookkeeping
 * lives in exactly one place.
 *
 * Every outcome is recorded to [BackupPreferences] here, which is what
 * the Settings screen renders ("Last upload…", "Reconnect needed…").
 */
@Singleton
class DriveBackupManager @Inject constructor(
    private val auth: DriveAuth,
    private val client: DriveBackupClient,
    private val backupPreferences: BackupPreferences
) {
    sealed interface UploadOutcome {
        data class Uploaded(val fileName: String) : UploadOutcome
        /** Consent missing/revoked — only the user can fix this, from Settings. */
        data object NeedsReconnect : UploadOutcome
        /** Transient (offline, Drive hiccup) — the next nightly run retries. */
        data class Failed(val error: Throwable) : UploadOutcome
    }

    /** Worker entry point: no-op returning null when Drive backup is off. */
    suspend fun uploadIfEnabled(file: File): UploadOutcome? {
        val status = backupPreferences.driveStatus.first()
        if (!status.enabled) return null
        return upload(file)
    }

    suspend fun upload(file: File): UploadOutcome {
        val token = auth.silentAccessToken()
        if (token == null) {
            backupPreferences.recordDriveNeedsReconnect()
            return UploadOutcome.NeedsReconnect
        }
        return runCatching {
            val name = client.uploadBackup(token, file)
            client.pruneOldBackups(token)
            name
        }.fold(
            onSuccess = { name ->
                backupPreferences.recordDriveBackup(name)
                // Backfill the account identity for connections made before
                // the email was surfaced in Settings (or if the connect-time
                // lookup failed) — the nightly run heals it.
                if (backupPreferences.driveStatus.first().accountEmail.isBlank()) {
                    client.accountEmail(token)?.let { backupPreferences.recordDriveAccount(it) }
                }
                UploadOutcome.Uploaded(name)
            },
            onFailure = { error ->
                backupPreferences.recordDriveFailure()
                UploadOutcome.Failed(error)
            }
        )
    }

    /** Look up and persist which Google account the Drive consent belongs to. */
    suspend fun refreshAccountEmail() {
        val token = auth.silentAccessToken() ?: return
        client.accountEmail(token)?.let { backupPreferences.recordDriveAccount(it) }
    }

    /** Backups in the Drive folder, newest first. Null when consent is needed. */
    suspend fun listBackups(): List<DriveBackupFile>? {
        val token = auth.silentAccessToken() ?: return null
        return client.listBackups(token)
    }

    /** Content stream for a backup zip. Null when consent is needed. Caller closes. */
    suspend fun openBackupStream(fileId: String): InputStream? {
        val token = auth.silentAccessToken() ?: return null
        return client.openBackupStream(token, fileId)
    }
}

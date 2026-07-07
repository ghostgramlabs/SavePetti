package com.ghostgramlabs.pettibox.data.drive

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Play Services' AuthorizationClient for the
 * `drive.file` scope — the narrow one that only sees files this app
 * created, so users aren't asked to hand over their whole Drive.
 *
 * No client ID lives in code: Play Services matches the app to its
 * Google Cloud OAuth client by package name + signing SHA-1.
 *
 * Two call sites, two behaviours from the same [authorize] call:
 * - Settings (foreground): a result with [AuthorizationResult.hasResolution]
 *   carries a PendingIntent the screen launches to show the consent sheet.
 * - Backup worker (background): consent can't be asked for, so
 *   [silentAccessToken] returns null and the worker flags "reconnect
 *   needed" instead of failing the whole backup.
 */
@Singleton
class DriveAuth @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun authorize(): AuthorizationResult =
        Identity.getAuthorizationClient(context)
            .authorize(
                AuthorizationRequest.builder()
                    .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                    .build()
            )
            .await()

    /** Access token if the user has already consented, else null. Never shows UI. */
    suspend fun silentAccessToken(): String? =
        runCatching { authorize() }
            .getOrNull()
            ?.takeIf { !it.hasResolution() }
            ?.accessToken

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}

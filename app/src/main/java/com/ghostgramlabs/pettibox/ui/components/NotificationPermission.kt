package com.ghostgramlabs.pettibox.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Wraps the Android 13+ runtime notification permission request in a
 * single callable. The returned function takes a deferred [onGranted]
 * action; it either runs that immediately (permission already granted,
 * pre-API-33, or just-granted-via-prompt) or no-ops silently when the
 * user denies. Caller may show their own snackbar in the deny path
 * via [onDenied].
 *
 * Usage:
 * ```
 * val requestNotificationPermission = rememberNotificationPermissionRequester(
 *     onDenied = { scope.launch { snackbar.showSnackbar("Enable notifications in settings to get reminders") } }
 * )
 * // Later, when scheduling a reminder:
 * requestNotificationPermission {
 *     viewModel.setRemindAt(item, atMillis)
 * }
 * ```
 *
 * On Android 12 and below, [POST_NOTIFICATIONS] doesn't exist as a
 * runtime permission — the action runs immediately.
 */
@Composable
fun rememberNotificationPermissionRequester(
    onDenied: () -> Unit = {}
): ((onGranted: () -> Unit) -> Unit) {
    val ctx = LocalContext.current
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val deferred = pending.value
        pending.value = null
        if (granted) deferred?.invoke()
        else onDenied()
    }

    return remember(launcher) {
        request@ { onGranted ->
            // Pre-Tiramisu the permission is granted at install time; just run.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onGranted()
                return@request
            }
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onGranted()
            } else {
                // Stash the deferred action; the launcher result calls it.
                pending.value = onGranted
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

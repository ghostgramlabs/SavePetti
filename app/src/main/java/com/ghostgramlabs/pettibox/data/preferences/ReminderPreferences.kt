package com.ghostgramlabs.pettibox.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore by preferencesDataStore(name = "reminder_preferences")

@Singleton
class ReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationsBlockedKey = booleanPreferencesKey("notifications_blocked")

    val notificationsBlocked: Flow<Boolean> = context.reminderDataStore.data.map { prefs ->
        prefs[notificationsBlockedKey] ?: false
    }

    suspend fun setNotificationsBlocked(blocked: Boolean) {
        context.reminderDataStore.edit { prefs ->
            prefs[notificationsBlockedKey] = blocked
        }
    }
}

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

private val Context.reminderDataStore by preferencesDataStore(name = "reminder_preferences")

/** A wall-clock time of day, used to anchor the quick-reminder presets. */
data class ReminderTime(val hour: Int, val minute: Int)

@Singleton
class ReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationsBlockedKey = booleanPreferencesKey("notifications_blocked")
    // The two anchors the quick-reminder presets snap to. "Tonight" fires at
    // the evening time; "Tomorrow" / "This weekend" / "Next week" fire at the
    // morning time. Stored as hour + minute so the value survives locale and
    // 12/24-hour display changes.
    private val morningHourKey = intPreferencesKey("morning_hour")
    private val morningMinuteKey = intPreferencesKey("morning_minute")
    private val eveningHourKey = intPreferencesKey("evening_hour")
    private val eveningMinuteKey = intPreferencesKey("evening_minute")

    val notificationsBlocked: Flow<Boolean> = context.reminderDataStore.data.map { prefs ->
        prefs[notificationsBlockedKey] ?: false
    }

    val morningTime: Flow<ReminderTime> = context.reminderDataStore.data.map { prefs ->
        ReminderTime(
            hour = (prefs[morningHourKey] ?: DEFAULT_MORNING_HOUR).coerceIn(0, 23),
            minute = (prefs[morningMinuteKey] ?: 0).coerceIn(0, 59)
        )
    }

    val eveningTime: Flow<ReminderTime> = context.reminderDataStore.data.map { prefs ->
        ReminderTime(
            hour = (prefs[eveningHourKey] ?: DEFAULT_EVENING_HOUR).coerceIn(0, 23),
            minute = (prefs[eveningMinuteKey] ?: 0).coerceIn(0, 59)
        )
    }

    suspend fun setNotificationsBlocked(blocked: Boolean) {
        context.reminderDataStore.edit { prefs ->
            prefs[notificationsBlockedKey] = blocked
        }
    }

    suspend fun setMorningTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit { prefs ->
            prefs[morningHourKey] = hour.coerceIn(0, 23)
            prefs[morningMinuteKey] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setEveningTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit { prefs ->
            prefs[eveningHourKey] = hour.coerceIn(0, 23)
            prefs[eveningMinuteKey] = minute.coerceIn(0, 59)
        }
    }

    companion object {
        const val DEFAULT_MORNING_HOUR = 9
        const val DEFAULT_EVENING_HOUR = 21
    }
}

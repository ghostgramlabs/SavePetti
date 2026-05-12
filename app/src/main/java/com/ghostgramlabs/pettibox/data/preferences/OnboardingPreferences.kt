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

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_preferences")

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val completedKey = booleanPreferencesKey("home_onboarding_completed")

    val showHomeOnboarding: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[completedKey] != true
    }

    suspend fun markHomeOnboardingComplete() {
        context.onboardingDataStore.edit { prefs ->
            prefs[completedKey] = true
        }
    }
}

package com.ghostgramlabs.pettibox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ghostgramlabs.pettibox.data.preferences.ThemeMode
import com.ghostgramlabs.pettibox.data.preferences.ThemePreferences
import com.ghostgramlabs.pettibox.data.reminders.ReminderAlarmReceiver
import com.ghostgramlabs.pettibox.ui.nav.PettiBoxNavGraph
import com.ghostgramlabs.pettibox.ui.theme.PettiBoxTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences

    /**
     * Holds the item id from a reminder-notification tap (or any future
     * deep link) until the NavController is composed and can navigate
     * to it. Using mutableStateOf instead of a Channel keeps this
     * idempotent: a second cold-start with the same intent fires once
     * after the NavGraph reads + clears it.
     */
    private var pendingItemOpen by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingItemOpen = readPendingOpenId(intent)
        setContent {
            val themeMode by themePreferences.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            // Remember the value so a recomposition driven by theme change
            // doesn't re-fire the deep link.
            val openTarget = pendingItemOpen
            PettiBoxTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // Transparent so the app-wide paper texture PettiBoxTheme
                    // paints behind everything actually shows through (cards and
                    // sheets draw their own opaque surfaces on top). Painting the
                    // background color here was covering the grain, leaving the
                    // paper reading as flat vector color — the exact "AI tell"
                    // the texture exists to kill. contentColor keeps default text
                    // on-palette since the Surface no longer supplies a bg color.
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    PettiBoxNavGraph(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            lifecycleScope.launch { themePreferences.setMode(mode) }
                        },
                        initialOpenItemId = openTarget,
                        onInitialOpenItemConsumed = {
                            // Clear once the NavGraph has navigated, so a
                            // back-press to Home doesn't bounce the user
                            // straight back into Detail.
                            pendingItemOpen = null
                        }
                    )
                }
            }
        }
    }

    /**
     * Notification-tap path while the activity is already alive. Setting
     * [pendingItemOpen] flips the state Compose is observing; the
     * NavGraph's LaunchedEffect picks it up and navigates without us
     * having to recreate() the activity.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readPendingOpenId(intent)?.let { pendingItemOpen = it }
    }

    private fun readPendingOpenId(intent: Intent?): Long? {
        val id = intent?.getLongExtra(ReminderAlarmReceiver.EXTRA_OPEN_ITEM_ID, -1L) ?: -1L
        return if (id > 0L) id else null
    }
}

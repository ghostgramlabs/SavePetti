package com.ghostgramlabs.pettibox.ui.screens.save

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ghostgramlabs.pettibox.MainActivity
import com.ghostgramlabs.pettibox.data.reminders.ReminderAlarmReceiver
import com.ghostgramlabs.pettibox.ui.theme.PettiBoxTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receives Android share intents (text/url/image/file/PDF) and opens the
 * Save bottom sheet over a transparent activity. Finishes itself once the
 * user saves or dismisses, so it never lands in the recents stack.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    private var incomingShare by mutableStateOf<IncomingShare?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val incoming = IncomingShare.from(intent)
        if (!incoming.hasAnything) {
            finish(); return
        }
        incomingShare = incoming

        setContent {
            incomingShare?.let { share ->
                PettiBoxTheme {
                    SaveSheet(
                        incoming = share,
                        onDismiss = { finish() },
                        onSaved = { finish() },
                        // Duplicate "Open it" → hand off to the main app via
                        // the same deep-link MainActivity uses for reminder
                        // taps, then close this transparent share activity.
                        onOpenExisting = { id ->
                            val i = Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(ReminderAlarmReceiver.EXTRA_OPEN_ITEM_ID, id)
                            }
                            startActivity(i)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incoming = IncomingShare.from(intent)
        if (!incoming.hasAnything) {
            finish()
        } else {
            incomingShare = incoming
        }
    }
}

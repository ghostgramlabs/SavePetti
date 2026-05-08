package com.savepetti.ui.screens.save

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.savepetti.ui.theme.SavePettiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receives Android share intents (text/url/image/file/PDF) and opens the
 * Save bottom sheet over a transparent activity. Finishes itself once the
 * user saves or dismisses, so it never lands in the recents stack.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val incoming = IncomingShare.from(intent)
        if (!incoming.hasAnything) {
            finish(); return
        }

        setContent {
            SavePettiTheme {
                SaveSheet(
                    incoming = incoming,
                    onDismiss = { finish() },
                    onSaved = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}

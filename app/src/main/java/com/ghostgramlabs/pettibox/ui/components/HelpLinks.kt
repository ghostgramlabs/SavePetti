package com.ghostgramlabs.pettibox.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * External help content. The share-sheet flow is the app's core gesture
 * and the hardest one to explain in words, so onboarding and Help both
 * point at a short demo video instead of longer prose.
 */
object HelpLinks {

    /** ~30s screen recording of sharing into PettiBox from another app. */
    const val SHARE_DEMO_VIDEO = "https://www.youtube.com/shorts/SH0tnIFicgg"

    /** Opens in the YouTube app or browser. False when neither exists. */
    fun openShareDemo(context: Context): Boolean = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHARE_DEMO_VIDEO)))
    }.isSuccess
}

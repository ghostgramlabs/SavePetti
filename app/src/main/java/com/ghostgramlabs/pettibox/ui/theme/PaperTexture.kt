package com.ghostgramlabs.pettibox.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

// Low-frequency noise tile applied across the whole app background. Real
// paper isn't perfectly flat — without this overlay the bone color reads
// as "vector graphic," which is the AI tell. ~1.5% black grain is enough
// to add tooth without being visible as "noise."
//
// We bake one 128×128 bitmap once per process and reuse the same
// BitmapShader for every draw. Cost is a single allocation at first use.

private val NoiseBitmap: Bitmap by lazy { generateNoise(size = 128, seed = 7L) }
private val NoiseShader: BitmapShader by lazy {
    BitmapShader(NoiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
}
private val NoisePaint: Paint by lazy {
    Paint().apply {
        shader = NoiseShader
        // Pixel alpha already caps grain darkness; this further dampens
        // the whole layer so it reads as paper tooth not visible noise.
        alpha = 90 // 0..255
        isAntiAlias = false
    }
}

private fun generateNoise(size: Int, seed: Long): Bitmap {
    val rng = java.util.Random(seed)
    val pixels = IntArray(size * size)
    for (i in pixels.indices) {
        val v = rng.nextInt(256)
        // Map 0..255 to 0..22 alpha so the strongest grain pixel is still
        // ~9% black. Most pixels will be dimmer than that.
        val a = (v * 22) / 255
        pixels[i] = a shl 24
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
        it.setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

/**
 * Paints a solid background then a repeating low-opacity noise overlay.
 * Apply once at the app root; descendants should use transparent
 * surface colors so the texture shows through.
 */
fun Modifier.paperTexture(background: Color): Modifier = drawBehind {
    drawRect(background)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, NoisePaint)
    }
}

// Suppress unused-import warning for toArgb if Compose ever complains.
@Suppress("unused")
private val _keep = Color.Transparent.toArgb()

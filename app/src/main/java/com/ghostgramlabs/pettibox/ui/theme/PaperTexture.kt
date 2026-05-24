package com.ghostgramlabs.pettibox.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas

// Low-frequency paper grain painted across the whole app background. Real
// paper has tooth — without it the flat fill reads as "vector graphic," the
// classic AI tell. The grain is THEME-AWARE so the material survives the
// theme switch: near-black specks darken the cream in light mode; warm
// off-white specks catch faint fibre highlights on the charcoal in dark mode.
// Same material in both (a scrapbook is still paper at night), just tuned so
// neither ever crosses from "paper tooth" into "screen noise" — dark is
// deliberately dialled lower since light speckle on charcoal is the easier of
// the two to mistake for sensor noise / OLED artifacts.
//
// We bake each tile once (128×128) and reuse one BitmapShader per tint. Cost
// is two small allocations at first use.

private const val NoiseTile = 128
private const val NoiseSeed = 7L

// Light theme: near-black specks, ~9% max per pixel, then damped by the layer.
private val LightGrainBitmap: Bitmap by lazy { grainTile(rgb = 0x000000, maxAlpha = 22) }
private val LightGrainPaint: Paint by lazy { grainPaint(LightGrainBitmap, layerAlpha = 90) }

// Dark theme: warm off-white specks (matches the CreamOnDark body ink family),
// fewer and fainter so the charcoal stays calm and never reads as noise.
private val DarkGrainBitmap: Bitmap by lazy { grainTile(rgb = 0xE9E2D2, maxAlpha = 18) }
private val DarkGrainPaint: Paint by lazy { grainPaint(DarkGrainBitmap, layerAlpha = 60) }

private fun grainTile(rgb: Int, maxAlpha: Int): Bitmap {
    // Same seed for both tints so the fibre pattern is identical across themes
    // — only the colour and intensity change, never the "grain of the paper."
    val rng = java.util.Random(NoiseSeed)
    val base = rgb and 0x00FFFFFF
    val pixels = IntArray(NoiseTile * NoiseTile)
    for (i in pixels.indices) {
        val v = rng.nextInt(256)
        // Map 0..255 to 0..maxAlpha so the strongest speck is still faint;
        // most pixels land well below that.
        val a = (v * maxAlpha) / 255
        pixels[i] = (a shl 24) or base
    }
    return Bitmap.createBitmap(NoiseTile, NoiseTile, Bitmap.Config.ARGB_8888).also {
        it.setPixels(pixels, 0, NoiseTile, 0, 0, NoiseTile, NoiseTile)
    }
}

private fun grainPaint(bitmap: Bitmap, layerAlpha: Int): Paint = Paint().apply {
    shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    // Per-pixel alpha already caps speck strength; this dampens the whole
    // layer further so it reads as paper tooth, not visible noise.
    alpha = layerAlpha
    isAntiAlias = false
}

/**
 * Paints a solid [background] then a repeating low-opacity paper-grain
 * overlay, choosing dark specks for light backgrounds and warm-light specks
 * for dark ones. Apply once at the app root; descendants should use
 * transparent surface colors so the texture shows through.
 */
fun Modifier.paperTexture(background: Color): Modifier = drawBehind {
    drawRect(background)
    val paint = if (background.luminance() > 0.5f) LightGrainPaint else DarkGrainPaint
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

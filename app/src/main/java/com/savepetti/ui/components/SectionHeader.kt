package com.savepetti.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .drawBehind { drawSquiggleUnderline(accent.copy(alpha = 0.55f)) }
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/**
 * Draws a single-stroke wavy underline ("highlighter squiggle") just under
 * the title. Three or four hand-tilted humps — looks like someone underlined
 * with a felt-tip pen rather than a perfectly straight CSS border.
 *
 * Drawn in the modifier so the underline tracks the actual rendered text
 * width (no need to measure ahead of time).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSquiggleUnderline(color: Color) {
    val baseY = size.height + 3f
    val amp = 1.6f
    val period = 14f
    val path = Path().apply {
        moveTo(0f, baseY)
        var x = 0f
        while (x <= size.width) {
            val y = baseY + amp * sin(x / period * Math.PI.toFloat() * 2f)
            lineTo(x, y)
            x += 2f
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Subtle dot at the end — a flick of the pen — only when the title is wide
    // enough that the dot reads as deliberate.
    if (size.width > 80f) {
        drawCircle(
            color = color,
            radius = 1.6f,
            center = Offset(size.width + 4f, baseY)
        )
    }
}

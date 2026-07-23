package com.p2pchat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * WhatsApp-style subtle repeating doodle pattern background composable.
 * Draws subtle chat icons, signal waves, locks, and nodes with low opacity.
 */
@Composable
fun DoodleChatBackground(
    modifier: Modifier = Modifier,
    doodleColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingX = 80.dp.toPx()
        val spacingY = 80.dp.toPx()
        val rows = (size.height / spacingY).toInt() + 1
        val cols = (size.width / spacingX).toInt() + 1

        for (r in 0..rows) {
            for (c in 0..cols) {
                val x = c * spacingX + (if (r % 2 == 1) spacingX / 2 else 0f)
                val y = r * spacingY
                val type = (r + c) % 4

                when (type) {
                    0 -> drawChatBubbleDoodle(x, y, doodleColor)
                    1 -> drawLockDoodle(x, y, doodleColor)
                    2 -> drawWifiDoodle(x, y, doodleColor)
                    else -> drawStarDoodle(x, y, doodleColor)
                }
            }
        }
    }
}

private fun DrawScope.drawChatBubbleDoodle(x: Float, y: Float, color: Color) {
    drawCircle(
        color = color,
        radius = 8.dp.toPx(),
        center = Offset(x, y),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawLockDoodle(x: Float, y: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(x - 5.dp.toPx(), y - 3.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 10.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawWifiDoodle(x: Float, y: Float, color: Color) {
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(x - 10.dp.toPx(), y - 10.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 20.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )
}

private fun DrawScope.drawStarDoodle(x: Float, y: Float, color: Color) {
    drawLine(color, Offset(x - 5.dp.toPx(), y), Offset(x + 5.dp.toPx(), y), strokeWidth = 1.5.dp.toPx())
    drawLine(color, Offset(x, y - 5.dp.toPx()), Offset(x, y + 5.dp.toPx()), strokeWidth = 1.5.dp.toPx())
}

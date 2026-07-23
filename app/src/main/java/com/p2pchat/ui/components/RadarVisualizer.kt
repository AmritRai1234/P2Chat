package com.p2pchat.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.p2pchat.ui.theme.Cyan40
import com.p2pchat.ui.theme.Cyan60
import com.p2pchat.ui.theme.Purple40
import com.p2pchat.ui.theme.Purple60

/**
 * Animated Sonar Radar Visualizer for offline P2P peer scanning.
 * Displays expanding pulse rings, rotating sweep indicator, and glowing mesh node.
 */
@Composable
fun RadarVisualizer(
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")

    // Ripple 1
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Scale"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Alpha"
    )

    // Ripple 2 (staggered)
    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Scale"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Alpha"
    )

    // Rotation angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radar Circle Stack
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        // Expanding Pulse Ring 1
                        Canvas(modifier = Modifier.size(90.dp * wave1Scale)) {
                            drawCircle(
                                color = Cyan40.copy(alpha = wave1Alpha.coerceIn(0f, 1f)),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        // Expanding Pulse Ring 2
                        Canvas(modifier = Modifier.size(90.dp * wave2Scale)) {
                            drawCircle(
                                color = Purple40.copy(alpha = wave2Alpha.coerceIn(0f, 1f)),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Outer Static Ring with Border Gradient
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Cyan40.copy(alpha = 0.6f),
                                        Purple40.copy(alpha = 0.6f),
                                        Cyan60.copy(alpha = 0.2f),
                                        Cyan40.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Purple40.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScanning) {
                            // Sweep gradient cone
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .rotate(rotationAngle)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Cyan40.copy(alpha = 0.25f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Central Glowing Icon Orb
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isScanning) {
                                        listOf(Cyan40, Purple60)
                                    } else {
                                        listOf(
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Filled.WifiTethering else Icons.Filled.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = if (isScanning) "Searching Nearby Space…" else "Radar Paused",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isScanning) {
                        "Bluetooth & Wi-Fi Direct active.\nBring devices within range to auto-connect."
                    } else {
                        "Tap the radar button to scan for nearby peers without internet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

package com.p2pchat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.p2pchat.ui.theme.Connected
import com.p2pchat.ui.theme.Cyan40
import com.p2pchat.ui.theme.Cyan60
import com.p2pchat.ui.theme.Purple40
import com.p2pchat.ui.theme.Scanning

/**
 * Premium glassmorphism status bar showing connection and scanning state.
 */
@Composable
fun ConnectionStatusBar(
    isAdvertising: Boolean,
    isDiscovering: Boolean,
    connectedCount: Int,
    modifier: Modifier = Modifier
) {
    val isScanning = isAdvertising || isDiscovering
    val hasConnections = connectedCount > 0

    val statusColor by animateColorAsState(
        targetValue = when {
            hasConnections -> Connected
            isScanning -> Scanning
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        label = "statusBarColor"
    )

    val statusText = when {
        hasConnections && isScanning -> "$connectedCount Connected · Scanning"
        hasConnections -> "$connectedCount Connected"
        isScanning -> "Scanning for nearby peers…"
        else -> "P2P Offline"
    }

    val statusIcon = when {
        hasConnections -> Icons.Filled.SignalWifi4Bar
        isScanning -> Icons.Filled.BluetoothSearching
        else -> Icons.Filled.Bluetooth
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    statusColor.copy(alpha = 0.4f),
                    statusColor.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .alpha(if (isScanning) pulseAlpha else 1f)
                    .background(statusColor)
            )

            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = statusColor
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

package com.p2pchat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SignalWifi4Bar
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.p2pchat.domain.model.ConnectionStatus
import com.p2pchat.domain.model.Peer
import com.p2pchat.ui.theme.Connected
import com.p2pchat.ui.theme.Connecting
import com.p2pchat.ui.theme.Disconnected
import com.p2pchat.ui.theme.Purple40
import com.p2pchat.ui.theme.Purple60
import com.p2pchat.ui.theme.Scanning

/**
 * Card component representing a discovered or connected peer.
 * Shows peer name, connection status with animated indicator, and tap-to-connect.
 */
@Composable
fun PeerCard(
    peer: Peer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (peer.connectionStatus) {
            ConnectionStatus.CONNECTED -> Connected
            ConnectionStatus.CONNECTING -> Connecting
            ConnectionStatus.DISCOVERED -> Scanning
            ConnectionStatus.DISCONNECTED -> Disconnected
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "statusColor"
    )

    val statusText = when (peer.connectionStatus) {
        ConnectionStatus.CONNECTED -> "Connected"
        ConnectionStatus.CONNECTING -> "Connecting…"
        ConnectionStatus.DISCOVERED -> "Tap to connect"
        ConnectionStatus.DISCONNECTED -> "Disconnected"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar with gradient background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Purple40, Purple60)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name and status
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Animated status dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Connection type icon
            Icon(
                imageVector = Icons.Filled.Bluetooth,
                contentDescription = "Bluetooth",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

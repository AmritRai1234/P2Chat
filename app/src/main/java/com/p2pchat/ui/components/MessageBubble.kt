package com.p2pchat.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.ui.theme.BubbleReceived
import com.p2pchat.ui.theme.BubbleReceivedLight
import com.p2pchat.ui.theme.BubbleSent
import com.p2pchat.ui.theme.BubbleSentLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat message bubble component with sent/received styling.
 * Supports text messages and file attachments (photos, PDFs, etc.)
 * Sent messages appear on the right with purple gradient.
 * Received messages appear on the left with dark/light surface color.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isSent = message.isFromMe
    val bubbleColor = if (isSent) {
        if (isDarkTheme) BubbleSent else BubbleSentLight
    } else {
        if (isDarkTheme) BubbleReceived else BubbleReceivedLight
    }

    val textColor = if (isSent) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSent) 16.dp else 4.dp,
                        bottomEnd = if (isSent) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Sender name for received messages
            if (!isSent) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // File attachment preview
            if (message.hasAttachment) {
                AttachmentPreview(
                    fileName = message.attachmentFileName ?: "",
                    mimeType = message.attachmentMimeType ?: "",
                    fileSize = message.attachmentSize,
                    localPath = message.attachmentLocalPath,
                    textColor = textColor
                )
                if (message.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Message text
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }

            // Timestamp and delivery status
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.6f)
                )

                if (isSent) {
                    Icon(
                        imageVector = if (message.isDelivered) Icons.Filled.DoneAll else Icons.Filled.Check,
                        contentDescription = if (message.isDelivered) "Delivered" else "Sent",
                        modifier = Modifier.size(14.dp),
                        tint = if (message.isDelivered) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            textColor.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    fileName: String,
    mimeType: String,
    fileSize: Long,
    localPath: String?,
    textColor: androidx.compose.ui.graphics.Color
) {
    val isImage = mimeType.startsWith("image/")
    val isPdf = mimeType == "application/pdf"
    val isVideo = mimeType.startsWith("video/")

    val icon = when {
        isImage -> Icons.Filled.Image
        isPdf -> Icons.Filled.PictureAsPdf
        else -> Icons.Filled.InsertDriveFile
    }

    val displaySize = when {
        fileSize < 1024 -> "${fileSize} B"
        fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
        else -> "${"%.1f".format(fileSize / (1024.0 * 1024.0))} MB"
    }

    // File attachment card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(textColor.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // File type icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isImage -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        isPdf -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        isVideo -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = when {
                    isImage -> MaterialTheme.colorScheme.primary
                    isPdf -> MaterialTheme.colorScheme.error
                    isVideo -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                }
            )
        }

        // File info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$displaySize · ${mimeType.substringAfter("/").uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

package com.p2pchat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting chat messages.
 * Supports both 1-to-1 and group messages via optional groupId.
 * Supports file attachments via attachment fields.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val senderEndpointId: String,
    val senderName: String,
    val peerEndpointId: String,
    val groupId: String? = null,
    val timestamp: Long,
    val isFromMe: Boolean,
    val isDelivered: Boolean,
    val attachmentFileName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentLocalPath: String? = null,
    val attachmentSize: Long = 0
)

package com.p2pchat.domain.model

/**
 * Domain model representing a chat message.
 * Supports text, file attachments, and group messages.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val senderEndpointId: String,
    val senderName: String,
    val recipientEndpointId: String = "",
    val timestamp: Long,
    val isFromMe: Boolean,
    val isDelivered: Boolean = false,
    val groupId: String? = null,
    val attachmentFileName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentLocalPath: String? = null,
    val attachmentSize: Long = 0
) {
    val hasAttachment: Boolean get() = attachmentFileName != null
    val isImageAttachment: Boolean get() = attachmentMimeType?.startsWith("image/") == true
    val isPdfAttachment: Boolean get() = attachmentMimeType == "application/pdf"
}

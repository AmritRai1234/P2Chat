package com.p2pchat.domain.model

/**
 * Represents a file attachment in a chat message.
 */
data class FileAttachment(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val localPath: String? = null,
    val transferProgress: Float = 0f,
    val transferStatus: TransferStatus = TransferStatus.PENDING
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isPdf: Boolean get() = mimeType == "application/pdf"
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isAudio: Boolean get() = mimeType.startsWith("audio/")

    val displaySize: String get() {
        return when {
            fileSize < 1024 -> "${fileSize} B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> "${"%.1f".format(fileSize / (1024.0 * 1024.0))} MB"
        }
    }

    val fileIcon: String get() = when {
        isImage -> "🖼️"
        isPdf -> "📄"
        isVideo -> "🎬"
        isAudio -> "🎵"
        mimeType.contains("zip") || mimeType.contains("rar") -> "📦"
        mimeType.contains("text") -> "📝"
        else -> "📎"
    }
}

enum class TransferStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

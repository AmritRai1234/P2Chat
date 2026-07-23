package com.p2pchat.nearby

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.p2pchat.domain.model.FileAttachment
import com.p2pchat.domain.model.TransferStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages file transfers over Nearby Connections.
 *
 * Supports sending and receiving:
 * - Photos (JPEG, PNG, WebP)
 * - PDFs
 * - Videos
 * - Audio
 * - Any other file type
 *
 * Uses Nearby Connections FILE payload for large files (efficient, zero-copy)
 * and BYTES payload for file metadata (filename, mime type, size).
 */
@Singleton
class FileTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadHandler: PayloadHandler
) {
    companion object {
        private const val TAG = "FileTransferMgr"
        private const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100 MB limit
    }

    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }

    // Track active transfers: payloadId -> FileTransferInfo
    private val _activeTransfers = MutableStateFlow<Map<Long, FileTransferInfo>>(emptyMap())
    val activeTransfers: StateFlow<Map<Long, FileTransferInfo>> = _activeTransfers.asStateFlow()

    // Directory for received files
    private val receivedFilesDir: File by lazy {
        File(context.filesDir, "received_files").also { it.mkdirs() }
    }

    /**
     * Send a file to a peer.
     * First sends metadata as BYTES payload, then sends the actual file.
     */
    fun sendFile(uri: Uri, endpointId: String, senderName: String): FileAttachment? {
        try {
            // Get file info from URI
            val fileInfo = getFileInfo(uri) ?: return null

            if (fileInfo.fileSize > MAX_FILE_SIZE) {
                Log.e(TAG, "File too large: ${fileInfo.displaySize}")
                return null
            }

            // Create file payload
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val filePayload = Payload.fromFile(pfd)
            val payloadId = filePayload.id

            // Create file metadata to send first
            val metadata = FileMetadataPayload(
                payloadId = payloadId,
                fileName = fileInfo.fileName,
                mimeType = fileInfo.mimeType,
                fileSize = fileInfo.fileSize,
                senderName = senderName,
                attachmentId = fileInfo.id
            )

            // Send metadata first (as bytes)
            val metadataBytes = payloadHandler.serializeFileMetadata(metadata)
            val metadataPayload = Payload.fromBytes(metadataBytes)

            // Track this transfer
            val transferInfo = FileTransferInfo(
                payloadId = payloadId,
                attachment = fileInfo,
                endpointId = endpointId,
                isOutgoing = true
            )
            updateTransfer(payloadId, transferInfo)

            // Send metadata, then file
            connectionsClient.sendPayload(endpointId, metadataPayload)
                .addOnSuccessListener {
                    Log.d(TAG, "File metadata sent for ${fileInfo.fileName}")
                    // Now send the actual file
                    connectionsClient.sendPayload(endpointId, filePayload)
                        .addOnSuccessListener {
                            Log.d(TAG, "File payload queued: ${fileInfo.fileName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send file payload", e)
                            updateTransferStatus(payloadId, TransferStatus.FAILED)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send file metadata", e)
                    updateTransferStatus(payloadId, TransferStatus.FAILED)
                }

            return fileInfo
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file", e)
            return null
        }
    }

    /**
     * Handle incoming file metadata — prepare to receive the file.
     */
    fun onFileMetadataReceived(metadata: FileMetadataPayload, fromEndpointId: String) {
        val safeFileName = sanitizeFileName(metadata.fileName)
        val attachment = FileAttachment(
            id = metadata.attachmentId,
            fileName = safeFileName,
            mimeType = metadata.mimeType,
            fileSize = metadata.fileSize,
            transferStatus = TransferStatus.PENDING
        )

        val transferInfo = FileTransferInfo(
            payloadId = metadata.payloadId,
            attachment = attachment,
            endpointId = fromEndpointId,
            isOutgoing = false,
            senderName = metadata.senderName
        )

        updateTransfer(metadata.payloadId, transferInfo)
        Log.d(TAG, "Expecting file: $safeFileName (${attachment.displaySize})")
    }

    private fun sanitizeFileName(rawName: String): String {
        val baseName = java.io.File(rawName).name
        val clean = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return if (clean.isBlank() || clean.startsWith(".")) "file_${System.currentTimeMillis()}" else clean.take(100)
    }

    /**
     * Handle payload transfer progress updates.
     */
    fun onTransferUpdate(payloadId: Long, update: PayloadTransferUpdate) {
        val current = _activeTransfers.value[payloadId] ?: return

        when (update.status) {
            PayloadTransferUpdate.Status.IN_PROGRESS -> {
                val progress = update.bytesTransferred.toFloat() / update.totalBytes.toFloat()
                val updated = current.copy(
                    attachment = current.attachment.copy(
                        transferProgress = progress,
                        transferStatus = TransferStatus.IN_PROGRESS
                    )
                )
                updateTransfer(payloadId, updated)
            }
            PayloadTransferUpdate.Status.SUCCESS -> {
                // Move received file to our storage
                val localPath = if (!current.isOutgoing) {
                    moveReceivedFile(payloadId, current.attachment.fileName)
                } else {
                    current.attachment.localPath
                }

                val updated = current.copy(
                    attachment = current.attachment.copy(
                        transferProgress = 1f,
                        transferStatus = TransferStatus.COMPLETED,
                        localPath = localPath
                    )
                )
                updateTransfer(payloadId, updated)
                Log.d(TAG, "Transfer complete: ${current.attachment.fileName}")
            }
            PayloadTransferUpdate.Status.FAILURE -> {
                val updated = current.copy(
                    attachment = current.attachment.copy(
                        transferStatus = TransferStatus.FAILED
                    )
                )
                updateTransfer(payloadId, updated)
                Log.e(TAG, "Transfer failed: ${current.attachment.fileName}")
            }
            PayloadTransferUpdate.Status.CANCELED -> {
                val updated = current.copy(
                    attachment = current.attachment.copy(
                        transferStatus = TransferStatus.CANCELLED
                    )
                )
                updateTransfer(payloadId, updated)
            }
        }
    }

    /**
     * Get completed transfer info for a given payload ID.
     */
    fun getCompletedTransfer(payloadId: Long): FileTransferInfo? {
        return _activeTransfers.value[payloadId]?.takeIf {
            it.attachment.transferStatus == TransferStatus.COMPLETED
        }
    }

    private fun moveReceivedFile(payloadId: Long, fileName: String): String? {
        try {
            val receivedFile = File(context.cacheDir, payloadId.toString())
            if (receivedFile.exists()) {
                val destFile = File(receivedFilesDir, "${UUID.randomUUID()}_$fileName")
                receivedFile.renameTo(destFile)
                return destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move received file", e)
        }
        return null
    }

    private fun getFileInfo(uri: Uri): FileAttachment? {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    val fileName = if (nameIdx >= 0) it.getString(nameIdx) else "file"
                    val fileSize = if (sizeIdx >= 0) it.getLong(sizeIdx) else 0L
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                    return FileAttachment(
                        id = UUID.randomUUID().toString(),
                        fileName = fileName,
                        mimeType = mimeType,
                        fileSize = fileSize,
                        localPath = uri.toString(),
                        transferStatus = TransferStatus.PENDING
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file info", e)
        }
        return null
    }

    private fun updateTransfer(payloadId: Long, info: FileTransferInfo) {
        val current = _activeTransfers.value.toMutableMap()
        current[payloadId] = info
        _activeTransfers.value = current
    }

    private fun updateTransferStatus(payloadId: Long, status: TransferStatus) {
        val current = _activeTransfers.value[payloadId] ?: return
        updateTransfer(payloadId, current.copy(
            attachment = current.attachment.copy(transferStatus = status)
        ))
    }
}

/**
 * Info about an active file transfer.
 */
data class FileTransferInfo(
    val payloadId: Long,
    val attachment: FileAttachment,
    val endpointId: String,
    val isOutgoing: Boolean,
    val senderName: String = ""
)

/**
 * Metadata sent before the actual file payload.
 */
data class FileMetadataPayload(
    val payloadId: Long,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val senderName: String,
    val attachmentId: String
)

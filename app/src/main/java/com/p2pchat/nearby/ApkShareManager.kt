package com.p2pchat.nearby

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.google.android.gms.nearby.connection.Payload
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for P2P Self-Replication and APK sharing.
 * Enables P2Chat to extract its own running APK binary at runtime and transmit it over Bluetooth/Wi-Fi Direct.
 */
@Singleton
class ApkShareManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nearbyConnectionManager: NearbyConnectionManager,
    private val payloadHandler: PayloadHandler
) {
    companion object {
        private const val TAG = "ApkShareManager"
    }

    /**
     * Get reference to the running P2Chat APK file at runtime.
     */
    fun getInstalledApkFile(): File {
        return File(context.applicationInfo.sourceDir)
    }

    /**
     * Send P2Chat app APK to a connected peer over the mesh network.
     */
    fun sendApkToPeer(endpointId: String) {
        try {
            val apkFile = getInstalledApkFile()
            val fileName = "P2Chat-v1.0.0.apk"
            val fileSize = apkFile.length()

            // 1. Send metadata payload
            val filePayload = Payload.fromFile(apkFile)

            // 1. Send metadata payload
            val metadata = FileMetadataPayload(
                payloadId = filePayload.id,
                fileName = fileName,
                mimeType = "application/vnd.android.package-archive",
                fileSize = fileSize,
                senderName = "P2Chat App",
                attachmentId = java.util.UUID.randomUUID().toString()
            )
            val metadataBytes = payloadHandler.serializeFileMetadata(metadata)
            nearbyConnectionManager.sendBytesPayload(endpointId, metadataBytes)

            // 2. Send actual APK file payload
            nearbyConnectionManager.sendPayload(endpointId, filePayload)
            Log.d(TAG, "Broadcasting P2Chat APK ($fileSize bytes) to $endpointId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share APK over P2P", e)
        }
    }

    /**
     * Launch Android Package Installer when an APK file is received.
     */
    fun installApk(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer", e)
        }
    }
}

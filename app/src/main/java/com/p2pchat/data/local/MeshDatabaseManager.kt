package com.p2pchat.data.local

import android.util.Log
import com.p2pchat.data.model.MeshPeerEntity
import com.p2pchat.data.model.MessageQueueEntity
import com.p2pchat.data.model.NetworkStatsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SQLite-backed mesh network manager.
 * Handles peer tracking, store-and-forward message queue, and network stats.
 * All data stored locally via Room (SQLite) — fully offline, no server needed.
 */
@Singleton
class MeshDatabaseManager @Inject constructor(
    private val meshDao: MeshDao
) {
    companion object {
        private const val TAG = "MeshDB"
    }

    /**
     * Track a discovered or connected peer in the mesh database.
     */
    suspend fun upsertMeshPeer(
        endpointId: String,
        displayName: String,
        publicKey: String? = null,
        isRelayCapable: Boolean = true
    ) {
        try {
            val now = System.currentTimeMillis()
            val existing = meshDao.getMeshPeer(endpointId)
            val peer = MeshPeerEntity(
                endpointId = endpointId,
                displayName = displayName,
                publicKey = publicKey ?: existing?.publicKey,
                trustScore = existing?.trustScore ?: 0.5,
                lastSeenAt = now,
                totalMessagesRelayed = existing?.totalMessagesRelayed ?: 0,
                avgLatencyMs = existing?.avgLatencyMs ?: 0.0,
                isRelayCapable = isRelayCapable,
                createdAt = existing?.createdAt ?: now
            )
            meshDao.upsertMeshPeer(peer)
            Log.d(TAG, "Mesh peer upserted: $endpointId ($displayName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upsert mesh peer: $endpointId", e)
        }
    }

    /**
     * Update trust score for a peer based on interaction quality.
     */
    suspend fun updateTrustScore(endpointId: String, delta: Double) {
        try {
            meshDao.updateTrustScore(endpointId, delta)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update trust score: $endpointId", e)
        }
    }

    /**
     * Queue a message for store-and-forward delivery.
     */
    suspend fun queueMessage(
        id: String,
        payload: ByteArray,
        destinationEndpoint: String,
        groupId: String? = null,
        priority: Int = 0,
        ttlMs: Long = 3600000
    ) {
        try {
            val now = System.currentTimeMillis()
            val entity = MessageQueueEntity(
                id = id,
                payload = payload,
                destinationEndpoint = destinationEndpoint,
                groupId = groupId,
                priority = priority,
                createdAt = now,
                expiresAt = now + ttlMs
            )
            meshDao.queueMessage(entity)
            Log.d(TAG, "Message queued for $destinationEndpoint")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue message", e)
        }
    }

    /**
     * Get pending messages for a destination peer.
     */
    suspend fun getPendingMessages(destinationEndpoint: String): List<MessageQueueEntity> {
        return try {
            meshDao.getPendingMessages(destinationEndpoint, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get pending messages", e)
            emptyList()
        }
    }

    /**
     * Record network statistics.
     */
    suspend fun recordNetworkStats(
        connectedPeers: Int,
        messagesSent: Int,
        messagesReceived: Int,
        messagesRelayed: Int = 0,
        avgLatencyMs: Double = 0.0
    ) {
        try {
            meshDao.insertStats(
                NetworkStatsEntity(
                    timestamp = System.currentTimeMillis(),
                    connectedPeers = connectedPeers,
                    messagesSent = messagesSent,
                    messagesReceived = messagesReceived,
                    messagesRelayed = messagesRelayed,
                    avgLatencyMs = avgLatencyMs
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record stats", e)
        }
    }

    /**
     * Clean up expired messages from the queue.
     */
    suspend fun cleanupExpired() {
        try {
            meshDao.cleanupExpired(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup", e)
        }
    }

    fun getAllMeshPeers(): Flow<List<MeshPeerEntity>> = meshDao.getAllMeshPeers()
    fun getPendingCount(): Flow<Int> = meshDao.getPendingCount()
}

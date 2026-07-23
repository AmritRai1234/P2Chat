package com.p2pchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.p2pchat.data.model.MeshPeerEntity
import com.p2pchat.data.model.MessageQueueEntity
import com.p2pchat.data.model.NetworkStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for mesh networking data — peer tracking, message queue, and stats.
 * All backed by SQLite via Room.
 */
@Dao
interface MeshDao {

    // ── Mesh Peers ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeshPeer(peer: MeshPeerEntity)

    @Query("SELECT * FROM mesh_peers ORDER BY lastSeenAt DESC")
    fun getAllMeshPeers(): Flow<List<MeshPeerEntity>>

    @Query("SELECT * FROM mesh_peers WHERE endpointId = :endpointId")
    suspend fun getMeshPeer(endpointId: String): MeshPeerEntity?

    @Query("UPDATE mesh_peers SET trustScore = MIN(1.0, MAX(0.0, trustScore + :delta)) WHERE endpointId = :endpointId")
    suspend fun updateTrustScore(endpointId: String, delta: Double)

    @Query("UPDATE mesh_peers SET lastSeenAt = :timestamp WHERE endpointId = :endpointId")
    suspend fun updateLastSeen(endpointId: String, timestamp: Long)

    @Query("DELETE FROM mesh_peers WHERE endpointId = :endpointId")
    suspend fun deleteMeshPeer(endpointId: String)

    // ── Message Queue (store-and-forward) ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun queueMessage(message: MessageQueueEntity)

    @Query("SELECT * FROM message_queue WHERE destinationEndpoint = :endpoint AND status = 'PENDING' AND expiresAt > :now ORDER BY priority DESC, createdAt ASC")
    suspend fun getPendingMessages(endpoint: String, now: Long): List<MessageQueueEntity>

    @Query("UPDATE message_queue SET status = 'DELIVERED' WHERE id = :messageId")
    suspend fun markDelivered(messageId: String)

    @Query("UPDATE message_queue SET retryCount = retryCount + 1 WHERE id = :messageId")
    suspend fun incrementRetry(messageId: String)

    @Query("DELETE FROM message_queue WHERE expiresAt < :now OR status = 'DELIVERED'")
    suspend fun cleanupExpired(now: Long)

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    // ── Network Stats ──

    @Insert
    suspend fun insertStats(stats: NetworkStatsEntity)

    @Query("SELECT * FROM network_stats ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentStats(limit: Int = 50): List<NetworkStatsEntity>
}

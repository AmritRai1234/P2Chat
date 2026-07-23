package com.p2pchat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking mesh peer metadata.
 * Stores trust scores, relay capability, and connection stats.
 */
@Entity(tableName = "mesh_peers")
data class MeshPeerEntity(
    @PrimaryKey
    val endpointId: String,
    val displayName: String,
    val publicKey: String? = null,
    val trustScore: Double = 0.5,
    val lastSeenAt: Long,
    val totalMessagesRelayed: Int = 0,
    val avgLatencyMs: Double = 0.0,
    val isRelayCapable: Boolean = true,
    val createdAt: Long
)

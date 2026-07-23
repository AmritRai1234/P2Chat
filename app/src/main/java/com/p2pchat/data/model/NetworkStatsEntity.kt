package com.p2pchat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for tracking network performance stats.
 */
@Entity(tableName = "network_stats")
data class NetworkStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val connectedPeers: Int = 0,
    val messagesSent: Int = 0,
    val messagesReceived: Int = 0,
    val messagesRelayed: Int = 0,
    val avgLatencyMs: Double = 0.0
)

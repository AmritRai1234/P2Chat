package com.p2pchat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting known peer devices.
 */
@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey
    val endpointId: String,
    val name: String,
    val connectionStatus: String,
    val lastSeen: Long
)

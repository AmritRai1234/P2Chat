package com.p2pchat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the store-and-forward message queue.
 * Queues messages when destination peer is offline.
 */
@Entity(tableName = "message_queue")
data class MessageQueueEntity(
    @PrimaryKey
    val id: String,
    val payload: ByteArray,
    val destinationEndpoint: String,
    val groupId: String? = null,
    val priority: Int = 0,
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val createdAt: Long,
    val expiresAt: Long,
    val status: String = "PENDING"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageQueueEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

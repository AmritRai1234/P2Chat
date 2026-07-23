package com.p2pchat.domain.repository

import com.p2pchat.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat message operations.
 */
interface ChatRepository {
    fun getMessagesForPeer(endpointId: String): Flow<List<ChatMessage>>
    fun getMessagesForGroup(groupId: String): Flow<List<ChatMessage>>
    fun getAllRecentMessages(): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun markAsDelivered(messageId: String)
    suspend fun deleteMessagesForPeer(endpointId: String)
}

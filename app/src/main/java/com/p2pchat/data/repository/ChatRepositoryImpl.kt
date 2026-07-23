package com.p2pchat.data.repository

import com.p2pchat.data.local.MessageDao
import com.p2pchat.data.model.MessageEntity
import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository backed by Room database.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : ChatRepository {

    override fun getMessagesForPeer(endpointId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForPeer(endpointId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessagesForGroup(groupId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForGroup(groupId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllRecentMessages(): Flow<List<ChatMessage>> {
        return messageDao.getLatestMessagePerPeer().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        messageDao.insertMessage(message.toEntity())
    }

    override suspend fun markAsDelivered(messageId: String) {
        messageDao.markAsDelivered(messageId)
    }

    override suspend fun deleteMessagesForPeer(endpointId: String) {
        messageDao.deleteMessagesForPeer(endpointId)
    }

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        text = text,
        senderEndpointId = senderEndpointId,
        senderName = senderName,
        recipientEndpointId = peerEndpointId,
        timestamp = timestamp,
        isFromMe = isFromMe,
        isDelivered = isDelivered,
        groupId = groupId,
        attachmentFileName = attachmentFileName,
        attachmentMimeType = attachmentMimeType,
        attachmentLocalPath = attachmentLocalPath,
        attachmentSize = attachmentSize
    )

    private fun ChatMessage.toEntity() = MessageEntity(
        id = id,
        text = text,
        senderEndpointId = senderEndpointId,
        senderName = senderName,
        peerEndpointId = if (isFromMe) recipientEndpointId else senderEndpointId,
        groupId = groupId,
        timestamp = timestamp,
        isFromMe = isFromMe,
        isDelivered = isDelivered,
        attachmentFileName = attachmentFileName,
        attachmentMimeType = attachmentMimeType,
        attachmentLocalPath = attachmentLocalPath,
        attachmentSize = attachmentSize
    )
}

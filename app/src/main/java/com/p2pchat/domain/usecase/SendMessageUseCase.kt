package com.p2pchat.domain.usecase

import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.domain.repository.ChatRepository
import com.p2pchat.nearby.NearbyConnectionManager
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for sending a message to a connected peer.
 * Handles both network transmission and local persistence.
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val nearbyConnectionManager: NearbyConnectionManager
) {
    suspend operator fun invoke(
        text: String,
        recipientEndpointId: String,
        senderName: String
    ): Result<ChatMessage> {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            senderEndpointId = "me",
            senderName = senderName,
            recipientEndpointId = recipientEndpointId,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            isDelivered = false
        )

        return try {
            // Send via Nearby Connections
            nearbyConnectionManager.sendMessage(recipientEndpointId, message)

            // Save locally with delivered status
            val deliveredMessage = message.copy(isDelivered = true)
            chatRepository.saveMessage(deliveredMessage)

            Result.success(deliveredMessage)
        } catch (e: Exception) {
            // Save locally even if send fails (for retry)
            chatRepository.saveMessage(message)
            Result.failure(e)
        }
    }
}

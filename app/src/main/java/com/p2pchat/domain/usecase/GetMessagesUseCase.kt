package com.p2pchat.domain.usecase

import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving messages for a specific peer conversation.
 */
class GetMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(peerEndpointId: String): Flow<List<ChatMessage>> {
        return chatRepository.getMessagesForPeer(peerEndpointId)
    }
}

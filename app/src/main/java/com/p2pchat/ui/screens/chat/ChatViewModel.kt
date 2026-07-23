package com.p2pchat.ui.screens.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.domain.model.ConnectionStatus
import com.p2pchat.domain.repository.ChatRepository
import com.p2pchat.domain.repository.GroupRepository
import com.p2pchat.domain.repository.PeerRepository
import com.p2pchat.domain.usecase.SendMessageUseCase
import com.p2pchat.nearby.FileTransferManager
import com.p2pchat.nearby.NearbyConnectionManager
import com.p2pchat.nearby.NearbyEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val peerName: String = "",
    val peerEndpointId: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val peerRepository: PeerRepository,
    private val groupRepository: GroupRepository,
    private val cryptoManager: com.p2pchat.crypto.CryptoManager,
    private val nearbyConnectionManager: NearbyConnectionManager,
    private val fileTransferManager: FileTransferManager
) : ViewModel() {

    val peerEndpointId: String = savedStateHandle["endpointId"] ?: ""
    val groupId: String? = savedStateHandle["groupId"]
    private val initialName: String = savedStateHandle["peerName"] ?: savedStateHandle["groupName"] ?: "Contact"

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTED)
    private val _isSending = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _displayName = MutableStateFlow("P2Chat User")
    private val _resolvedName = MutableStateFlow(
        if (initialName == "Unknown" || initialName.isBlank()) "Peer" else initialName
    )

    private val messagesFlow = if (!groupId.isNullOrBlank()) {
        chatRepository.getMessagesForGroup(groupId)
    } else {
        chatRepository.getMessagesForPeer(peerEndpointId)
    }

    val uiState: StateFlow<ChatUiState> = combine(
        messagesFlow,
        _connectionStatus,
        _resolvedName,
        _isSending,
        _errorMessage
    ) { messages, status, peerName, isSending, error ->
        ChatUiState(
            messages = messages,
            peerName = peerName,
            peerEndpointId = peerEndpointId,
            connectionStatus = status,
            isSending = isSending,
            errorMessage = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatUiState(peerName = _resolvedName.value, peerEndpointId = peerEndpointId)
    )

    init {
        observeNearbyEvents()
        ensureNetworkActive()
        resolveRealName()
    }

    private fun resolveRealName() {
        if (peerEndpointId.isNotBlank() && (initialName == "Unknown" || initialName == "Peer")) {
            viewModelScope.launch {
                val peer = peerRepository.getPeer(peerEndpointId)
                if (peer != null && peer.name.isNotBlank() && peer.name != "Unknown") {
                    _resolvedName.value = peer.name
                }
            }
        }
    }

    private fun ensureNetworkActive() {
        if (!nearbyConnectionManager.isAdvertising.value && !nearbyConnectionManager.isDiscovering.value) {
            nearbyConnectionManager.startScanning(_displayName.value)
        }
    }

    private fun observeNearbyEvents() {
        viewModelScope.launch {
            nearbyConnectionManager.events.collect { event ->
                when (event) {
                    is NearbyEvent.MessageReceived -> {
                        if (event.fromEndpointId == peerEndpointId) {
                            val receivedMessage = event.message.copy(
                                isFromMe = false,
                                senderEndpointId = peerEndpointId
                            )
                            chatRepository.saveMessage(receivedMessage)
                        }
                    }
                    is NearbyEvent.GroupMessageReceived -> {
                        if (event.groupId == groupId) {
                            viewModelScope.launch {
                                val group = groupRepository.getGroup(groupId).firstOrNull()
                                val inviteCode = group?.inviteCode ?: "DEFAULT_KEY"
                                val decryptedText = cryptoManager.decryptGroupText(
                                    event.message.text,
                                    groupId,
                                    inviteCode
                                )
                                val receivedMessage = event.message.copy(
                                    text = decryptedText,
                                    isFromMe = false,
                                    groupId = groupId
                                )
                                chatRepository.saveMessage(receivedMessage)
                            }
                        }
                    }
                    is NearbyEvent.ConnectionChanged -> {
                        if (event.endpointId == peerEndpointId) {
                            _connectionStatus.value = event.status
                        }
                    }
                    is NearbyEvent.Error -> {
                        _errorMessage.value = event.message
                    }
                    else -> { /* handled elsewhere */ }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        ensureNetworkActive()

        viewModelScope.launch {
            _isSending.value = true
            val trimmed = text.trim()

            if (!groupId.isNullOrBlank()) {
                val group = groupRepository.getGroup(groupId).firstOrNull()
                val inviteCode = group?.inviteCode ?: "DEFAULT_KEY"
                val encryptedText = cryptoManager.encryptGroupText(trimmed, groupId, inviteCode)

                val messageToSave = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = trimmed,
                    senderEndpointId = "me",
                    senderName = _displayName.value,
                    groupId = groupId,
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    isDelivered = true
                )
                // Save plaintext locally for self view
                chatRepository.saveMessage(messageToSave)

                val messageToSend = messageToSave.copy(text = encryptedText)
                val connectedEndpoints = nearbyConnectionManager.connectedEndpoints.value.keys.toList()
                nearbyConnectionManager.sendGroupMessage(groupId, messageToSend, connectedEndpoints)
            } else {
                val result = sendMessageUseCase(
                    text = trimmed,
                    recipientEndpointId = peerEndpointId,
                    senderName = _displayName.value
                )
                result.onFailure { e ->
                    _errorMessage.value = "Failed to send: ${e.message}"
                }
            }
            _isSending.value = false
        }
    }

    /**
     * Send a file (photo, PDF, document, etc.) to the current peer.
     */
    fun sendFile(uri: Uri) {
        viewModelScope.launch {
            _isSending.value = true

            val attachment = fileTransferManager.sendFile(
                uri = uri,
                endpointId = peerEndpointId,
                senderName = _displayName.value
            )

            if (attachment != null) {
                // Save a message with the attachment info
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "",
                    senderEndpointId = "me",
                    senderName = _displayName.value,
                    recipientEndpointId = peerEndpointId,
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    isDelivered = false,
                    attachmentFileName = attachment.fileName,
                    attachmentMimeType = attachment.mimeType,
                    attachmentLocalPath = attachment.localPath,
                    attachmentSize = attachment.fileSize
                )
                chatRepository.saveMessage(message)
            } else {
                _errorMessage.value = "Failed to send file"
            }

            _isSending.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

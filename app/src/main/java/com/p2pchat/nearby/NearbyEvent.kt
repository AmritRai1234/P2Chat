package com.p2pchat.nearby

import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.domain.model.ConnectionStatus
import com.p2pchat.domain.model.Peer

/**
 * Sealed class representing events emitted by the NearbyConnectionManager.
 */
sealed class NearbyEvent {
    data class PeerDiscovered(val peer: Peer) : NearbyEvent()
    data class PeerLost(val endpointId: String) : NearbyEvent()
    data class ConnectionRequested(val endpointId: String, val peerName: String) : NearbyEvent()
    data class ConnectionChanged(val endpointId: String, val status: ConnectionStatus) : NearbyEvent()
    data class MessageReceived(val message: ChatMessage, val fromEndpointId: String) : NearbyEvent()
    data class Error(val message: String, val exception: Exception? = null) : NearbyEvent()
    data class ScanningStateChanged(val isAdvertising: Boolean, val isDiscovering: Boolean) : NearbyEvent()

    // Group events
    data class GroupMessageReceived(val groupId: String, val message: ChatMessage, val fromEndpointId: String) : NearbyEvent()
    data class GroupInviteReceived(val invite: GroupInvitePayload, val fromEndpointId: String) : NearbyEvent()
    data class JoinRequestReceived(val request: JoinRequestPayload, val fromEndpointId: String) : NearbyEvent()
    data class JoinApproved(val approval: JoinApprovalPayload) : NearbyEvent()

    // File transfer events
    data class FileMetadataReceived(val metadata: FileMetadataPayload, val fromEndpointId: String) : NearbyEvent()
}

package com.p2pchat.domain.repository

import com.p2pchat.domain.model.Peer
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for peer device operations.
 */
interface PeerRepository {
    fun getAllPeers(): Flow<List<Peer>>
    suspend fun savePeer(peer: Peer)
    suspend fun updatePeerStatus(endpointId: String, status: com.p2pchat.domain.model.ConnectionStatus)
    suspend fun removePeer(endpointId: String)
    suspend fun getPeer(endpointId: String): Peer?
}

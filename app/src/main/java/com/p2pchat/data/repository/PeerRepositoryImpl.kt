package com.p2pchat.data.repository

import com.p2pchat.data.local.PeerDao
import com.p2pchat.data.model.PeerEntity
import com.p2pchat.domain.model.ConnectionStatus
import com.p2pchat.domain.model.Peer
import com.p2pchat.domain.repository.PeerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PeerRepository backed by Room database.
 */
@Singleton
class PeerRepositoryImpl @Inject constructor(
    private val peerDao: PeerDao
) : PeerRepository {

    override fun getAllPeers(): Flow<List<Peer>> {
        return peerDao.getAllPeers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun savePeer(peer: Peer) {
        peerDao.insertPeer(peer.toEntity())
    }

    override suspend fun updatePeerStatus(endpointId: String, status: ConnectionStatus) {
        peerDao.updateStatus(endpointId, status.name)
    }

    override suspend fun removePeer(endpointId: String) {
        peerDao.deletePeer(endpointId)
    }

    override suspend fun getPeer(endpointId: String): Peer? {
        return peerDao.getPeer(endpointId)?.toDomain()
    }

    private fun PeerEntity.toDomain() = Peer(
        endpointId = endpointId,
        name = name,
        connectionStatus = try {
            ConnectionStatus.valueOf(connectionStatus)
        } catch (_: Exception) {
            ConnectionStatus.DISCOVERED
        },
        lastSeen = lastSeen
    )

    private fun Peer.toEntity() = PeerEntity(
        endpointId = endpointId,
        name = name,
        connectionStatus = connectionStatus.name,
        lastSeen = lastSeen
    )
}

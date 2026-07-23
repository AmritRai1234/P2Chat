package com.p2pchat.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p2pchat.domain.model.ConnectionStatus
import com.p2pchat.domain.model.Peer
import com.p2pchat.domain.repository.PeerRepository
import com.p2pchat.nearby.NearbyConnectionManager
import com.p2pchat.nearby.NearbyEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val peers: List<Peer> = emptyList(),
    val isScanning: Boolean = false,
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val connectedCount: Int = 0,
    val displayName: String = "P2Chat User",
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val nearbyConnectionManager: NearbyConnectionManager,
    private val peerRepository: PeerRepository
) : ViewModel() {

    private val _discoveredPeers = MutableStateFlow<Map<String, Peer>>(emptyMap())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _displayName = MutableStateFlow("P2Chat User")

    val uiState: StateFlow<HomeUiState> = combine(
        _discoveredPeers,
        nearbyConnectionManager.isAdvertising,
        nearbyConnectionManager.isDiscovering,
        nearbyConnectionManager.connectedEndpoints,
        _errorMessage
    ) { peers, isAdvertising, isDiscovering, connected, error ->
        // Merge discovered peers with connected peers
        val allPeers = peers.toMutableMap()
        connected.forEach { (id, peer) ->
            allPeers[id] = peer
        }

        HomeUiState(
            peers = allPeers.values.toList().sortedByDescending {
                when (it.connectionStatus) {
                    ConnectionStatus.CONNECTED -> 3
                    ConnectionStatus.CONNECTING -> 2
                    ConnectionStatus.DISCOVERED -> 1
                    ConnectionStatus.DISCONNECTED -> 0
                }
            },
            isScanning = isAdvertising || isDiscovering,
            isAdvertising = isAdvertising,
            isDiscovering = isDiscovering,
            connectedCount = connected.size,
            displayName = _displayName.value,
            errorMessage = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    init {
        observeNearbyEvents()
    }

    private fun observeNearbyEvents() {
        viewModelScope.launch {
            nearbyConnectionManager.events.collect { event ->
                when (event) {
                    is NearbyEvent.PeerDiscovered -> {
                        _discoveredPeers.update { map ->
                            map + (event.peer.endpointId to event.peer)
                        }
                        // Persist peer
                        viewModelScope.launch {
                            peerRepository.savePeer(event.peer)
                        }
                    }
                    is NearbyEvent.PeerLost -> {
                        _discoveredPeers.update { map ->
                            map - event.endpointId
                        }
                    }
                    is NearbyEvent.ConnectionChanged -> {
                        _discoveredPeers.update { map ->
                            val existing = map[event.endpointId]
                            if (existing != null) {
                                map + (event.endpointId to existing.copy(connectionStatus = event.status))
                            } else {
                                map
                            }
                        }
                        // Update in DB
                        viewModelScope.launch {
                            peerRepository.updatePeerStatus(event.endpointId, event.status)
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

    fun startScanning() {
        _errorMessage.value = null
        nearbyConnectionManager.startScanning(_displayName.value)
    }

    fun stopScanning() {
        nearbyConnectionManager.stopScanning()
    }

    fun toggleScanning() {
        if (uiState.value.isScanning) {
            stopScanning()
        } else {
            startScanning()
        }
    }

    fun connectToPeer(endpointId: String) {
        _discoveredPeers.update { map ->
            val existing = map[endpointId]
            if (existing != null) {
                map + (endpointId to existing.copy(connectionStatus = ConnectionStatus.CONNECTING))
            } else {
                map
            }
        }
        nearbyConnectionManager.connectToEndpoint(endpointId)
    }

    fun setDisplayName(name: String) {
        _displayName.value = name
        nearbyConnectionManager.setDisplayName(name)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        nearbyConnectionManager.stopAll()
    }
}

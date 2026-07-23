package com.p2pchat.domain.model

/**
 * Domain model representing a discovered or connected peer device.
 */
data class Peer(
    val endpointId: String,
    val name: String,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCOVERED,
    val lastSeen: Long = System.currentTimeMillis()
)

enum class ConnectionStatus {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

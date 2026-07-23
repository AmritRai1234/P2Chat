package com.p2pchat.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.p2pchat.crypto.P2PRateLimiter
import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.domain.model.ConnectionStatus
import com.p2pchat.domain.model.Peer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core networking manager that wraps the Google Nearby Connections API.
 *
 * Handles:
 * - Advertising this device so others can discover it
 * - Discovering nearby devices
 * - Managing connection lifecycle (request → accept → connected → disconnected)
 * - Sending and receiving chat message payloads
 *
 * Uses P2P_CLUSTER strategy for multi-device mesh-like topology.
 */
@Singleton
class NearbyConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val payloadHandler: PayloadHandler,
    private val meshDb: com.p2pchat.data.local.MeshDatabaseManager,
    private val rateLimiter: P2PRateLimiter
) {
    companion object {
        private const val TAG = "NearbyConnectionMgr"
        private const val SERVICE_ID = "com.p2pchat.nearby"
    }

    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val strategy = Strategy.P2P_CLUSTER
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Events stream for the UI layer
    private val _events = MutableSharedFlow<NearbyEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<NearbyEvent> = _events.asSharedFlow()

    // Connected endpoints tracking
    private val _connectedEndpoints = MutableStateFlow<Map<String, Peer>>(emptyMap())
    val connectedEndpoints: StateFlow<Map<String, Peer>> = _connectedEndpoints.asStateFlow()

    // Scanning state
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Store display name for advertising
    private var localDisplayName: String = "P2Chat User"

    // Auto-accept connections flag (can be controlled by settings)
    private var autoAcceptConnections = true

    // Pending connection requests
    private val pendingConnections = mutableMapOf<String, String>() // endpointId -> peerName

    /**
     * Set the local display name used when advertising to other devices.
     */
    fun setDisplayName(name: String) {
        localDisplayName = name
    }

    /**
     * Start advertising this device AND discovering others simultaneously.
     * This is the main "go online" action.
     */
    fun startScanning(displayName: String = localDisplayName) {
        localDisplayName = displayName
        startAdvertising()
        startDiscovery()
    }

    /**
     * Stop both advertising and discovery.
     */
    fun stopScanning() {
        stopAdvertising()
        stopDiscovery()
    }

    /**
     * Start advertising this device so others can discover it.
     */
    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startAdvertising(
            localDisplayName,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully")
            _isAdvertising.value = true
            emitEvent(NearbyEvent.ScanningStateChanged(_isAdvertising.value, _isDiscovering.value))
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start advertising", e)
            _isAdvertising.value = false
            emitEvent(NearbyEvent.Error("Failed to start advertising: ${e.message}", e as? Exception))
        }
    }

    /**
     * Start discovering nearby devices that are advertising.
     */
    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully")
            _isDiscovering.value = true
            emitEvent(NearbyEvent.ScanningStateChanged(_isAdvertising.value, _isDiscovering.value))
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery", e)
            _isDiscovering.value = false
            emitEvent(NearbyEvent.Error("Failed to start discovery: ${e.message}", e as? Exception))
        }
    }

    private fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
        emitEvent(NearbyEvent.ScanningStateChanged(_isAdvertising.value, _isDiscovering.value))
    }

    private fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
        emitEvent(NearbyEvent.ScanningStateChanged(_isAdvertising.value, _isDiscovering.value))
    }

    /**
     * Initiate a connection request to a discovered endpoint.
     */
    fun connectToEndpoint(endpointId: String) {
        connectionsClient.requestConnection(
            localDisplayName,
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Log.d(TAG, "Connection request sent to $endpointId")
            emitEvent(NearbyEvent.ConnectionChanged(endpointId, ConnectionStatus.CONNECTING))
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to request connection to $endpointId", e)
            emitEvent(NearbyEvent.Error("Connection request failed: ${e.message}", e as? Exception))
        }
    }

    /**
     * Accept a pending connection request from a peer.
     */
    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Accepted connection from $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to accept connection from $endpointId", e)
                emitEvent(NearbyEvent.Error("Failed to accept connection: ${e.message}", e as? Exception))
            }
    }

    /**
     * Reject a pending connection request.
     */
    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
        pendingConnections.remove(endpointId)
    }

    /**
     * Check if a specific endpoint is currently connected.
     */
    fun isConnected(endpointId: String): Boolean {
        return _connectedEndpoints.value.containsKey(endpointId)
    }

    /**
     * Send a chat message to a peer.
     * If the peer is not connected, automatically queues in SQLite for store-and-forward retry.
     */
    fun sendMessage(endpointId: String, message: ChatMessage) {
        val bytes = payloadHandler.serializeMessage(message)
        val payload = Payload.fromBytes(bytes)

        if (!isConnected(endpointId)) {
            coroutineScope.launch {
                meshDb.queueMessage(
                    id = message.id,
                    payload = bytes,
                    destinationEndpoint = endpointId,
                    groupId = message.groupId
                )
            }
            emitEvent(NearbyEvent.Error("Peer is offline. Message queued in SQLite for delivery when reconnected."))
            return
        }

        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent to $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message to $endpointId", e)
                coroutineScope.launch {
                    meshDb.queueMessage(
                        id = message.id,
                        payload = bytes,
                        destinationEndpoint = endpointId,
                        groupId = message.groupId
                    )
                }
                val userMsg = if (e.message?.contains("8011") == true || e.message?.contains("ENDPOINT_UNKNOWN") == true) {
                    "Peer is offline. Message saved to queue."
                } else {
                    "Message queued for delivery"
                }
                emitEvent(NearbyEvent.Error(userMsg, e as? Exception))
            }
    }

    /**
     * Send raw byte payload to an endpoint.
     */
    fun sendBytesPayload(endpointId: String, bytes: ByteArray) {
        val payload = Payload.fromBytes(bytes)
        sendPayload(endpointId, payload)
    }

    /**
     * Send generic payload (BYTES, FILE, STREAM) to an endpoint.
     */
    fun sendPayload(endpointId: String, payload: Payload) {
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Payload sent to $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send payload to $endpointId", e)
            }
    }

    /**
     * Send a group message to all connected group members.
     */
    fun sendGroupMessage(groupId: String, message: ChatMessage, memberEndpointIds: List<String>) {
        val bytes = payloadHandler.serializeGroupMessage(message, groupId)
        val payload = Payload.fromBytes(bytes)

        memberEndpointIds.forEach { endpointId ->
            if (_connectedEndpoints.value.containsKey(endpointId)) {
                connectionsClient.sendPayload(endpointId, payload)
                    .addOnSuccessListener {
                        Log.d(TAG, "Group message sent to $endpointId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to send group message to $endpointId", e)
                    }
            }
        }
    }

    /**
     * Send a group invite to a connected peer.
     */
    fun sendGroupInvite(endpointId: String, invite: GroupInvitePayload) {
        val bytes = payloadHandler.serializeGroupInvite(invite)
        val payload = Payload.fromBytes(bytes)

        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Group invite sent to $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send invite to $endpointId", e)
                emitEvent(NearbyEvent.Error("Failed to send invite: ${e.message}", e as? Exception))
            }
    }

    /**
     * Send a join approval to a peer who requested to join a group.
     */
    fun sendJoinApproval(endpointId: String, approval: JoinApprovalPayload) {
        val bytes = payloadHandler.serializeJoinApproval(approval)
        val payload = Payload.fromBytes(bytes)

        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Join approval sent to $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send approval to $endpointId", e)
            }
    }

    /**
     * Disconnect from a specific peer.
     */
    fun disconnectFromEndpoint(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        val current = _connectedEndpoints.value.toMutableMap()
        current.remove(endpointId)
        _connectedEndpoints.value = current
        emitEvent(NearbyEvent.ConnectionChanged(endpointId, ConnectionStatus.DISCONNECTED))
    }

    /**
     * Stop everything and disconnect from all peers.
     */
    fun stopAll() {
        connectionsClient.stopAllEndpoints()
        stopScanning()
        _connectedEndpoints.value = emptyMap()
    }

    // ──────────────────────────────────────────────
    // Callbacks
    // ──────────────────────────────────────────────

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId (${info.endpointName})")
            val peer = Peer(
                endpointId = endpointId,
                name = info.endpointName,
                connectionStatus = ConnectionStatus.DISCOVERED,
                lastSeen = System.currentTimeMillis()
            )
            // Track in SQLite mesh database
            coroutineScope.launch {
                meshDb.upsertMeshPeer(endpointId, info.endpointName)
            }
            emitEvent(NearbyEvent.PeerDiscovered(peer))
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            emitEvent(NearbyEvent.PeerLost(endpointId))
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with $endpointId (${connectionInfo.endpointName})")
            pendingConnections[endpointId] = connectionInfo.endpointName

            if (autoAcceptConnections) {
                acceptConnection(endpointId)
            } else {
                emitEvent(NearbyEvent.ConnectionRequested(endpointId, connectionInfo.endpointName))
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val peerName = pendingConnections.remove(endpointId) ?: "Unknown"

            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connected to $endpointId ($peerName)")
                    val peer = Peer(
                        endpointId = endpointId,
                        name = peerName,
                        connectionStatus = ConnectionStatus.CONNECTED,
                        lastSeen = System.currentTimeMillis()
                    )
                    val current = _connectedEndpoints.value.toMutableMap()
                    current[endpointId] = peer
                    _connectedEndpoints.value = current

                    // Track connection in SQLite mesh database + boost trust & flush pending offline queue
                    coroutineScope.launch {
                        meshDb.upsertMeshPeer(endpointId, peerName)
                        meshDb.updateTrustScore(endpointId, 0.1)

                        val pending = meshDb.getPendingMessages(endpointId)
                        pending.forEach { queued ->
                            val p = Payload.fromBytes(queued.payload)
                            connectionsClient.sendPayload(endpointId, p)
                        }
                        if (pending.isNotEmpty()) {
                            meshDb.cleanupExpired()
                        }
                    }

                    emitEvent(NearbyEvent.ConnectionChanged(endpointId, ConnectionStatus.CONNECTED))
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected by $endpointId")
                    emitEvent(NearbyEvent.ConnectionChanged(endpointId, ConnectionStatus.DISCONNECTED))
                }
                else -> {
                    Log.e(TAG, "Connection failed with $endpointId: ${result.status}")
                    emitEvent(NearbyEvent.Error("Connection failed: ${result.status.statusMessage}"))
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            val current = _connectedEndpoints.value.toMutableMap()
            current.remove(endpointId)
            _connectedEndpoints.value = current
            emitEvent(NearbyEvent.ConnectionChanged(endpointId, ConnectionStatus.DISCONNECTED))
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (!rateLimiter.allowPayload(endpointId)) {
                Log.w(TAG, "Dropping rate-limited payload flood from $endpointId")
                return
            }

            val bytes = payload.asBytes() ?: return
            val wrapper = payloadHandler.deserialize(bytes) ?: return

            when (wrapper.type) {
                PayloadType.CHAT_MESSAGE -> {
                    val message = payloadHandler.extractMessage(wrapper)
                    if (message != null) {
                        Log.d(TAG, "Message received from $endpointId: ${message.text}")
                        emitEvent(NearbyEvent.MessageReceived(message, endpointId))
                    }
                }
                PayloadType.GROUP_MESSAGE -> {
                    val groupMsg = payloadHandler.extractGroupMessage(wrapper)
                    if (groupMsg != null) {
                        Log.d(TAG, "Group message received from $endpointId in group ${groupMsg.groupId}")
                        emitEvent(NearbyEvent.GroupMessageReceived(groupMsg.groupId, groupMsg.message, endpointId))
                    }
                }
                PayloadType.GROUP_INVITE -> {
                    val invite = payloadHandler.extractGroupInvite(wrapper)
                    if (invite != null) {
                        Log.d(TAG, "Group invite received from $endpointId: ${invite.groupName}")
                        emitEvent(NearbyEvent.GroupInviteReceived(invite, endpointId))
                    }
                }
                PayloadType.JOIN_REQUEST -> {
                    val request = payloadHandler.extractJoinRequest(wrapper)
                    if (request != null) {
                        Log.d(TAG, "Join request from ${request.requesterName}")
                        emitEvent(NearbyEvent.JoinRequestReceived(request, endpointId))
                    }
                }
                PayloadType.JOIN_APPROVED -> {
                    val approval = payloadHandler.extractJoinApproval(wrapper)
                    if (approval != null) {
                        Log.d(TAG, "Join approved for group ${approval.groupName}")
                        emitEvent(NearbyEvent.JoinApproved(approval))
                    }
                }
                PayloadType.FILE_METADATA -> {
                    val metadata = payloadHandler.extractFileMetadata(wrapper)
                    if (metadata != null) {
                        Log.d(TAG, "File metadata received: ${metadata.fileName} (${metadata.fileSize} bytes)")
                        emitEvent(NearbyEvent.FileMetadataReceived(metadata, endpointId))
                    }
                }
                else -> {
                    Log.d(TAG, "Received payload type: ${wrapper.type}")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // For byte payloads, transfer is instant.
        }
    }

    private fun emitEvent(event: NearbyEvent) {
        _events.tryEmit(event)
    }
}

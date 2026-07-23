package com.p2pchat.crypto

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rate Limiter and Anti-Spam Guard for P2P Mesh Payloads.
 * Protects memory, battery, and CPU against flood attacks over Nearby Connections.
 */
@Singleton
class P2PRateLimiter @Inject constructor() {

    companion object {
        private const val MAX_MESSAGES_PER_SECOND = 15
        private const val WINDOW_MS = 1000L
    }

    private val peerTimestamps = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Check if a payload from endpointId should be allowed or dropped due to rate limiting.
     * Returns true if allowed, false if rate limited (flooding).
     */
    fun allowPayload(endpointId: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = peerTimestamps.computeIfAbsent(endpointId) { mutableListOf() }

        synchronized(timestamps) {
            // Remove timestamps outside the 1-second window
            timestamps.removeAll { now - it > WINDOW_MS }

            if (timestamps.size >= MAX_MESSAGES_PER_SECOND) {
                return false // Rate limit exceeded
            }

            timestamps.add(now)
            return true
        }
    }

    fun resetPeer(endpointId: String) {
        peerTimestamps.remove(endpointId)
    }
}

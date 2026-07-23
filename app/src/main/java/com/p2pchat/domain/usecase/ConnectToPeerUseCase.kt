package com.p2pchat.domain.usecase

import com.p2pchat.nearby.NearbyConnectionManager
import javax.inject.Inject

/**
 * Use case for initiating a connection to a discovered peer.
 */
class ConnectToPeerUseCase @Inject constructor(
    private val nearbyConnectionManager: NearbyConnectionManager
) {
    suspend operator fun invoke(endpointId: String): Result<Unit> {
        return try {
            nearbyConnectionManager.connectToEndpoint(endpointId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

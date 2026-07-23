package com.p2pchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.p2pchat.data.model.PeerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for peer devices.
 */
@Dao
interface PeerDao {

    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerEntity)

    @Query("UPDATE peers SET connectionStatus = :status WHERE endpointId = :endpointId")
    suspend fun updateStatus(endpointId: String, status: String)

    @Query("DELETE FROM peers WHERE endpointId = :endpointId")
    suspend fun deletePeer(endpointId: String)

    @Query("SELECT * FROM peers WHERE endpointId = :endpointId")
    suspend fun getPeer(endpointId: String): PeerEntity?
}

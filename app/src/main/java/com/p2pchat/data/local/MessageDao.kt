package com.p2pchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.p2pchat.data.model.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for chat messages.
 */
@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE peerEndpointId = :peerEndpointId AND groupId IS NULL ORDER BY timestamp ASC")
    fun getMessagesForPeer(peerEndpointId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroup(groupId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE id IN (
            SELECT id FROM messages 
            WHERE groupId IS NULL
            GROUP BY peerEndpointId 
            HAVING timestamp = MAX(timestamp)
        ) 
        ORDER BY timestamp DESC
    """)
    fun getLatestMessagePerPeer(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDelivered = 1 WHERE id = :messageId")
    suspend fun markAsDelivered(messageId: String)

    @Query("DELETE FROM messages WHERE peerEndpointId = :peerEndpointId AND groupId IS NULL")
    suspend fun deleteMessagesForPeer(peerEndpointId: String)

    @Query("DELETE FROM messages WHERE groupId = :groupId")
    suspend fun deleteMessagesForGroup(groupId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE peerEndpointId = :peerEndpointId AND isFromMe = 0 AND isDelivered = 0")
    fun getUnreadCount(peerEndpointId: String): Flow<Int>
}

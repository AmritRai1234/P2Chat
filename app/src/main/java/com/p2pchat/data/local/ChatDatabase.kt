package com.p2pchat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.p2pchat.data.model.GroupEntity
import com.p2pchat.data.model.GroupMemberEntity
import com.p2pchat.data.model.MeshPeerEntity
import com.p2pchat.data.model.MessageEntity
import com.p2pchat.data.model.MessageQueueEntity
import com.p2pchat.data.model.NetworkStatsEntity
import com.p2pchat.data.model.PeerEntity

/**
 * Room database for P2Chat.
 * All data stored locally in SQLite — fully offline, no server needed.
 */
@Database(
    entities = [
        MessageEntity::class,
        PeerEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        MeshPeerEntity::class,
        MessageQueueEntity::class,
        NetworkStatsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun peerDao(): PeerDao
    abstract fun groupDao(): GroupDao
    abstract fun meshDao(): MeshDao
}

package com.p2pchat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.p2pchat.data.model.GroupEntity
import com.p2pchat.data.model.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for groups and group members.
 */
@Dao
interface GroupDao {

    @Query("SELECT * FROM `groups` ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    fun getGroup(groupId: String): Flow<GroupEntity?>

    @Query("SELECT * FROM `groups` WHERE inviteCode = :inviteCode LIMIT 1")
    fun getGroupByInviteCode(inviteCode: String): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("DELETE FROM `groups` WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    // Members
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    suspend fun getMembers(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    fun getMembersFlow(groupId: String): Flow<List<GroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND endpointId = :endpointId")
    suspend fun removeMember(groupId: String, endpointId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun removeAllMembers(groupId: String)
}

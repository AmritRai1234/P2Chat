package com.p2pchat.domain.repository

import com.p2pchat.domain.model.ChatGroup
import com.p2pchat.domain.model.GroupMember
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for group chat operations.
 */
interface GroupRepository {
    fun getAllGroups(): Flow<List<ChatGroup>>
    fun getGroup(groupId: String): Flow<ChatGroup?>
    fun getGroupByInviteCode(inviteCode: String): Flow<ChatGroup?>
    suspend fun createGroup(group: ChatGroup)
    suspend fun addMember(groupId: String, member: GroupMember)
    suspend fun removeMember(groupId: String, endpointId: String)
    suspend fun deleteGroup(groupId: String)
    suspend fun getGroupMembers(groupId: String): List<GroupMember>
}

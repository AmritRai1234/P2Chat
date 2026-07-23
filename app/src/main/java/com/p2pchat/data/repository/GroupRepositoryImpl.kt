package com.p2pchat.data.repository

import com.p2pchat.data.local.GroupDao
import com.p2pchat.data.model.GroupEntity
import com.p2pchat.data.model.GroupMemberEntity
import com.p2pchat.domain.model.ChatGroup
import com.p2pchat.domain.model.GroupMember
import com.p2pchat.domain.model.GroupRole
import com.p2pchat.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of GroupRepository backed by Room database.
 */
@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao
) : GroupRepository {

    override fun getAllGroups(): Flow<List<ChatGroup>> {
        return groupDao.getAllGroups().map { entities ->
            entities.map { entity ->
                val members = groupDao.getMembers(entity.id)
                entity.toDomain(members)
            }
        }
    }

    override fun getGroup(groupId: String): Flow<ChatGroup?> {
        return combine(
            groupDao.getGroup(groupId),
            groupDao.getMembersFlow(groupId)
        ) { group, members ->
            group?.toDomain(members)
        }
    }

    override fun getGroupByInviteCode(inviteCode: String): Flow<ChatGroup?> {
        return groupDao.getGroupByInviteCode(inviteCode).map { entity ->
            entity?.let {
                val members = groupDao.getMembers(it.id)
                it.toDomain(members)
            }
        }
    }

    override suspend fun createGroup(group: ChatGroup) {
        groupDao.insertGroup(group.toEntity())
        // Add creator as admin member
        val adminMember = GroupMemberEntity(
            groupId = group.id,
            endpointId = group.creatorEndpointId,
            name = group.creatorName,
            role = GroupRole.ADMIN.name,
            joinedAt = group.createdAt
        )
        groupDao.insertMember(adminMember)
    }

    override suspend fun addMember(groupId: String, member: GroupMember) {
        groupDao.insertMember(member.toEntity(groupId))
    }

    override suspend fun removeMember(groupId: String, endpointId: String) {
        groupDao.removeMember(groupId, endpointId)
    }

    override suspend fun deleteGroup(groupId: String) {
        groupDao.removeAllMembers(groupId)
        groupDao.deleteGroup(groupId)
    }

    override suspend fun getGroupMembers(groupId: String): List<GroupMember> {
        return groupDao.getMembers(groupId).map { it.toDomain() }
    }

    // Mappers
    private fun GroupEntity.toDomain(members: List<GroupMemberEntity> = emptyList()) = ChatGroup(
        id = id,
        name = name,
        inviteCode = inviteCode,
        creatorEndpointId = creatorEndpointId,
        creatorName = creatorName,
        members = members.map { it.toDomain() },
        createdAt = createdAt
    )

    private fun ChatGroup.toEntity() = GroupEntity(
        id = id,
        name = name,
        inviteCode = inviteCode,
        creatorEndpointId = creatorEndpointId,
        creatorName = creatorName,
        createdAt = createdAt
    )

    private fun GroupMemberEntity.toDomain() = GroupMember(
        endpointId = endpointId,
        name = name,
        role = try { GroupRole.valueOf(role) } catch (_: Exception) { GroupRole.MEMBER },
        joinedAt = joinedAt
    )

    private fun GroupMember.toEntity(groupId: String) = GroupMemberEntity(
        groupId = groupId,
        endpointId = endpointId,
        name = name,
        role = role.name,
        joinedAt = joinedAt
    )
}

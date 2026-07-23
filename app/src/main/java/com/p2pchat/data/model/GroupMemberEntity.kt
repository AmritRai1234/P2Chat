package com.p2pchat.data.model

import androidx.room.Entity

/**
 * Room entity for group membership (many-to-many between groups and peers).
 */
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "endpointId"]
)
data class GroupMemberEntity(
    val groupId: String,
    val endpointId: String,
    val name: String,
    val role: String,
    val joinedAt: Long
)

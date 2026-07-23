package com.p2pchat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting chat groups.
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val inviteCode: String,
    val creatorEndpointId: String,
    val creatorName: String,
    val createdAt: Long
)

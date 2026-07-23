package com.p2pchat.domain.model

import java.util.UUID

/**
 * Domain model representing a chat group.
 * Groups are invite-only — only peers with a valid invite code can join.
 */
data class ChatGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val inviteCode: String = generateInviteCode(),
    val creatorEndpointId: String,
    val creatorName: String,
    val members: List<GroupMember> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Generate a 6-character alphanumeric invite code.
         * Short enough to share verbally in a P2P context.
         */
        fun generateInviteCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Non-ambiguous cryptographic charset
            val random = java.security.SecureRandom()
            val chunk1 = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
            val chunk2 = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
            val chunk3 = (1..4).map { chars[random.nextInt(chars.length)] }.joinToString("")
            return "$chunk1-$chunk2-$chunk3"
        }
    }
}

/**
 * A member within a chat group.
 */
data class GroupMember(
    val endpointId: String,
    val name: String,
    val role: GroupRole = GroupRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis()
)

enum class GroupRole {
    ADMIN,   // Creator — can invite and remove members
    MEMBER   // Regular member — can chat but not manage
}

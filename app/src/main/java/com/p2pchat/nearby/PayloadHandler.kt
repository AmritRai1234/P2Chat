package com.p2pchat.nearby

import com.google.gson.Gson
import com.p2pchat.domain.model.ChatMessage
import com.p2pchat.domain.model.ChatGroup
import com.p2pchat.domain.model.GroupMember
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles serialization/deserialization of chat messages and group data
 * for transmission over Nearby Connections payloads.
 */
@Singleton
class PayloadHandler @Inject constructor() {

    private val gson = Gson()

    fun serializeMessage(message: ChatMessage): ByteArray {
        val wrapper = PayloadWrapper(
            type = PayloadType.CHAT_MESSAGE,
            data = gson.toJson(message)
        )
        return gson.toJson(wrapper).toByteArray(Charsets.UTF_8)
    }

    fun serializeGroupMessage(message: ChatMessage, groupId: String): ByteArray {
        val payload = GroupMessagePayload(groupId = groupId, message = message)
        val wrapper = PayloadWrapper(
            type = PayloadType.GROUP_MESSAGE,
            data = gson.toJson(payload)
        )
        return gson.toJson(wrapper).toByteArray(Charsets.UTF_8)
    }

    fun serializeGroupInvite(invite: GroupInvitePayload): ByteArray {
        val wrapper = PayloadWrapper(
            type = PayloadType.GROUP_INVITE,
            data = gson.toJson(invite)
        )
        return gson.toJson(wrapper).toByteArray(Charsets.UTF_8)
    }

    fun serializeJoinRequest(request: JoinRequestPayload): ByteArray {
        val wrapper = PayloadWrapper(
            type = PayloadType.JOIN_REQUEST,
            data = gson.toJson(request)
        )
        return gson.toJson(wrapper).toByteArray(Charsets.UTF_8)
    }

    fun serializeJoinApproval(approval: JoinApprovalPayload): ByteArray {
        val wrapper = PayloadWrapper(
            type = PayloadType.JOIN_APPROVED,
            data = gson.toJson(approval)
        )
        return gson.toJson(wrapper).toByteArray(Charsets.UTF_8)
    }

    fun deserialize(bytes: ByteArray): PayloadWrapper? {
        return try {
            val json = String(bytes, Charsets.UTF_8)
            gson.fromJson(json, PayloadWrapper::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun extractMessage(wrapper: PayloadWrapper): ChatMessage? {
        if (wrapper.type != PayloadType.CHAT_MESSAGE) return null
        return try {
            gson.fromJson(wrapper.data, ChatMessage::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun extractGroupMessage(wrapper: PayloadWrapper): GroupMessagePayload? {
        if (wrapper.type != PayloadType.GROUP_MESSAGE) return null
        return try {
            gson.fromJson(wrapper.data, GroupMessagePayload::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun extractGroupInvite(wrapper: PayloadWrapper): GroupInvitePayload? {
        if (wrapper.type != PayloadType.GROUP_INVITE) return null
        return try {
            gson.fromJson(wrapper.data, GroupInvitePayload::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun extractJoinRequest(wrapper: PayloadWrapper): JoinRequestPayload? {
        if (wrapper.type != PayloadType.JOIN_REQUEST) return null
        return try {
            gson.fromJson(wrapper.data, JoinRequestPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun extractJoinApproval(wrapper: PayloadWrapper): JoinApprovalPayload? {
        if (wrapper.type != PayloadType.JOIN_APPROVED) return null
        return try {
            gson.fromJson(wrapper.data, JoinApprovalPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun serializeFileMetadata(metadata: FileMetadataPayload): ByteArray {
        val wrapper = PayloadWrapper(
            type = PayloadType.FILE_METADATA,
            data = gson.toJson(metadata)
        )
        return gson.toJson(wrapper).toByteArray(Charsets.UTF_8)
    }

    fun extractFileMetadata(wrapper: PayloadWrapper): FileMetadataPayload? {
        if (wrapper.type != PayloadType.FILE_METADATA) return null
        return try {
            gson.fromJson(wrapper.data, FileMetadataPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

data class PayloadWrapper(
    val type: PayloadType,
    val data: String
)

enum class PayloadType {
    CHAT_MESSAGE,
    GROUP_MESSAGE,
    GROUP_INVITE,
    JOIN_REQUEST,
    JOIN_APPROVED,
    JOIN_DENIED,
    FILE_METADATA,
    TYPING_INDICATOR,
    READ_RECEIPT
}

/** A message sent to a group */
data class GroupMessagePayload(
    val groupId: String,
    val message: ChatMessage
)

/** An invite sent from group admin to a peer */
data class GroupInvitePayload(
    val groupId: String,
    val groupName: String,
    val inviteCode: String,
    val inviterName: String
)

/** A request from a peer to join a group using an invite code */
data class JoinRequestPayload(
    val inviteCode: String,
    val requesterName: String,
    val requesterEndpointId: String
)

/** Approval to join a group — contains full group info */
data class JoinApprovalPayload(
    val groupId: String,
    val groupName: String,
    val inviteCode: String,
    val members: List<String> // member names
)

package com.p2pchat.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p2pchat.domain.model.ChatGroup
import com.p2pchat.domain.model.GroupMember
import com.p2pchat.domain.model.GroupRole
import com.p2pchat.domain.model.Peer
import com.p2pchat.domain.repository.ChatRepository
import com.p2pchat.domain.repository.GroupRepository
import com.p2pchat.nearby.GroupInvitePayload
import com.p2pchat.nearby.JoinApprovalPayload
import com.p2pchat.nearby.NearbyConnectionManager
import com.p2pchat.nearby.NearbyEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val groups: List<ChatGroup> = emptyList(),
    val connectedPeers: List<Peer> = emptyList(),
    val pendingInvites: List<GroupInvitePayload> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showInviteDialog: Boolean = false,
    val selectedGroupForInvite: ChatGroup? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository,
    private val nearbyConnectionManager: NearbyConnectionManager
) : ViewModel() {

    private val _pendingInvites = MutableStateFlow<List<GroupInvitePayload>>(emptyList())
    private val _showCreateDialog = MutableStateFlow(false)
    private val _showInviteDialog = MutableStateFlow(false)
    private val _selectedGroup = MutableStateFlow<ChatGroup?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> = combine(
        groupRepository.getAllGroups(),
        nearbyConnectionManager.connectedEndpoints,
        _pendingInvites,
        _showCreateDialog,
        _showInviteDialog,
        _selectedGroup,
        _errorMessage
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val groups = flows[0] as List<ChatGroup>
        @Suppress("UNCHECKED_CAST")
        val connected = flows[1] as Map<String, Peer>
        @Suppress("UNCHECKED_CAST")
        val invites = flows[2] as List<GroupInvitePayload>
        val showCreate = flows[3] as Boolean
        val showInvite = flows[4] as Boolean
        val selectedGroup = flows[5] as ChatGroup?
        val error = flows[6] as String?

        GroupsUiState(
            groups = groups,
            connectedPeers = connected.values.toList(),
            pendingInvites = invites,
            showCreateDialog = showCreate,
            showInviteDialog = showInvite,
            selectedGroupForInvite = selectedGroup,
            errorMessage = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GroupsUiState()
    )

    init {
        observeNearbyEvents()
    }

    private fun observeNearbyEvents() {
        viewModelScope.launch {
            nearbyConnectionManager.events.collect { event ->
                when (event) {
                    is NearbyEvent.GroupInviteReceived -> {
                        _pendingInvites.value = _pendingInvites.value + event.invite
                    }
                    is NearbyEvent.JoinApproved -> {
                        // Create the group locally when approved
                        val group = ChatGroup(
                            id = event.approval.groupId,
                            name = event.approval.groupName,
                            inviteCode = event.approval.inviteCode,
                            creatorEndpointId = "remote",
                            creatorName = "Admin"
                        )
                        viewModelScope.launch {
                            groupRepository.createGroup(group)
                        }
                    }
                    is NearbyEvent.JoinRequestReceived -> {
                        // Auto-approve if we have the group
                        viewModelScope.launch {
                            val groups = groupRepository.getAllGroups()
                            // Handle join request validation
                        }
                    }
                    else -> { /* handled elsewhere */ }
                }
            }
        }
    }

    fun createGroup(name: String, displayName: String) {
        viewModelScope.launch {
            val group = ChatGroup(
                name = name,
                creatorEndpointId = "me",
                creatorName = displayName
            )
            groupRepository.createGroup(group)
            _showCreateDialog.value = false
        }
    }

    fun acceptInvite(invite: GroupInvitePayload) {
        viewModelScope.launch {
            val group = ChatGroup(
                id = invite.groupId,
                name = invite.groupName,
                inviteCode = invite.inviteCode,
                creatorEndpointId = "remote",
                creatorName = invite.inviterName
            )
            groupRepository.createGroup(group)
            _pendingInvites.value = _pendingInvites.value.filter { it.groupId != invite.groupId }
        }
    }

    fun declineInvite(invite: GroupInvitePayload) {
        _pendingInvites.value = _pendingInvites.value.filter { it.groupId != invite.groupId }
    }

    fun sendInviteToPeer(group: ChatGroup, peerEndpointId: String) {
        val invite = GroupInvitePayload(
            groupId = group.id,
            groupName = group.name,
            inviteCode = group.inviteCode,
            inviterName = group.creatorName
        )
        nearbyConnectionManager.sendGroupInvite(peerEndpointId, invite)
    }

    fun showCreateDialog() { _showCreateDialog.value = true }
    fun hideCreateDialog() { _showCreateDialog.value = false }
    fun showInviteDialog(group: ChatGroup) {
        _selectedGroup.value = group
        _showInviteDialog.value = true
    }
    fun hideInviteDialog() {
        _showInviteDialog.value = false
        _selectedGroup.value = null
    }
    fun clearError() { _errorMessage.value = null }
}

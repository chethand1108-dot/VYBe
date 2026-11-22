package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UiState
import com.example.data.model.VybeConversation
import com.example.data.model.VybeMessage
import com.example.data.model.VybeProfile
import com.example.ui.components.VybeAvatar
import com.example.ui.components.VybeEmptyState
import com.example.ui.components.formatRelativeTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
    currentUserId: String,
    isExpandedWidth: Boolean,
    conversationsState: UiState<List<VybeConversation>>,
    profiles: List<VybeProfile>,
    selectedConversationId: String?,
    messagesState: UiState<List<VybeMessage>>,
    onSelectConversation: (String?) -> Unit,
    onStartChatWith: (VybeProfile) -> Unit,
    onSendMessage: (VybeConversation, String, () -> Unit) -> Unit
) {
    var showNewChatSheet by remember { mutableStateOf(false) }

    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val activeConversation = conversations.firstOrNull { it.id == selectedConversationId }

    if (activeConversation != null && !isExpandedWidth) {
        BackHandler { onSelectConversation(null) }
        ChatThreadPane(
            currentUserId = currentUserId,
            conversation = activeConversation,
            messagesState = messagesState,
            showBackButton = true,
            onBack = { onSelectConversation(null) },
            onSendMessage = { text, onSent -> onSendMessage(activeConversation, text, onSent) }
        )
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(if (isExpandedWidth) 0.42f else 1f)
                    .fillMaxSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Direct Messages",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .clickable { showNewChatSheet = true }
                            .testTag("new_chat_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddComment,
                                contentDescription = "New message",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "New Chat",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                when (conversationsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        VybeEmptyState(
                            title = "Could not load chats",
                            subtitle = conversationsState.message
                        )
                    }
                    is UiState.Success -> {
                        if (conversations.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                VybeEmptyState(
                                    title = "No messages yet",
                                    subtitle = "Start a private chat with anyone in the VYBE community.",
                                    actionLabel = "Start a Chat",
                                    onAction = { showNewChatSheet = true }
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(conversations, key = { it.id }) { conv ->
                                    val peerName = conv.peerName(currentUserId)
                                    val peerHandle = conv.peerHandle(currentUserId)
                                    val peerProfile = profiles.firstOrNull { it.userId == conv.peerId(currentUserId) }
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (conv.id == selectedConversationId)
                                                MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable { onSelectConversation(conv.id) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(14.dp)
                                        ) {
                                            VybeAvatar(
                                                displayName = peerName,
                                                avatarColor = peerProfile?.avatarColor ?: "violet",
                                                photoUrl = peerProfile?.photoUrl,
                                                photoDataUrl = peerProfile?.photoDataUrl,
                                                size = 48.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = peerName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = formatRelativeTimestamp(conv.lastMessageAt ?: conv.updatedAt),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = conv.lastMessageText.ifBlank { "@$peerHandle • Say hello" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isExpandedWidth) {
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxSize()
                ) {
                    if (activeConversation != null) {
                        ChatThreadPane(
                            currentUserId = currentUserId,
                            conversation = activeConversation,
                            messagesState = messagesState,
                            showBackButton = false,
                            onBack = {},
                            onSendMessage = { text, onSent -> onSendMessage(activeConversation, text, onSent) }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            VybeEmptyState(
                                title = "Select a conversation",
                                subtitle = "Pick a chat on the left or start a new private message."
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        val candidates = remember(profiles, currentUserId) {
            profiles.filter { it.userId != currentUserId }
        }
        ModalBottomSheet(onDismissRequest = { showNewChatSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Message a Creator", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(12.dp))
                if (candidates.isEmpty()) {
                    Text(
                        text = "No other discoverable members yet. Once another user signs into VYBE, you can message them here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.userId }) { peer ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        showNewChatSheet = false
                                        onStartChatWith(peer)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    VybeAvatar(
                                        displayName = peer.displayName,
                                        avatarColor = peer.avatarColor,
                                        photoUrl = peer.photoUrl,
                                        photoDataUrl = peer.photoDataUrl,
                                        size = 42.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(peer.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "@${peer.handle} • ${peer.moodTag}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadPane(
    currentUserId: String,
    conversation: VybeConversation,
    messagesState: UiState<List<VybeMessage>>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String, () -> Unit) -> Unit
) {
    var draft by remember(conversation.id) { mutableStateOf("") }
    val listState = rememberLazyListState()

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to conversations")
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.peerName(currentUserId),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "@${conversation.peerHandle(currentUserId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f)) {
            when (messagesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    VybeEmptyState(
                        title = "Message stream error",
                        subtitle = messagesState.message
                    )
                }
                is UiState.Success -> {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Send the first message to ${conversation.peerName(currentUserId)}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                val isMine = msg.senderId == currentUserId
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = if (isMine) 18.dp else 4.dp,
                                            bottomEnd = if (isMine) 4.dp else 18.dp
                                        ),
                                        color = if (isMine) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.widthIn(max = 290.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = formatRelativeTimestamp(msg.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                placeholder = { Text("Write a message...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSendMessage(draft) { draft = "" }
                    }
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

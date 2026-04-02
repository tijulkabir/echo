package com.echo.android.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.android.R
import com.echo.android.model.BitchatMessage
import java.text.SimpleDateFormat
import java.util.*

/**
 * WhatsApp-style Direct Message conversation list screen.
 * Shows all 1:1 conversations with last message preview, timestamp, and unread badges.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessageListScreen(
    viewModel: ChatViewModel,
    onBackToMesh: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val privateChats by viewModel.privateChats.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val peerNicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val unreadPrivateMessages by viewModel.unreadPrivateMessages.collectAsStateWithLifecycle()
    val favoritePeers by viewModel.favoritePeers.collectAsStateWithLifecycle()
    val peerFingerprints by viewModel.peerFingerprints.collectAsStateWithLifecycle()

    // Show "new chat" peer picker
    var showNewChatPicker by remember { mutableStateOf(false) }

    // Build conversation list from privateChats
    val conversations = remember(privateChats, unreadPrivateMessages, connectedPeers, peerNicknames) {
        privateChats.entries
            .filter { it.value.isNotEmpty() }
            .map { (peerID, messages) ->
                val lastMessage = messages.lastOrNull()
                val displayName = peerNicknames[peerID] ?: peerID.take(12)
                val isOnline = connectedPeers.contains(peerID)
                val hasUnread = unreadPrivateMessages.contains(peerID)
                val unreadCount = if (hasUnread) {
                    messages.count { msg -> msg.sender != nickname }
                } else 0
                ConversationItem(
                    peerID = peerID,
                    displayName = displayName,
                    lastMessage = lastMessage,
                    isOnline = isOnline,
                    hasUnread = hasUnread,
                    unreadCount = unreadCount
                )
            }
            .sortedWith(
                compareBy<ConversationItem> { !it.hasUnread }
                    .thenByDescending { it.lastMessage?.timestamp?.time ?: 0L }
            )
    }

    // Build list of connected peers available for new chats (not already in conversations)
    val availablePeers = remember(connectedPeers, privateChats, peerNicknames, nickname) {
        connectedPeers
            .filter { it != viewModel.meshService.myPeerID }
            .map { peerID ->
                val displayName = peerNicknames[peerID] ?: peerID.take(12)
                peerID to displayName
            }
            .sortedBy { it.second.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Direct Messages",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMesh) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Mesh"
                        )
                    }
                },
                actions = {
                    // New chat button
                    IconButton(onClick = { showNewChatPicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "New Chat",
                            tint = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (conversations.isEmpty() && !showNewChatPicker) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Tap the ✏️ icon to start a chat\nwith a connected peer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showNewChatPicker = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start New Chat")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = conversations,
                    key = { it.peerID }
                ) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        colorScheme = colorScheme,
                        onClick = { onOpenChat(conversation.peerID) }
                    )
                }

                // "Start new chat" row at bottom
                item {
                    Surface(
                        onClick = { showNewChatPicker = true },
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start New Chat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // New chat peer picker dialog
    if (showNewChatPicker) {
        NewChatPickerDialog(
            availablePeers = availablePeers,
            colorScheme = colorScheme,
            onPeerSelected = { peerID ->
                showNewChatPicker = false
                onOpenChat(peerID)
            },
            onDismiss = { showNewChatPicker = false }
        )
    }
}

/**
 * Data class for a conversation list item
 */
private data class ConversationItem(
    val peerID: String,
    val displayName: String,
    val lastMessage: BitchatMessage?,
    val isOnline: Boolean,
    val hasUnread: Boolean,
    val unreadCount: Int
)

/**
 * A single conversation row in the list — WhatsApp style
 */
@Composable
private fun ConversationRow(
    conversation: ConversationItem,
    colorScheme: ColorScheme,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (conversation.hasUnread) {
            colorScheme.primaryContainer.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle with initial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = colorScheme.primary
                )
                // Online indicator dot
                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(colorScheme.background)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C851))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name + last message
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (conversation.hasUnread) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Timestamp
                    conversation.lastMessage?.let { msg ->
                        Text(
                            text = formatTimestamp(msg.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (conversation.hasUnread) {
                                Color(0xFF00C851)
                            } else {
                                colorScheme.onSurface.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message preview
                    Text(
                        text = conversation.lastMessage?.content ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread badge
                    if (conversation.hasUnread && conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF00C851),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                .defaultMinSize(minWidth = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Divider
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp),
        color = colorScheme.outline.copy(alpha = 0.15f)
    )
}

/**
 * Dialog to pick a connected peer for starting a new chat
 */
@Composable
private fun NewChatPickerDialog(
    availablePeers: List<Pair<String, String>>,
    colorScheme: ColorScheme,
    onPeerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Start New Chat",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        },
        text = {
            if (availablePeers.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No peers connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Get closer to other Echo users via Bluetooth",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn {
                    items(availablePeers) { (peerID, displayName) ->
                        Surface(
                            onClick = { onPeerSelected(peerID) },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Outlined.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF007AFF)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Format a Date into a human-readable relative timestamp
 */
private fun formatTimestamp(date: Date): String {
    val now = System.currentTimeMillis()
    val diff = now - date.time
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

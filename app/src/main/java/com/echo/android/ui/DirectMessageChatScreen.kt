package com.echo.android.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.android.R
import com.echo.android.nostr.GeohashAliasRegistry
import com.echo.android.ui.media.FullScreenImageViewer

/**
 * Full-screen Direct Message chat screen — replaces the PrivateChatSheet bottom sheet
 * with a proper WhatsApp-style full-screen chat experience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessageChatScreen(
    viewModel: ChatViewModel,
    peerID: String,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val privateChats by viewModel.privateChats.collectAsStateWithLifecycle()
    val peerNicknames by viewModel.peerNicknames.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    val peerDirectMap by viewModel.peerDirect.collectAsStateWithLifecycle()
    val peerSessionStates by viewModel.peerSessionStates.collectAsStateWithLifecycle()
    val favoritePeers by viewModel.favoritePeers.collectAsStateWithLifecycle()
    val peerFingerprints by viewModel.peerFingerprints.collectAsStateWithLifecycle()
    val verifiedFingerprints by viewModel.verifiedFingerprints.collectAsStateWithLifecycle()

    // Start private chat when screen opens
    LaunchedEffect(peerID) {
        viewModel.startPrivateChat(peerID)
    }

    val isNostrPeer = peerID.startsWith("nostr_") || peerID.startsWith("nostr:")
    val messages = privateChats[peerID] ?: emptyList()
    val isDirect = peerDirectMap[peerID] == true
    val isConnected = connectedPeers.contains(peerID) || isDirect
    val sessionState = peerSessionStates[peerID]
    val fingerprint = peerFingerprints[peerID]
    val isFavorite = remember(favoritePeers, fingerprint) {
        if (fingerprint != null) favoritePeers.contains(fingerprint) else viewModel.isFavorite(peerID)
    }
    val isVerified = remember(peerID, verifiedFingerprints) {
        viewModel.isPeerVerified(peerID, verifiedFingerprints)
    }

    // Compute display name
    val displayName = remember(peerID, peerNicknames) {
        if (isNostrPeer) {
            val gh = GeohashConversationRegistry.get(peerID) ?: "geohash"
            val fullPubkey = GeohashAliasRegistry.get(peerID) ?: ""
            if (fullPubkey.isNotEmpty()) {
                viewModel.geohashViewModel.displayNameForGeohashConversation(fullPubkey, gh)
            } else {
                peerNicknames[peerID] ?: "unknown"
            }
        } else {
            peerNicknames[peerID] ?: peerID.take(12)
        }
    }

    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var forceScrollToBottom by remember { mutableStateOf(false) }
    var isScrolledUp by remember { mutableStateOf(false) }
    var showFullScreenImageViewer by remember { mutableStateOf(false) }
    var viewerImagePaths by remember { mutableStateOf(emptyList<String>()) }
    var initialViewerIndex by remember { mutableStateOf(0) }

    val securityModifier = if (!isNostrPeer) {
        Modifier.clickable { viewModel.showSecurityVerificationSheet() }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        val headerHeight = 56.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Header spacer
            Spacer(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(headerHeight)
            )

            // Messages list
            MessagesList(
                messages = messages,
                currentUserNickname = nickname,
                meshService = viewModel.meshService,
                modifier = Modifier.weight(1f),
                forceScrollToBottom = forceScrollToBottom,
                onScrolledUpChanged = { isUp -> isScrolledUp = isUp },
                onNicknameClick = { fullSenderName ->
                    val (baseName, _) = splitSuffix(fullSenderName)
                    val mentionText = "@$baseName"
                    val currentText = messageText.text
                    val newText = when {
                        currentText.isEmpty() -> "$mentionText "
                        currentText.endsWith(" ") -> "$currentText$mentionText "
                        else -> "$currentText $mentionText "
                    }
                    messageText = TextFieldValue(
                        text = newText,
                        selection = TextRange(newText.length)
                    )
                },
                onMessageLongPress = { /* TODO: message actions */ },
                onCancelTransfer = { msg -> viewModel.cancelMediaSend(msg.id) },
                onImageClick = { currentPath, allImagePaths, initialIndex ->
                    viewerImagePaths = allImagePaths
                    initialViewerIndex = initialIndex
                    showFullScreenImageViewer = true
                }
            )

            // Input section
            ChatInputSection(
                messageText = messageText,
                onMessageTextChange = { newText ->
                    messageText = newText
                    viewModel.updateMentionSuggestions(newText.text)
                },
                onSend = {
                    if (messageText.text.trim().isNotEmpty()) {
                        viewModel.sendMessage(messageText.text.trim())
                        messageText = TextFieldValue("")
                        forceScrollToBottom = !forceScrollToBottom
                    }
                },
                onSendVoiceNote = { peer, channel, path ->
                    viewModel.sendVoiceNote(peer, channel, path)
                },
                onSendImageNote = { peer, channel, path ->
                    viewModel.sendImageNote(peer, channel, path)
                },
                onSendFileNote = { peer, channel, path ->
                    viewModel.sendFileNote(peer, channel, path)
                },
                showCommandSuggestions = false,
                commandSuggestions = emptyList(),
                showMentionSuggestions = false,
                mentionSuggestions = emptyList(),
                onCommandSuggestionClick = { },
                onMentionSuggestionClick = { },
                selectedPrivatePeer = peerID,
                currentChannel = null,
                nickname = nickname,
                colorScheme = colorScheme,
                showMediaButtons = true
            )
        }

        // Floating header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .windowInsetsPadding(WindowInsets.statusBars),
            color = colorScheme.background,
            shadowElevation = 2.dp
        ) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Connection status icon
                        when {
                            isDirect -> Icon(
                                imageVector = Icons.Outlined.SettingsInputAntenna,
                                contentDescription = "Direct connection",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF007AFF)
                            )
                            isConnected -> Icon(
                                imageVector = Icons.Filled.Route,
                                contentDescription = "Routed connection",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF00C851)
                            )
                            isNostrPeer -> Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = "Nostr",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF9C27B0)
                            )
                            else -> Icon(
                                imageVector = Icons.Outlined.Circle,
                                contentDescription = "Offline",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                        }

                        // Peer name
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isNostrPeer) Color(0xFFFF9500) else colorScheme.onSurface
                        )

                        // Encryption indicator
                        if (!isNostrPeer) {
                            Row(
                                modifier = Modifier.then(securityModifier),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NoiseSessionIcon(
                                    sessionState = sessionState,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Verified badge
                        if (isVerified) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF32D74B)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Favorite toggle
                    IconButton(
                        onClick = { viewModel.toggleFavorite(peerID) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            modifier = Modifier.size(20.dp),
                            tint = if (isFavorite) Color(0xFFFFD700) else colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.height(headerHeight)
            )
        }

        // Divider under header
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .offset(y = headerHeight)
                .zIndex(1f),
            color = colorScheme.outline.copy(alpha = 0.3f)
        )

        // Scroll-to-bottom button
        AnimatedVisibility(
            visible = isScrolledUp,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 64.dp)
                .zIndex(1.5f)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.background,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(2.dp, Color(0xFF00C851))
            ) {
                IconButton(onClick = { forceScrollToBottom = !forceScrollToBottom }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = Color(0xFF00C851)
                    )
                }
            }
        }
    }

    // Full-screen image viewer
    if (showFullScreenImageViewer) {
        FullScreenImageViewer(
            imagePaths = viewerImagePaths,
            initialIndex = initialViewerIndex,
            onClose = { showFullScreenImageViewer = false }
        )
    }

    // Security verification sheet
    val showSecurityVerificationSheet by viewModel.showSecurityVerificationSheet.collectAsStateWithLifecycle()
    if (showSecurityVerificationSheet) {
        SecurityVerificationSheet(
            isPresented = showSecurityVerificationSheet,
            onDismiss = viewModel::hideSecurityVerificationSheet,
            viewModel = viewModel
        )
    }
}

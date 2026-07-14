package com.mstf.tiktokchat.chat

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mstf.tiktokchat.R
import com.mstf.tiktokchat.ui.theme.TikTokChatTheme
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun avatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5),
        Color(0xFF03A9F4), Color(0xFF009688), Color(0xFF4CAF50),
        Color(0xFFFF9800), Color(0xFF795548), Color(0xFF607D8B)
    )
    return colors[name.hashCode().absoluteValue % colors.size]
}

private val reactionEmojis = listOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDE4F")

// ---------------------------------------------------------------------------
// Main screen
// ---------------------------------------------------------------------------

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // --- Spotlight animation state (UI concern, stays here) ---
    val spotlightProgress = remember { Animatable(0f) }
    var lastSelectedId by remember { mutableStateOf<String?>(null) }
    var needsShiftOnDismiss by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.selectedMessageId) {
        if (viewModel.selectedMessageId != null) {
            spotlightProgress.animateTo(1f, tween(300))
        } else {
            spotlightProgress.animateTo(0f, tween(200))
            if (needsShiftOnDismiss) {
                delay(350)
            }
            lastSelectedId = null
            needsShiftOnDismiss = false
        }
    }

    // --- Bubble shift to keep emoji bar / action dialog on screen ---
    var targetShiftPx by remember { mutableStateOf(0f) }
    val animatedShiftPx by animateFloatAsState(
        targetValue = targetShiftPx, animationSpec = tween(300), label = "shift"
    )

    LaunchedEffect(viewModel.selectedMessageId, viewModel.overlaySize) {
        if (viewModel.selectedMessageId != null) {
            val rect = viewModel.bubblePositions[viewModel.selectedMessageId]
            if (rect != null && viewModel.overlaySize != IntSize.Zero) {
                val localY = rect.top - viewModel.overlayPosition.y
                val overlayHeight = viewModel.overlaySize.height.toFloat()
                val emojiRoom = with(density) { 56.dp.toPx() }
                val dialogRoom = with(density) { 250.dp.toPx() }
                val overflowTop = maxOf(0f, emojiRoom - localY)
                val bottomY = localY + rect.height
                val overflowBottom = maxOf(0f, dialogRoom - (overlayHeight - bottomY))
                val shift = when {
                    overflowTop > 0f && overflowBottom > 0f -> {
                        val midY = (overlayHeight - rect.height) / 2f
                        midY - localY
                    }
                    overflowTop > 0f -> overflowTop
                    overflowBottom > 0f -> -overflowBottom
                    else -> 0f
                }
                targetShiftPx = shift
                needsShiftOnDismiss = shift != 0f
                lastSelectedId = viewModel.selectedMessageId
            }
        } else {
            targetShiftPx = 0f
        }
    }

    // --- Auto-scroll to bottom ---
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    // --- Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                itemsIndexed(viewModel.messages, key = { _, msg -> msg.id }) { index, message ->
                    val isNewGroup = index == 0 || viewModel.messages[index - 1].isMine != message.isMine
                    val isLastInGroup = index == viewModel.messages.lastIndex || viewModel.messages[index + 1].isMine != message.isMine
                    MessageBubble(
                        message = message,
                        showAvatar = !message.isMine && isLastInGroup,
                        reaction = viewModel.reactions[message.id],
                        onLongPress = { viewModel.selectedMessageId = message.id },
                        onPositioned = { rect -> viewModel.bubblePositions[message.id] = rect },
                        selectedShiftPx = if (message.id == lastSelectedId) animatedShiftPx else 0f,
                        isSelected = message.id == lastSelectedId || message.id == viewModel.selectedMessageId,
                        modifier = Modifier.padding(top = if (isNewGroup) 12.dp else 2.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Input bar
            ChatInputBar(
                inputText = viewModel.inputText,
                senderIsMe = viewModel.senderIsMe,
                onInputChange = { viewModel.inputText = it },
                onSenderMeClick = { viewModel.senderIsMe = true },
                onSenderAliceClick = { viewModel.senderIsMe = false },
                onSend = viewModel::sendMessage
            )
        }

        // Spotlight overlay
        if (spotlightProgress.value > 0f) {
            SpotlightOverlay(
                selectedMessageId = viewModel.selectedMessageId,
                messages = viewModel.messages,
                bubblePositions = viewModel.bubblePositions,
                overlayPosition = viewModel.overlayPosition,
                overlaySize = viewModel.overlaySize,
                spotlightProgress = spotlightProgress.value,
                density = density,
                onDismiss = { viewModel.selectedMessageId = null },
                onEmojiClick = { emoji ->
                    viewModel.selectedMessageId?.let { viewModel.addReaction(it, emoji) }
                },
                onActionClick = { /* handled internally via Toast */ },
                onOverlayLayout = { pos, size ->
                    viewModel.overlayPosition = pos
                    viewModel.overlaySize = size
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ChatInputBar
// ---------------------------------------------------------------------------

@Composable
private fun ChatInputBar(
    inputText: String,
    senderIsMe: Boolean,
    onInputChange: (String) -> Unit,
    onSenderMeClick: () -> Unit,
    onSenderAliceClick: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            // Sender toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Send as:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = senderIsMe,
                    onClick = onSenderMeClick,
                    label = { Text("Me") }
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = !senderIsMe,
                    onClick = onSenderAliceClick,
                    label = { Text("Alice") }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Text field + send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = { Text("Message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSend) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Spotlight overlay + cutout
// ---------------------------------------------------------------------------

@Composable
private fun SpotlightOverlay(
    selectedMessageId: String?,
    messages: List<ChatMessage>,
    bubblePositions: Map<String, Rect>,
    overlayPosition: Offset,
    overlaySize: IntSize,
    spotlightProgress: Float,
    density: Density,
    onDismiss: () -> Unit,
    onEmojiClick: (String) -> Unit,
    onActionClick: (String) -> Unit,
    onOverlayLayout: (Offset, IntSize) -> Unit
) {
    val cutout = selectedMessageId?.let { bubblePositions[it] }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { onOverlayLayout(it.positionInWindow(), it.size) }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = spotlightProgress
            }
            .drawBehind {
                if (cutout != null) {
                    val msg = selectedMessageId?.let { id -> messages.find { it.id == id } }
                    val localX = cutout.left - overlayPosition.x
                    val localY = cutout.top - overlayPosition.y
                    val radiusLarge = with(density) { 16.dp.toPx() }
                    val radiusSmall = with(density) { 4.dp.toPx() }

                    val path = Path().apply {
                        if (msg != null && msg.isMine) {
                            addRoundRect(
                                RoundRect(
                                    left = localX, top = localY,
                                    right = localX + cutout.width, bottom = localY + cutout.height,
                                    topLeftCornerRadius = CornerRadius(radiusLarge),
                                    topRightCornerRadius = CornerRadius(radiusSmall),
                                    bottomLeftCornerRadius = CornerRadius(radiusLarge),
                                    bottomRightCornerRadius = CornerRadius(radiusLarge)
                                )
                            )
                        } else {
                            addRoundRect(
                                RoundRect(
                                    left = localX, top = localY,
                                    right = localX + cutout.width, bottom = localY + cutout.height,
                                    topLeftCornerRadius = CornerRadius(radiusSmall),
                                    topRightCornerRadius = CornerRadius(radiusLarge),
                                    bottomLeftCornerRadius = CornerRadius(radiusLarge),
                                    bottomRightCornerRadius = CornerRadius(radiusLarge)
                                )
                            )
                            val avatarSize = with(density) { 36.dp.toPx() }
                            val avatarMargin = with(density) { 4.dp.toPx() }
                            addOval(
                                Rect(
                                    left = localX + avatarMargin,
                                    top = localY,
                                    right = localX + avatarMargin + avatarSize,
                                    bottom = localY + avatarSize
                                )
                            )
                        }
                    }

                    drawRect(Color.White.copy(alpha = 0.75f))
                    drawPath(path = path, color = Color.Transparent, blendMode = BlendMode.Clear)
                }
            }
            .clickable { onDismiss() }
    ) {
        if (cutout != null) {
            val selectedMessage = messages.find { it.id == selectedMessageId }
            val isMine = selectedMessage?.isMine ?: false
            val localX = cutout.left - overlayPosition.x
            val localY = cutout.top - overlayPosition.y

            EmojiReactionBar(
                isMine = isMine,
                cutout = cutout,
                localX = localX,
                localY = localY,
                density = density,
                slideProgress = spotlightProgress,
                onEmojiClick = onEmojiClick
            )

            ActionDialogMenu(
                isMine = isMine,
                cutout = cutout,
                localX = localX,
                localY = localY,
                density = density,
                onActionClick = onActionClick
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Emoji reaction bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoxScope.EmojiReactionBar(
    isMine: Boolean,
    cutout: Rect,
    localX: Float,
    localY: Float,
    density: Density,
    slideProgress: Float,
    onEmojiClick: (String) -> Unit
) {
    val slideOffset = with(density) { 48.dp.toPx() * (1f - slideProgress) }
    var emojiBarWidth by remember { mutableStateOf(0) }
    val bubbleLeft = if (isMine) localX else localX + with(density) { 44.dp.toPx() }
    val bubbleWidth = if (isMine) cutout.width else cutout.width - with(density) { 44.dp.toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (if (isMine) bubbleLeft + bubbleWidth - emojiBarWidth else bubbleLeft).toInt(),
                    y = (localY - with(density) { 48.dp.toPx() } + slideOffset).toInt()
                )
            }
            .align(Alignment.TopStart)
    ) {
        Surface(
            modifier = Modifier.onSizeChanged { emojiBarWidth = it.width },
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                reactionEmojis.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .combinedClickable(
                                onClick = { onEmojiClick(emoji) },
                                onLongClick = {}
                            )
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Action dialog menu
// ---------------------------------------------------------------------------

@Composable
private fun BoxScope.ActionDialogMenu(
    isMine: Boolean,
    cutout: Rect,
    localX: Float,
    localY: Float,
    density: Density,
    onActionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val actionItems = remember {
        listOf(
            "Reply" to R.drawable.ic_reply,
            "Forward" to R.drawable.ic_forward,
            "Copy" to R.drawable.ic_copy,
            "Delete" to R.drawable.ic_delete,
            "Report" to R.drawable.ic_flag
        )
    }
    var dialogWidth by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .offset {
                val bubbleLeft = if (isMine) localX else localX + with(density) { 44.dp.toPx() }
                val bubbleWidth = if (isMine) cutout.width else cutout.width - with(density) { 44.dp.toPx() }
                val x = if (isMine) (bubbleLeft + bubbleWidth - dialogWidth).toInt() else bubbleLeft.toInt()
                IntOffset(
                    x = x,
                    y = (localY + cutout.height.toInt() + with(density) { 8.dp.toPx() }).toInt()
                )
            }
            .align(Alignment.TopStart)
    ) {
        Surface(
            modifier = Modifier.onSizeChanged { dialogWidth = it.width },
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                actionItems.forEach { (label, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                                onActionClick(label)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = label,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Message bubble
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    showAvatar: Boolean,
    reaction: String?,
    onLongPress: () -> Unit,
    onPositioned: (Rect) -> Unit,
    selectedShiftPx: Float = 0f,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (message.isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (message.isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (message.isMine) {
        RoundedCornerShape(
            topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp
        )
    }

    val avatarPx = with(LocalDensity.current) { 44.dp.toPx() }

    Row(
        modifier = modifier
            .zIndex(if (isSelected) 10f else 0f)
            .offset { IntOffset(x = 0, y = selectedShiftPx.toInt()) }
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        // Avatar area for other person — reserve space even when hidden
        if (!message.isMine) {
            if (showAvatar) {
                Avatar(name = message.senderName, modifier = Modifier.padding(end = 8.dp))
            } else {
                Box(modifier = Modifier.width(44.dp))
            }
        }

        // Message bubble
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInWindow()
                val size = coordinates.size
                val left = if (message.isMine) pos.x else pos.x - avatarPx
                onPositioned(Rect(left, pos.y, pos.x + size.width, pos.y + size.height))
            }
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    if (!message.isMine) {
                        Text(
                            text = message.senderName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                        text = message.text,
                        color = contentColor,
                        fontSize = 16.sp
                    )
                }
            }

            // Reaction badge
            if (reaction != null) {
                val badgeAlign = if (message.isMine) Alignment.BottomStart else Alignment.BottomEnd
                Surface(
                    modifier = Modifier
                        .align(badgeAlign)
                        .offset(x = if (message.isMine) (-6).dp else 6.dp, y = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = reaction,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Avatar
// ---------------------------------------------------------------------------

@Composable
private fun Avatar(name: String, modifier: Modifier = Modifier) {
    val color = avatarColor(name)
    val initial = if (name.isNotEmpty()) name.first().uppercase() else "?"

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    TikTokChatTheme {
        ChatScreen(viewModel = ChatViewModel())
    }
}

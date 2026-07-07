package com.mstf.tiktokchat.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mstf.tiktokchat.ui.theme.TikTokChatTheme
import kotlin.math.absoluteValue

// Generate a consistent color from a name
private fun avatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5),
        Color(0xFF03A9F4), Color(0xFF009688), Color(0xFF4CAF50),
        Color(0xFFFF9800), Color(0xFF795548), Color(0xFF607D8B)
    )
    return colors[name.hashCode().absoluteValue % colors.size]
}

@Composable
fun ChatScreen() {
    val messages = remember {
        mutableStateListOf(
            ChatMessage("1", "Hey! How are you?", isMine = false, senderName = "Alice"),
            ChatMessage("2", "I'm good, thanks! What about you?", isMine = true, senderName = "Me"),
            ChatMessage("3", "Doing great! Want to grab coffee later?", isMine = false, senderName = "Alice"),
            ChatMessage("4", "Sure, sounds like a plan!", isMine = true, senderName = "Me")
        )
    }

    var inputText by remember { mutableStateOf("") }
    var senderIsMe by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    var messageCounter by remember { mutableStateOf(5) }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
            itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
                val isNewGroup = index == 0 || messages[index - 1].isMine != message.isMine
                val isLastInGroup = index == messages.lastIndex || messages[index + 1].isMine != message.isMine
                MessageBubble(
                    message = message,
                    showAvatar = !message.isMine && isLastInGroup,
                    modifier = Modifier.padding(top = if (isNewGroup) 12.dp else 2.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Input bar
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Send as:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = senderIsMe,
                        onClick = { senderIsMe = true },
                        label = { Text("Me") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = !senderIsMe,
                        onClick = { senderIsMe = false },
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
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isNotEmpty()) {
                                messages.add(
                                    ChatMessage(
                                        id = messageCounter.toString(),
                                        text = text,
                                        isMine = senderIsMe,
                                        senderName = if (senderIsMe) "Me" else "Alice"
                                    )
                                )
                                inputText = ""
                                messageCounter++
                            }
                        }
                    ) {
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
}

@Composable
private fun MessageBubble(message: ChatMessage, showAvatar: Boolean, modifier: Modifier = Modifier) {
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
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        // Avatar area for other person (left side) — reserve space even when hidden
        if (!message.isMine) {
            if (showAvatar) {
                Avatar(name = message.senderName, modifier = Modifier.padding(end = 8.dp))
            } else {
                Box(modifier = Modifier.width(44.dp))
            }
        }

        // Message bubble
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
    }
}

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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    TikTokChatTheme {
        ChatScreen()
    }
}

package com.mstf.tiktokchat.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {

    val messages = mutableStateListOf(
        ChatMessage("1", "Hey! How are you?", isMine = false, senderName = "Alice"),
        ChatMessage("2", "I'm good, thanks! What about you?", isMine = true, senderName = "Me"),
        ChatMessage("3", "Doing great! Want to grab coffee later?", isMine = false, senderName = "Alice"),
        ChatMessage("4", "Sure, sounds like a plan!", isMine = true, senderName = "Me"),
        ChatMessage("5", "How about 3pm at the usual spot?", isMine = false, senderName = "Alice"),
        ChatMessage("6", "Perfect, see you there \uD83D\uDC4B", isMine = true, senderName = "Me"),
        ChatMessage("7", "Btw, did you finish the project?", isMine = false, senderName = "Alice"),
        ChatMessage("8", "Almost done! Just polishing a few things.", isMine = true, senderName = "Me"),
        ChatMessage("9", "Awesome, can't wait to see it!", isMine = false, senderName = "Alice"),
        ChatMessage("10", "Thanks! I'll show you the demo soon.", isMine = true, senderName = "Me")
    )

    var inputText by mutableStateOf("")
    var senderIsMe by mutableStateOf(true)
    var messageCounter by mutableStateOf(11)
    var selectedMessageId by mutableStateOf<String?>(null)

    val bubblePositions = mutableStateMapOf<String, Rect>()
    var overlayPosition by mutableStateOf(Offset.Zero)
    var overlaySize by mutableStateOf(IntSize.Zero)
    val reactions = mutableStateMapOf<String, String>()

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty()) return
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

    fun addReaction(messageId: String, emoji: String) {
        reactions[messageId] = emoji
        selectedMessageId = null
    }
}

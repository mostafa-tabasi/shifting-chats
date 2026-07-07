package com.mstf.tiktokchat.chat

data class ChatMessage(
    val id: String,
    val text: String,
    val isMine: Boolean,
    val senderName: String
)

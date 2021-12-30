package com.example.data.models

data class ChatToSendAsRecent(
    val chattingTo: String,
    val newMessages: Boolean,
    val lastMessageTimestamp: Long
)

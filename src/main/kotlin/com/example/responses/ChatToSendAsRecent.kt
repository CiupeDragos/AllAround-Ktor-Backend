package com.example.responses

data class ChatToSendAsRecent(
    val chattingTo: String,
    val newMessages: Int,
    val lastMessage: String,
    val lastMessageSender: String,
    val lastMessageTimestamp: Long,
    val typeOfChat: String = "TYPE_CHAT"
)

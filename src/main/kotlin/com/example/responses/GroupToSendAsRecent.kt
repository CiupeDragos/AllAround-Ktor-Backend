package com.example.responses

data class GroupToSendAsRecent(
    val name: String,
    val groupId: String,
    val newMessages: Int,
    val lastMessage: String,
    val lastMessageSender: String,
    val lastMessageTimestamp: Long,
    val owner: String,
    val typeOfChat: String = "TYPE_GROUP"
)

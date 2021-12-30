package com.example.data.models

data class GroupToSendAsRecent(
    val name: String,
    val groupId: String,
    val newMessages: Boolean,
    val lastMessageTimestamp: Long
)

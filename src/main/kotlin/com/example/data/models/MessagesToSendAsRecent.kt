package com.example.data.models

import com.example.util.Constants.TYPE_RECENT_CHATS

data class MessagesToSendAsRecent(
    val chats: List<ChatToSendAsRecent>,
    val groups: List<GroupToSendAsRecent>
): BaseModel(TYPE_RECENT_CHATS)

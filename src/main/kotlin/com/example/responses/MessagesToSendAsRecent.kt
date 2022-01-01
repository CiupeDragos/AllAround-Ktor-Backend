package com.example.responses

import com.example.data.models.BaseModel
import com.example.util.Constants.TYPE_RECENT_CHATS

data class MessagesToSendAsRecent(
    val chats: List<ChatToSendAsRecent>,
    val groups: List<GroupToSendAsRecent>
): BaseModel(TYPE_RECENT_CHATS)

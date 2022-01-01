package com.example.responses

import com.example.data.models.BaseModel
import com.example.data.models.NormalChatMessage
import com.example.util.Constants.TYPE_MESSAGES_FOR_THIS_CHAT

data class MessagesForThisChat(
    val messages: List<NormalChatMessage>
): BaseModel(TYPE_MESSAGES_FOR_THIS_CHAT)

package com.example.responses

import com.example.data.models.BaseModel
import com.example.data.models.ChatGroupMessage
import com.example.util.Constants.TYPE_MESSAGES_FOR_THIS_GROUP

data class MessagesForThisGroup(
    val messages: List<ChatGroupMessage>
): BaseModel(TYPE_MESSAGES_FOR_THIS_GROUP)

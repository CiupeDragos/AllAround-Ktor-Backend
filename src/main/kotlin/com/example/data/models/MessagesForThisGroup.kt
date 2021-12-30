package com.example.data.models

import com.example.util.Constants.TYPE_MESSAGES_FOR_THIS_GROUP

data class MessagesForThisGroup(
    val messages: List<ChatGroupMessage>
): BaseModel(TYPE_MESSAGES_FOR_THIS_GROUP)

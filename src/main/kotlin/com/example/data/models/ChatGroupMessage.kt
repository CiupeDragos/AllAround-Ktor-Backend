package com.example.data.models

import com.example.util.Constants.TYPE_CHAT_GROUP_MESSAGE
import org.bson.codecs.pojo.annotations.BsonId

data class ChatGroupMessage(
    val message: String,
    val sender: String,
    val timestamp: Long,
    val groupId: String,
    @BsonId
    val messageId: String? = null
): BaseModel(TYPE_CHAT_GROUP_MESSAGE)

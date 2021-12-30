package com.example.data.models

import com.example.util.Constants.TYPE_NORMAL_CHAT_MESSAGE
import org.bson.codecs.pojo.annotations.BsonId

data class NormalChatMessage(
    val message: String,
    val sender: String,
    val receiver: String,
    val timestamp: Long,
    @BsonId
    val messageId: String? = null
): BaseModel(TYPE_NORMAL_CHAT_MESSAGE)

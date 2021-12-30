package com.example.data.models

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class ChatGroup(
    val name: String,
    val owner: String,
    val admins: List<String>,
    val members: List<String>,
    var lastMessageTimestamp: Long? = -1,
    @BsonId
    val groupId: String = ObjectId().toString()
)
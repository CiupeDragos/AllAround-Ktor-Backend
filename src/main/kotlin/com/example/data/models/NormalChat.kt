package com.example.data.models

import org.bson.codecs.pojo.annotations.BsonId

data class NormalChat(
    val participant1: String,
    val participant2: String,
    var messages: List<String>,
    val deletedForP1: String? = null,
    val deletedForP2: String? = null,
    var lastMessageTimestamp: Long? = -1,
    @BsonId
    val chatId: String? = null
)

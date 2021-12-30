package com.example.data.models

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    val username: String,
    val password: String,
    val friends: List<String>,
    val lastReadForGroups: List<String> = listOf(),
    val lastReadForChats: List<String> = listOf(),
    @BsonId
    val userId: String = ObjectId().toString()
)

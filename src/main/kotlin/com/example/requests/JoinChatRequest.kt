package com.example.requests

import com.example.data.models.BaseModel
import com.example.util.Constants.TYPE_JOIN_CHAT_REQUEST

data class JoinChatRequest(
    val username: String,
    val chatPartner: String
): BaseModel(TYPE_JOIN_CHAT_REQUEST)

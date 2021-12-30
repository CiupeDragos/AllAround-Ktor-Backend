package com.example.requests

import com.example.data.models.BaseModel
import com.example.util.Constants.TYPE_JOIN_MY_CHATS_REQUEST

data class JoinMyChatsRequest(val username: String): BaseModel(TYPE_JOIN_MY_CHATS_REQUEST)

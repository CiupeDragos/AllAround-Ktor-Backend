package com.example.requests

import com.example.data.models.BaseModel
import com.example.util.Constants.TYPE_DISCONNECT_UNCHATTING_USER

data class DisconnectUnchattingUser(
    val username: String
): BaseModel(TYPE_DISCONNECT_UNCHATTING_USER)

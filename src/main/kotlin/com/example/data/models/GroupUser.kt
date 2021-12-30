package com.example.data.models

import io.ktor.http.cio.websocket.*

data class GroupUser(
    val username: String,
    val socket: WebSocketSession,
    val groupId: String
)

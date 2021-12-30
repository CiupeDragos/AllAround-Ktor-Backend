package com.example.data.models

import io.ktor.http.cio.websocket.*

data class NormalChatUser(
    val username: String,
    val socket: WebSocketSession,
    val chatPartner: String
)

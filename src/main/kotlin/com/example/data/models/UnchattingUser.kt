package com.example.data.models

import io.ktor.http.cio.websocket.*

data class UnchattingUser(
    val username: String,
    val socket: WebSocketSession
)

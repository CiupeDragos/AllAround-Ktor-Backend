package com.example.plugins

import io.ktor.application.*
import io.ktor.websocket.*

fun Application.configureSockets() {
    install(WebSockets)
}
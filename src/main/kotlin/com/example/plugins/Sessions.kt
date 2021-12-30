package com.example.plugins

import com.example.data.models.ChatSession
import io.ktor.application.*
import io.ktor.sessions.*
import io.ktor.util.*
import kotlin.collections.set

fun Application.configureSessions() {
    install(Sessions) {
        cookie<ChatSession>("SESSION")
    }

    intercept(ApplicationCallPipeline.Features) {
        val username = call.parameters["username"]
        val session = call.sessions.get<ChatSession>()
        if(username != null) {
            if(session == null) {
                call.sessions.set(ChatSession(username, generateNonce()))
            }
        }
    }
}
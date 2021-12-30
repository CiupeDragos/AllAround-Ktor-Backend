package com.example.plugins

import com.example.routes.connectToChat
import com.example.routes.createChatGroup
import com.example.routes.loginRoute
import com.example.routes.registerRoute
import io.ktor.routing.*
import io.ktor.application.*

fun Application.configureRouting() {
    install(Routing) {
        registerRoute()
        loginRoute()
        createChatGroup()
        connectToChat()
    }
}

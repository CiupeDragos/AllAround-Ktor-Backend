package com.example.plugins

import com.example.routes.*
import io.ktor.routing.*
import io.ktor.application.*

fun Application.configureRouting() {
    install(Routing) {
        registerRoute()
        loginRoute()
        createChatGroup()
        connectToChat()
        findFriends()
        findUsers()
    }
}

package com.example.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.routing.*
import org.litote.kmongo.json

fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
}
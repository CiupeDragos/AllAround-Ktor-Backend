package com.example.routes

import com.example.data.*
import com.example.data.models.ChatSession
import com.example.data.models.User
import com.example.requests.AccountRequest
import com.example.requests.CreateGroupRequest
import com.example.responses.BasicApiResponse
import com.example.util.Constants.MAX_CHAT_GROUP_NAME_LENGTH
import com.example.util.Constants.MIN_CHAT_GROUP_NAME_LENGTH
import io.ktor.application.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import org.litote.kmongo.eq

fun Route.registerRoute() {
    route("/registerAccount") {
        post {
            val registerRequest = try {
                call.receive<AccountRequest>()
            } catch (e: ContentTransformationException) {
                call.respond(BadRequest, "Bad register request")
                return@post
            }
            println(registerRequest)

            if(checkIfUserExists(registerRequest.username)) {
                call.respond(OK, BasicApiResponse(false, "An account with this username is already registered"))
                return@post
            }
            if(registerUser(registerRequest.username, registerRequest.password)) {
                call.respond(OK, BasicApiResponse(true, "Account registered successfully"))
            } else {
                call.respond(OK, BasicApiResponse(false, "Password or username do not meet the length requirements"))
            }
        }
    }
}

fun Route.loginRoute() {
    post("/loginAccount") {
        val loginRequest = call.receiveOrNull<AccountRequest>()

        if(loginRequest == null) {
            call.respond(BadRequest, BasicApiResponse(false, "Bad login request"))
            return@post
        }
        if(!checkIfUserExists(loginRequest.username)) {
            println(loginRequest.username)
            call.respond(OK, BasicApiResponse(false,"No account with this username exists"))
            return@post
        }
        if(loginUser(loginRequest.username, loginRequest.password)) {
            call.respond(OK, BasicApiResponse(true, "Successfully logged in"))
        } else {
            call.respond(OK, BasicApiResponse(false, "Username or password do not match"))
        }
    }
}

fun Route.findUsers() {
    get("/findUsers") {
        val session = call.sessions.get<ChatSession>()
        if(session == null) {
            call.respond(BadRequest, "No session")
            return@get
        }
        val username = call.parameters["username"]
        if(username == null) {
            call.respond(BadRequest, "Username search query not provided")
            return@get
        }
        //Should modify to search all the users whose usernames contain the search query
        val users = users.find(User::username eq username).toList()
        if(users.isEmpty()) {
            call.respond(BadRequest, "No users found")
        } else {
            val userList = users.map {
                it.username
            }
            call.respond(OK, userList)
        }
    }
}

fun Route.findFriends() {
    get("/findFriends") {
        val session = call.sessions.get<ChatSession>()
        if(session == null) {
            call.respond(BadRequest, "No session")
            return@get
        }
        val username = session.username
        val user = users.findOne(User::username eq username)
        if(user == null) {
            call.respond(BadRequest, "User does not exist")
            return@get
        }
        val friends = user.friends
        call.respond(OK, friends)
    }
}

fun Route.createChatGroup() {
    post("/createChatGroup") {

        val chatGroupRequest = call.receiveOrNull<CreateGroupRequest>()
        val session = call.sessions.get<ChatSession>()

        println(session?.username ?: "No session")

        println(chatGroupRequest)
        if(chatGroupRequest == null){
            call.respond(BadRequest, BasicApiResponse(false, "Bad chat group create request"))
            return@post
        }
        if(session == null) {
            call.respond(BadRequest, BasicApiResponse(false, "No session"))
            return@post
        }
        if(!checkIfUserExists(session.username)){
            call.respond(Conflict, BasicApiResponse(false, "No account with the session's username exists"))
            return@post
        }
        if(hasGroupWithThatName(session.username, chatGroupRequest.name)) {
            call.respond(Conflict, BasicApiResponse(false, "There's already a chat group with this name owned by ${session.username}"))
            return@post
        }
        if(validateAndCreateChatGroup(session.username, chatGroupRequest.name, chatGroupRequest.members)) {
            call.respond(OK, BasicApiResponse(true, "Chat group '${chatGroupRequest.name}' created successfully"))
        } else {
            call.respond(
                OK,
                BasicApiResponse(
                    false,
                    "Group name length must be between $MIN_CHAT_GROUP_NAME_LENGTH and $MAX_CHAT_GROUP_NAME_LENGTH characters"
                )
            )
        }
    }
}
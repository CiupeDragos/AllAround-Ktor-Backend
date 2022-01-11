package com.example.routes

import com.example.data.*
import com.example.data.models.ChatSession
import com.example.data.models.User
import com.example.requests.AccountRequest
import com.example.requests.CreateGroupRequest
import com.example.responses.BasicApiResponse
import com.example.responses.FriendRequestsResponse
import com.example.server.gson
import com.example.util.Constants.MAX_CHAT_GROUP_NAME_LENGTH
import com.example.util.Constants.MIN_CHAT_GROUP_NAME_LENGTH
import com.example.util.Constants.NO_GROUP_ID
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.ContentDisposition.Companion.File
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import org.litote.kmongo.eq
import java.io.File
import java.security.BasicPermission

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
        val username = call.parameters["usernameToSearch"]
        if(username == null) {
            call.respond(BadRequest, "Username search query not provided")
            return@get
        }
        //Should modify to search for all the users whose usernames contain the search query
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
            call.respond(OK, BasicApiResponse(false, "No account with the session's username exists"))
            return@post
        }
        if(hasGroupWithThatName(session.username, chatGroupRequest.name)) {
            call.respond(OK, BasicApiResponse(false, "There's already a chat group with this name owned by ${session.username}"))
            return@post
        }
        val createGroupCall = validateAndCreateChatGroup(session.username, chatGroupRequest.name, chatGroupRequest.members)
        if(createGroupCall != NO_GROUP_ID) {
            call.respond(OK, BasicApiResponse(true, createGroupCall))
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

fun Route.getFriendRequests() {
    get("/getFriendRequests") {
        val session = call.sessions.get<ChatSession>()
        val username = session?.username
        if(session ==  null) {
            call.respond(BadRequest, "No session")
            return@get
        }
        val user = users.findOne(User::username eq username)
        if(user == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@get
        }
        if(user.sentFriends.isEmpty() && user.receivedFriends.isEmpty()) {
            call.respond(BadRequest, "No requests")
            return@get
        }
        val sentFriendRequest = user.sentFriends
        val receivedFriendRequests = user.receivedFriends
        val response = FriendRequestsResponse(sentFriendRequest, receivedFriendRequests)
        call.respond(OK, response)
    }
}

fun Route.sendFriendRequest() {
    post("/sendFriendRequest") {
        val session = call.sessions.get<ChatSession>()
        val username = session?.username
        val sentToUsername = call.parameters["sentToUsername"]
        if(session ==  null) {
            call.respond(BadRequest, "No session")
            return@post
        }
        val user = users.findOne(User::username eq username)
        if(user == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        if(sentToUsername == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val sentToUser = users.findOne(User::username eq sentToUsername)
        if(sentToUser == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val modifiedSentRequests = user.sentFriends.toMutableList()
        val modifiedReceivedRequests = sentToUser.receivedFriends.toMutableList()
        val senderReceivedRequests = user.receivedFriends.toMutableList()
        if(sentToUsername in senderReceivedRequests) {
            call.respond(OK, BasicApiResponse(false, "This user already sent you a friend request"))
            return@post
        }
        if(sentToUsername in modifiedSentRequests) {
            call.respond(OK, BasicApiResponse(false, "You can not send a friend request twice"))
            return@post
        }
        if(sentToUsername in user.friends) {
            call.respond(OK, BasicApiResponse(false, "You are already friends with $sentToUsername"))
            return@post
        }
        if(sentToUsername == username) {
            call.respond(BasicApiResponse(false, "You can't send a friend request to yourself"))
            return@post
        }
        modifiedSentRequests.add(sentToUsername)
        modifiedReceivedRequests.add(username!!)
        users.replaceOneById(user.userId, user.copy(sentFriends = modifiedSentRequests.toList()))
        users.replaceOneById(sentToUser.userId, sentToUser.copy(receivedFriends = modifiedReceivedRequests.toList()))

        call.respond(OK, BasicApiResponse(true, "You have sent a friend request to $sentToUsername"))
    }
}

fun Route.cancelFriendRequest() {
    post("/cancelFriendRequest") {
        val session = call.sessions.get<ChatSession>()
        val username = session?.username
        val sentToUsername = call.parameters["sentToUsername"]
        if(session == null) {
            call.respond(BadRequest, "No session")
            return@post
        }
        if(sentToUsername == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val user = users.findOne(User::username eq username)
        val sentToUser = users.findOne(User::username eq sentToUsername)
        if(user == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        if(sentToUser == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val userSentRequests = user.sentFriends.toMutableList()
        val sentToUserReceivedRequests = sentToUser.receivedFriends.toMutableList()
        userSentRequests.remove(sentToUsername)
        sentToUserReceivedRequests.remove(username!!)
        users.replaceOneById(user.userId, user.copy(sentFriends = userSentRequests))
        users.replaceOneById(sentToUser.userId, sentToUser.copy(receivedFriends = sentToUserReceivedRequests))

        call.respond(OK, BasicApiResponse(true, "Friend request canceled"))
    }
}

fun Route.refuseFriendRequest() {
    post("/refuseFriendRequest") {
        val session = call.sessions.get<ChatSession>()
        val username = session?.username
        val senderUsername = call.parameters["senderUsername"]
        if(session == null) {
            call.respond(BadRequest, "No session")
            return@post
        }
        if(senderUsername == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val user = users.findOne(User::username eq username)
        val senderUser = users.findOne(User::username eq senderUsername)
        if(user == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        if(senderUser == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val userReceivedRequests = user.receivedFriends.toMutableList()
        val senderUserSentRequests = senderUser.sentFriends.toMutableList()
        userReceivedRequests.remove(senderUsername)
        senderUserSentRequests.remove(username!!)
        users.replaceOneById(user.userId, user.copy(receivedFriends = userReceivedRequests.toList()))
        users.replaceOneById(senderUser.userId, senderUser.copy(sentFriends = senderUserSentRequests.toList()))

        call.respond(OK, BasicApiResponse(true, "Friend request refused"))
    }
}

fun Route.acceptFriendRequest() {
    post("/acceptFriendRequest") {
        val session = call.sessions.get<ChatSession>()
        val username = session?.username
        val senderUsername = call.parameters["senderUsername"]
        if(session == null) {
            call.respond(BadRequest, "No session")
            return@post
        }
        if(senderUsername == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val user = users.findOne(User::username eq username)
        val senderUser = users.findOne(User::username eq senderUsername)
        if(user == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        if(senderUser == null) {
            call.respond(BadRequest, "No user with this username exists")
            return@post
        }
        val userReceivedRequests = user.receivedFriends.toMutableList()
        val senderUserSentRequests = senderUser.sentFriends.toMutableList()
        val userFriendList = user.friends.toMutableList()
        val senderFriendList = senderUser.friends.toMutableList()
        userReceivedRequests.remove(senderUsername)
        userFriendList.add(senderUsername)
        senderFriendList.add(username!!)
        senderUserSentRequests.remove(username)
        users.replaceOneById(
            user.userId,
            user.copy(receivedFriends = userReceivedRequests.toList(), friends = userFriendList.toList())
        )
        users.replaceOneById(
            senderUser.userId,
            senderUser.copy(sentFriends = senderUserSentRequests.toList(), friends = senderFriendList.toList())
        )

        call.respond(OK, BasicApiResponse(true, "Friend request accepted"))
    }
}

//Testing image uploading

fun Route.upload() {
    post("/uploadImage") {
        val multipartData = try {
            call.receiveMultipart()
        } catch (e: Exception) {
            call.respond(BadRequest, "Bad request")
            return@post
        }
        var userId = ""

        multipartData.forEachPart { part ->
            when(part) {
                is PartData.FormItem -> {
                    userId = part.value
                    println(userId)
                }
                is PartData.FileItem -> {
                    val fileName = part.originalFileName as String
                    val fileBytes = part.streamProvider().readBytes()
                    File("uploads/$fileName.jpg").writeBytes(fileBytes)
                    call.respond(OK, BasicApiResponse(true,"Image uploaded successfully"))
                }
            }
        }
    }
}
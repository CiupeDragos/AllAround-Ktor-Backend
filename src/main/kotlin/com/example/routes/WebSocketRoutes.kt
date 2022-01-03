package com.example.routes

import com.example.data.models.*
import com.example.data.saveChatGroupMessage
import com.example.data.saveNormalChatMessage
import com.example.requests.JoinChatRequest
import com.example.requests.JoinGroupRequest
import com.example.requests.JoinMyChatsRequest
import com.example.responses.ConnectedToSocket
import com.example.server.*
import com.example.util.Constants.TYPE_CHAT_GROUP_MESSAGE
import com.example.util.Constants.TYPE_JOIN_CHAT_REQUEST
import com.example.util.Constants.TYPE_JOIN_GROUP_REQUEST
import com.example.util.Constants.TYPE_JOIN_MY_CHATS_REQUEST
import com.example.util.Constants.TYPE_NORMAL_CHAT_MESSAGE
import com.google.gson.JsonParser

import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach


fun Route.connectToChat() {
    route("/connectToChat") {
        standardWebSocket { socket, unparsedJson, parsedJson, _ ->
            when(parsedJson) {
                is JoinMyChatsRequest -> {
                    if(!addNotChattingUser(parsedJson.username, socket)) {
                        //socket.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Already online as a not chatting user"))
                        return@standardWebSocket
                    } else {
                        println("${parsedJson.username} joined the recent chats")
                        tryDisconnectChattingUsers(parsedJson.username)
                        val messages = getAllRecentChatsForUser(parsedJson.username)
                        socket.send(Frame.Text(messages))
                    }
                }
                is JoinChatRequest -> {
                    if(!addNormalChatUser(parsedJson.username, socket, parsedJson.chatPartner)) {
                        //socket.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Already online as a chat user"))
                        return@standardWebSocket
                    } else {
                        println("${parsedJson.username} joined the chat with ${parsedJson.chatPartner}")
                        val messagesForThisChat = getAllChatMessages(parsedJson.username, parsedJson.chatPartner)
                        socket.send(Frame.Text(messagesForThisChat))
                        tryDisconnectUnchattingUser(parsedJson.username)
                    }
                }
                is JoinGroupRequest -> {
                    if(!addChatGroupUser(parsedJson.username, socket, parsedJson.groupId)) {
                        socket.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Already online as a chat group user"))
                        return@standardWebSocket
                    } else {
                        val messagesForThisChatGroup = getAllChatGroupMessages(parsedJson.groupId)
                        socket.send(Frame.Text(messagesForThisChatGroup))
                        tryDisconnectUnchattingUser(parsedJson.username)
                    }
                }
                is NormalChatMessage -> {
                    saveNormalChatMessage(parsedJson)
                    broadcastNormalChatMessage(unparsedJson, parsedJson.sender, parsedJson.receiver)

                }
                is ChatGroupMessage -> {
                    saveChatGroupMessage(parsedJson)
                    broadcastChatGroupMessage(unparsedJson, parsedJson.groupId)
                }
            }
        }
    }
}

fun Route.standardWebSocket(
    handleFrame: suspend (
        socket: DefaultWebSocketServerSession,
        unparsedJson: String,
        parsedJson: BaseModel,
        senderUsername: String
    ) -> Unit
) {
    webSocket {
        val session = call.sessions.get<ChatSession>()
        if(session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
            return@webSocket
        }
        //send connected event for client side socket reconnect bug
        this.send(Frame.Text(gson.toJson(ConnectedToSocket())))
        println("sent connected to socket to ${session.username}")
        try {
            incoming.consumeEach { frame ->
                if(frame is Frame.Text) {
                    val unparsedString = frame.readText()
                    val jsonObject = JsonParser.parseString(unparsedString).asJsonObject
                    val type = when(jsonObject.get("type").asString) {
                        TYPE_JOIN_CHAT_REQUEST -> JoinChatRequest::class.java
                        TYPE_JOIN_GROUP_REQUEST -> JoinGroupRequest::class.java
                        TYPE_NORMAL_CHAT_MESSAGE -> NormalChatMessage::class.java
                        TYPE_CHAT_GROUP_MESSAGE -> ChatGroupMessage::class.java
                        TYPE_JOIN_MY_CHATS_REQUEST -> JoinMyChatsRequest::class.java
                        else -> BaseModel::class.java
                    }
                    val parsedJsonMessage = gson.fromJson(unparsedString, type)

                    handleFrame(this, unparsedString, parsedJsonMessage, session.username)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tryDisconnect(session.username)
        }
    }
}
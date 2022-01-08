package com.example.server

import com.example.data.*
import com.example.data.models.*
import com.example.responses.*
import com.google.gson.Gson
import io.ktor.http.cio.websocket.*
import org.bson.types.ObjectId
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import java.util.concurrent.ConcurrentHashMap

val gson = Gson()
val onlineGroupUsers = ConcurrentHashMap<String, GroupUser>()
val onlineChatUsers = ConcurrentHashMap<String, NormalChatUser>()
val onlineNotChattingUsers = ConcurrentHashMap<String, UnchattingUser>()


suspend fun tryDisconnect(username: String) {
    if (onlineGroupUsers.containsKey(username)) {
        addLastReadTimestampForGroup(username, onlineGroupUsers[username]!!.groupId)
        onlineGroupUsers[username]!!.socket.close(CloseReason(CloseReason.Codes.NORMAL, "Socket closed"))
        onlineGroupUsers.remove(username)
    } else if (onlineChatUsers.containsKey(username)) {
        addLastReadTimestampForChat(username, onlineChatUsers[username]!!.chatPartner)
        onlineChatUsers[username]!!.socket.close(CloseReason(CloseReason.Codes.NORMAL, "Socket closed"))
        onlineChatUsers.remove(username)
        println("$username disconnected this chat")
    } else {
        if (onlineNotChattingUsers.containsKey(username)) {
            onlineNotChattingUsers[username]!!.socket.close(CloseReason(CloseReason.Codes.NORMAL, "Socket closed"))
            onlineNotChattingUsers.remove(username)
            println("$username disconnected from recent chats")
        }
    }
}

suspend fun tryDisconnectChattingUsers(username: String) {
    if (onlineGroupUsers.containsKey(username)) {
        addLastReadTimestampForGroup(username, onlineGroupUsers[username]!!.groupId)
        onlineGroupUsers.remove(username)
    } else if (onlineChatUsers.containsKey(username)) {
        addLastReadTimestampForChat(username, onlineChatUsers[username]!!.chatPartner)
        onlineChatUsers.remove(username)
    }
}

suspend fun addNormalChatUser(username: String, socket: WebSocketSession, chatPartner: String): Boolean {
    if (onlineChatUsers.containsKey(username)) {
        return false
    }
    createChat(username, chatPartner)

    val chatUser = NormalChatUser(
        username = username,
        socket = socket,
        chatPartner = chatPartner
    )
    onlineChatUsers[username] = chatUser
    return true
}

fun addNotChattingUser(username: String, socket: WebSocketSession): Boolean {
    if (onlineNotChattingUsers.containsKey(username)) {
        return false
    }
    val unchattingUser = UnchattingUser(
        username = username,
        socket = socket
    )
    onlineNotChattingUsers[username] = unchattingUser
    return true
}

fun tryDisconnectUnchattingUser(username: String) {
    if (onlineNotChattingUsers.containsKey(username)) {
        onlineNotChattingUsers.remove(username)
    }
}


fun addChatGroupUser(username: String, socket: WebSocketSession, groupId: String): Boolean {
    if (onlineGroupUsers.containsKey(username)) {
        return false
    }
    val groupUser = GroupUser(
        username = username,
        socket = socket,
        groupId = groupId
    )
    onlineGroupUsers[username] = groupUser
    return true
}

suspend fun addLastReadTimestampForChat(currentPerson: String, chatPartner: String) {
    val chat = findChat(currentPerson, chatPartner) ?: return
    val user = users.findOne(User::username eq currentPerson) ?: return
    val lastReadTimestamps = user.lastReadForChats.toMutableList()
    val stringToCheck = "${chat.chatId}:"
    var positionToChange = -1
    for (i in lastReadTimestamps.indices) {
        if (lastReadTimestamps[i].contains(stringToCheck)) {
            positionToChange = i
            break
        }
    }
    if (positionToChange != -1) {
        lastReadTimestamps[positionToChange] = "${stringToCheck}${System.currentTimeMillis()}"
    } else {
        lastReadTimestamps.add("${stringToCheck}${System.currentTimeMillis()}")
    }
    val listToUpdate = lastReadTimestamps.toList()
    val userToUpdate = user.copy(lastReadForChats = listToUpdate)
    users.updateOneById(user.userId, userToUpdate)
}

suspend fun addLastReadTimestampForGroup(username: String, groupId: String) {
    val user = users.findOne(User::username eq username) ?: return
    val lastReadTimestamps = user.lastReadForGroups.toMutableList()
    val stringToCheck = "${groupId}:"
    var positionToChange = -1
    for (i in lastReadTimestamps.indices) {
        if (lastReadTimestamps[i].contains(stringToCheck)) {
            positionToChange = i
            break
        }
    }
    if (positionToChange != -1) {
        lastReadTimestamps[positionToChange] = "${stringToCheck}${System.currentTimeMillis()}"
    } else {
        lastReadTimestamps.add("${stringToCheck}${System.currentTimeMillis()}")
    }
    val listToUpdate = lastReadTimestamps.toList()
    val userToUpdate = user.copy(lastReadForGroups = listToUpdate)
    users.replaceOneById(user.userId, userToUpdate)
}

suspend fun broadcastNormalChatMessage(message: String, messageSender: String, messageReceiver: String) {
    val sender = onlineChatUsers[messageSender]
    val receiver = onlineChatUsers[messageReceiver]

    sender!!.socket.send(Frame.Text(message))

    if (receiver != null && receiver.chatPartner == messageSender) {
        receiver.socket.send(Frame.Text(message))
    } else {
        if(onlineNotChattingUsers.containsKey(messageReceiver)) {
            onlineNotChattingUsers[messageReceiver]!!.socket.send(Frame.Text(getAllRecentChatsForUser(messageReceiver)))
        }
    }
}

suspend fun broadcastChatGroupMessage(message: String, groupId: String) {
    onlineGroupUsers.values.forEach { groupUser ->
        if (groupUser.groupId == groupId) {
            groupUser.socket.send(Frame.Text(message))
        }
    }
    val groupMembers = (chatGroups.findOneById(groupId) ?: return).members
    onlineNotChattingUsers.values.forEach { user ->
        if(user.username in groupMembers) {
            user.socket.send(Frame.Text(getAllRecentChatsForUser(user.username)))
        }
    }
}

suspend fun createChat(participant1: String, participant2: String) {
    if (findChat(participant1, participant2) != null) return
    val newChat = NormalChat(
        participant1,
        participant2,
        listOf(),
        chatId = ObjectId().toString()
    )
    chats.insertOne(newChat)
}

suspend fun findChat(participant1: String, participant2: String): NormalChat? {
    val check1 = chats.findOne(NormalChat::participant1 eq participant1, NormalChat::participant2 eq participant2)
    val check2 = chats.findOne(NormalChat::participant1 eq participant2, NormalChat::participant2 eq participant1)
    return check1 ?: check2
}

suspend fun insertChatMessageToChat(chat: NormalChat, message: NormalChatMessage) {
    val messages = chat.messages + message.messageId!!
    chat.messages = messages
    chat.lastMessageTimestamp = message.timestamp
    chats.replaceOneById(chat.chatId!!, chat)
}

suspend fun getAllChatMessages(participant1: String, participant2: String): String {
    val chat = findChat(participant1, participant2)!!
    val messagesById = chat.messages
    val messagesAsNormalMessages = messagesById.map { messageId ->
        normalChatMessages.findOneById(messageId)!!
    }
    val messagesForThisChat = MessagesForThisChat(messagesAsNormalMessages)
    return gson.toJson(messagesForThisChat)
}

suspend fun getAllChatGroupMessages(groupId: String): String {
    val messages = chatGroupMessages.find(ChatGroupMessage::groupId eq groupId).toList()
    val messagesForThisGroup = MessagesForThisGroup(messages)
    return gson.toJson(messagesForThisGroup)
}

suspend fun getAllRecentChatsForUser(username: String): String {
    val user = users.findOne(User::username eq username)!!
    val chats1 = chats.find(NormalChat::participant1 eq username).toList()
    val chats2 = chats.find(NormalChat::participant2 eq username).toList()
    val chatsForUser = chats1 + chats2
    val chatsAsRecentChats = mutableListOf<ChatToSendAsRecent>()
    val lastReadTimestampsForChats = user.lastReadForChats
    chatsForUser.forEach { chat ->
        val chattingTo = if (chat.participant1 == username) {
            chat.participant2
        } else {
            chat.participant1
        }
        val lastReadForThisChat = lastReadTimestampsForChats.filter { it.contains("${chat.chatId}:") }[0].split(":")[1]
        var newMessages = 0
        var lastMessage = ""
        var lastMessageSender = ""
        val messagesFromMessageIdList = chat.messages.map { messageId ->
            normalChatMessages.findOneById(messageId)!!
        }
        if(messagesFromMessageIdList.isNotEmpty()) {
            lastMessage = messagesFromMessageIdList.last().message
            lastMessageSender = messagesFromMessageIdList.last().sender
        } else {
            lastMessage = "No message"
            lastMessageSender = "No message"
        }
        for (message in messagesFromMessageIdList.reversed()) {
            if(message.timestamp > lastReadForThisChat.toLong()) {
                newMessages++
            } else {
                break
            }
        }
        val chatAsRecentChat = ChatToSendAsRecent(
            chattingTo,
            newMessages,
            lastMessage,
            lastMessageSender,
            chat.lastMessageTimestamp!!
        )
        chatsAsRecentChats.add(chatAsRecentChat)
    }
    val chatsToSend = chatsAsRecentChats.toList()

    val groupsForUser = chatGroups.find(ChatGroup::members contains username).toList()
    val lastReadTimestampsForGroups = user.lastReadForGroups
    val groupsAsRecentGroups = mutableListOf<GroupToSendAsRecent>()
    groupsForUser.forEach { curGroup ->
        val name = curGroup.name
        val owner = curGroup.owner
        val groupId = curGroup.groupId
        val lastReadForThisGroup = lastReadTimestampsForGroups.filter { it.contains("${curGroup.groupId}:") }[0].split(":")[1]
        var newMessages = 0
        var lastMessage = ""
        var lastMessageSender = ""
        val messagesForThisGroup = chatGroupMessages.find(ChatGroupMessage::groupId eq groupId).toList()
        if(messagesForThisGroup.isNotEmpty()) {
            lastMessage = messagesForThisGroup.last().message
            lastMessageSender = messagesForThisGroup.last().sender
        } else {
            lastMessage = "No message"
            lastMessageSender = "No message"
        }
        for (message in messagesForThisGroup.reversed()) {
            if(message.timestamp > lastReadForThisGroup.toLong()) {
                newMessages++
            } else {
                break
            }
        }
        val groupAsRecentGroup = GroupToSendAsRecent(
            name,
            groupId,
            newMessages,
            lastMessage,
            lastMessageSender,
            curGroup.lastMessageTimestamp!!,
            owner
        )
        groupsAsRecentGroups.add(groupAsRecentGroup)
    }
    val groupsToSend = groupsAsRecentGroups.toList()

    val messagesToSendAsRecent = MessagesToSendAsRecent(chatsToSend, groupsToSend)

    return gson.toJson(messagesToSendAsRecent)
}

fun disconnectUnchattingUser(username: String) {
    if(onlineNotChattingUsers.containsKey(username)) {
        onlineNotChattingUsers.remove(username)
    }
}
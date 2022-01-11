package com.example.data


import com.example.data.models.*
import com.example.securityutil.checkHashForPassword
import com.example.securityutil.getHashWithSalt
import com.example.server.*
import com.example.util.Constants.CHAT_DEBUGGING
import com.example.util.Constants.MAX_CHAT_GROUP_NAME_LENGTH
import com.example.util.Constants.MAX_PASSWORD_LENGTH
import com.example.util.Constants.MAX_USERNAME_LENGTH
import com.example.util.Constants.MIN_CHAT_GROUP_NAME_LENGTH
import com.example.util.Constants.MIN_PASSWORD_LENGTH
import com.example.util.Constants.MIN_USERNAME_LENGTH
import com.example.util.Constants.NO_GROUP_ID
import io.ktor.http.cio.websocket.*
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo

val client = KMongo.createClient().coroutine
val database = client.getDatabase("AllAroundDatabase")

val users = database.getCollection<User>("users")
val chatGroups = database.getCollection<ChatGroup>("chat_groups")
val normalChatMessages = database.getCollection<NormalChatMessage>("normal_chat_messages")
val chatGroupMessages = database.getCollection<ChatGroupMessage>("chat_group_messages")
val chats = database.getCollection<NormalChat>("individual_chats")

suspend fun checkIfUserExists(username: String): Boolean {
    return users.findOne(User::username eq username) != null
}

suspend fun registerUser(username: String, password: String): Boolean {
    if(username.length < MIN_USERNAME_LENGTH || username.length > MAX_USERNAME_LENGTH) {
        return false
    }
    if(password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
        return false
    }
    val hashedPassword = getHashWithSalt(password)
    val user = User(
        username,
        hashedPassword,
        listOf(),
        listOf(),
        listOf()
    )
    return users.insertOne(user).wasAcknowledged()
}

suspend fun loginUser(username: String, passwordToCheck: String): Boolean {
    val actualPassword = users.findOne(User::username eq username)!!.password
    return checkHashForPassword(passwordToCheck, actualPassword)
}

suspend fun hasGroupWithThatName(username: String, groupName: String): Boolean {
    val ownedGroups = chatGroups.find(ChatGroup::owner eq username).toList()

    return ownedGroups.filter { it.name == groupName }.isNotEmpty()
}

suspend fun validateAndCreateChatGroup(username: String, name: String, members: List<String>): String {

    if(name.length < MIN_CHAT_GROUP_NAME_LENGTH || name.length > MAX_CHAT_GROUP_NAME_LENGTH) {
        return NO_GROUP_ID
    }

    val chatGroup = ChatGroup(
        name,
        username,
        listOf(username),
        members,
        System.currentTimeMillis()
    )
    chatGroups.insertOne(chatGroup)
    for (member in members) {
        val curUser = users.findOne(User::username eq member)!!
        val userLastReadForGroups = curUser.lastReadForGroups.toMutableList()
        userLastReadForGroups += "${chatGroup.groupId}:${System.currentTimeMillis()}"
        val updatedUser = curUser.copy(lastReadForGroups = userLastReadForGroups.toList())
        users.replaceOne(User::userId eq curUser.userId, updatedUser)
        if(onlineNotChattingUsers.containsKey(member)) {
            onlineNotChattingUsers[member]!!.socket.send(Frame.Text(getAllRecentChatsForUser(member)))
        }
    }

    return "${chatGroup.groupId},${chatGroup.name}"
}

suspend fun saveNormalChatMessage(message: NormalChatMessage) {
    createChat(message.sender, message.receiver)
    val chat = findChat(message.sender, message.receiver)!!
    val messageToInsert = message.copy(messageId = ObjectId().toString(), timestamp = System.currentTimeMillis())
    normalChatMessages.insertOne(messageToInsert)
    insertChatMessageToChat(chat, messageToInsert)
    println("$CHAT_DEBUGGING: Chat message saved")
}

suspend fun saveChatGroupMessage(message: ChatGroupMessage) {
    val group = chatGroups.findOneById(message.groupId) ?: return
    group.lastMessageTimestamp = message.timestamp
    chatGroups.replaceOneById(group.groupId, group)
    chatGroupMessages.insertOne(message.copy(messageId = ObjectId().toString()))
}


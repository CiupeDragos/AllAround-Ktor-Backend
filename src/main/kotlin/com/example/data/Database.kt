package com.example.data

import com.example.Security.checkHashForPassword
import com.example.Security.getHashWithSalt
import com.example.data.models.*
import com.example.server.findChat
import com.example.server.insertChatMessageToChat
import com.example.util.Constants.MAX_CHAT_GROUP_NAME_LENGTH
import com.example.util.Constants.MAX_PASSWORD_LENGTH
import com.example.util.Constants.MAX_USERNAME_LENGTH
import com.example.util.Constants.MIN_CHAT_GROUP_NAME_LENGTH
import com.example.util.Constants.MIN_PASSWORD_LENGTH
import com.example.util.Constants.MIN_USERNAME_LENGTH
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo

val client = KMongo.createClient().coroutine
val database = client.getDatabase("AllAroundDatabase")

val users = database.getCollection<User>("user")
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

suspend fun validateAndCreateChatGroup(username: String, name: String, members: List<String>): Boolean {

    if(name.length < MIN_CHAT_GROUP_NAME_LENGTH || name.length > MAX_CHAT_GROUP_NAME_LENGTH) {
        return false
    }

    val chatGroup = ChatGroup(
        name,
        username,
        listOf(username),
        members
    )
    return chatGroups.insertOne(chatGroup).wasAcknowledged()
}

suspend fun saveNormalChatMessage(message: NormalChatMessage) {
    val chat = findChat(message.sender, message.receiver) ?: return
    val messageToInsert = message.copy(messageId = ObjectId().toString())
    normalChatMessages.insertOne(messageToInsert)
    insertChatMessageToChat(chat, messageToInsert)

}

suspend fun saveChatGroupMessage(message: ChatGroupMessage) {
    val group = chatGroups.findOneById(message.groupId) ?: return
    group.lastMessageTimestamp = message.timestamp
    chatGroups.replaceOneById(group.groupId, group)
    chatGroupMessages.insertOne(message.copy(messageId = ObjectId().toString()))
}


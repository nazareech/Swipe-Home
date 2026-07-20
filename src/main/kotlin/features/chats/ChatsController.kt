package com.swipehome.features.chats

import com.swipehome.database.chats.Chats
import com.swipehome.database.chats.CreateChatRequest
import com.swipehome.database.chats.CreateChatResponse
import com.swipehome.database.chats.Messages
import com.swipehome.database.chats.SendMessageRequest
import com.swipehome.utils.TokenCheck
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ChatsController(private val call: ApplicationCall) {

    suspend fun createChat() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        val request = call.receive<CreateChatRequest>()

        if(currentUserId == request.id_owner){
            call.respond(HttpStatusCode.BadRequest, "You can't create a chat with yourself")
            return
        }

        val chatId = Chats.createOrGetChat(
            seekerId = currentUserId,
            ownerId = request.id_owner,
            propertyId = request.id_property,
        )

        call.respond(HttpStatusCode.OK, CreateChatResponse(chatId, "Second message"))
    }

    suspend fun getMyChats() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?: ""
        val currentUserId = TokenCheck.getIDByToken(token)
        if(currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }
        val chats = Chats.fetchChatForUser(currentUserId)
        call.respond(HttpStatusCode.OK, mapOf("chats" to chats))
    }

    suspend fun sendMessage(){
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        // Отримуємо ID чату з URL
        val chatId = call.parameters["id_chat"] ?.toIntOrNull()
        if(chatId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing cat Id")
            return
        }

        val request = call.receive<SendMessageRequest>()

        // Перевіряємо, чи має юзер доступ до цього чату
        val hasAccess = checkUserHasAccessToChat(chatId, currentUserId)
        if (!hasAccess) {
            call.respond(HttpStatusCode.Forbidden, "You are not a member of this chat")
            return
        }

        val saveMessage = Messages.insertMessage(
            chatId = chatId,
            senderId = currentUserId,
            textContent = request.content
        )

        if (saveMessage != null) {
            call.respond(HttpStatusCode.OK, saveMessage)
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Failed to save message")
        }
    }

    suspend fun getChatHistory() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        val catId = call.parameters["id_chat"]?.toIntOrNull()
        if(catId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing cat Id")
            return
        }

        // Знову перевіряємо доступ
        if(!checkUserHasAccessToChat(catId, currentUserId)) {
            call.respond(HttpStatusCode.Forbidden, "You are not a member of this chat")
            return
        }

        val history = Messages.fetchChatHistory(chatId = catId)
        call.respond(HttpStatusCode.OK, mapOf("messages" to history))
    }

    private fun checkUserHasAccessToChat(chatId: Int, userId: Int): Boolean {
        return transaction {
            val chatRow = Chats.selectAll().where { Chats.id_chat eq chatId }.singleOrNull()
            if (chatRow == null) return@transaction false

            // Користувач має доступ якщо він шукач АБО влесник у цьому конкретному чату
            chatRow[Chats.id_seeker] == userId || chatRow[Chats.id_owner] == userId
        }
    }
}
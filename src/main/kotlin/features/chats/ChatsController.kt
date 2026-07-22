package com.swipehome.features.chats

import com.swipehome.database.chats.ChatActions
import com.swipehome.database.chats.Chats
import com.swipehome.database.chats.CreateChatRequest
import com.swipehome.database.chats.CreateChatResponse
import com.swipehome.database.chats.EditMessageRequest
import com.swipehome.database.chats.Messages
import com.swipehome.database.chats.SendMessageRequest
import com.swipehome.database.chats.WsEvent
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

        val chatId = call.parameters["id_chat"]?.toIntOrNull()
        if(chatId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing cat Id")
            return
        }

        // Знову перевіряємо доступ
        if(!checkUserHasAccessToChat(chatId, currentUserId)) {
            call.respond(HttpStatusCode.Forbidden, "You are not a member of this chat")
            return
        }

        val history = Messages.fetchChatHistory(chatId = chatId)
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
    suspend fun editMessage() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        val messageId = call.parameters["id_message"]?.toIntOrNull()
        if(messageId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing message Id")
            return
        }

        val chatId = call.parameters["id_chat"]?.toIntOrNull()
        if(chatId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing cat Id")
            return
        }

        // Знову перевіряємо доступ
        if(!checkUserHasAccessToChat(chatId, currentUserId)) {
            call.respond(HttpStatusCode.Forbidden, "You are not a member of this chat")
            return
        }

        val request = call.receive<EditMessageRequest>()
        val newCallContent = request.new_content
        val success = Messages.editMessage(
            messageId = messageId,
            senderId = currentUserId,
            newText = newCallContent
        )

        if (success) {
            // Сповіщаємо WebSocket про зміну
            val event = WsEvent(
                action = ChatActions.EDIT,
                id_message = messageId,
                content = newCallContent // Відправляємо розшифрований текст, щоб одразу його висвітлити
            )
            ChatSessionManager.broadcastEvent(chatId, event)

            call.respond(HttpStatusCode.OK, mapOf("status" to "Message updated"))
        } else {
            // Помилка 403 (Forbidden) означає, що повідомлення немає, або юзер не є його автором
            call.respond(HttpStatusCode.Forbidden, "You can only edit your own messages")
        }
    }

    suspend fun deleteMessage() {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        val messageId = call.parameters["id_message"]?.toIntOrNull()
        if(messageId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing message Id")
            return
        }

        val chatId = call.parameters["id_chat"]?.toIntOrNull()
        if(chatId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing cat Id")
            return
        }

        // Знову перевіряємо доступ
        if(!checkUserHasAccessToChat(chatId, currentUserId)) {
            call.respond(HttpStatusCode.Forbidden, "You are not a member of this chat")
            return
        }

        val success = Messages.deleteMessage(messageId, chatId)

        if (success) {
            // Сповіщаємо WebSocket про зміну
            val event = WsEvent(
                action = ChatActions.DELETE,
                id_message = messageId,
            )
            ChatSessionManager.broadcastEvent(chatId, event)

            call.respond(HttpStatusCode.OK, mapOf("status" to "Message deleted"))
        } else {
            // Помилка 403 (Forbidden) означає, що повідомлення немає, або юзер не є його автором
            call.respond(HttpStatusCode.Forbidden, "You can only delete your own messages")
        }
    }
}
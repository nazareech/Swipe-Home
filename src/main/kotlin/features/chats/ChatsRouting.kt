package com.swipehome.features.chats

import com.swipehome.database.chats.GlobalConnectionManager
import com.swipehome.database.chats.Messages
import com.swipehome.utils.TokenCheck
import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json


fun Application.configureChatsRouting() {
    routing {
        post("/chats/create") {
            val controller = ChatsController(call)
            controller.createChat()
        }

        get("/chats/my") {
            val controller = ChatsController(call)
            controller.getMyChats()
        }

        post("/chats/{id_chat}/message") {
            val controller = ChatsController(call)
            controller.sendMessage()
        }

        get("/chats/{id_chat}/history") {
            val controller = ChatsController(call)
            controller.getChatHistory()
        }

        webSocket("/chats/{id_chat}/stream") {
            // Авторизація. У сокетах часто неможливо передати Headers
            // тому ми дозволяємо передавати токен як параметр URL: ?token=..
            val token = call.request.headers["Authorization"]?.replace("Bearer ", "")
                ?:call.request.queryParameters["token"]
                ?: ""

            val currentUserId = TokenCheck.getIDByToken(token)
            if (currentUserId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }

            val chatId = call.parameters["id_chat"]?.toIntOrNull()
            if (chatId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing chat id"))
                return@webSocket
            }

            // Юзер під'єднався до мережі
            GlobalConnectionManager.userConnected(currentUserId)

            // БЕЗПЕКА: Тут має бути твоя перевірка доступу (чи є юзер в цьому чаті).
            // Якщо використовуєш той самий метод checkUserHasAccessToChat, виклич його тут.
            // if (!checkUserHasAccessToChat(chatId, currentUserId)) { ... close() ... }

            // Додаємо користувача до кімнати
            val connection = Connection(this, currentUserId)
            ChatSessionManager.addConnection(chatId, connection)

            try {
                // Нескінченний цикл, який чекає на нові повідомлення від цього клієнта
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()

                        // Зберігаємо повідомлення в базу даних
                        val saveMessage = Messages.insertMessage(
                            chatId = chatId,
                            senderId = currentUserId,
                            textContent = receivedText
                        )

                        if (saveMessage != null) {
                            // Перетворюємо збережений об'єкт (з ID та часом) назад у JSON
                            val messageJson = Json.encodeToString(saveMessage)

                            // Розсилаємо це повідомлення ВСІМ, хто зараз в онлайн у цьому чаті
                            val activeConnections = ChatSessionManager.getConnection(chatId)
                            activeConnections.forEach { activeConnection ->
                                activeConnection.session.send(Frame.Text(messageJson))
                            }
                        }
                    }
                }
            }catch (e: Exception) {
                println("WevSocket disconected/error: ${e.localizedMessage}")
            } finally {
                // Юзер вийшов з мережі
                GlobalConnectionManager.userDisconnected(currentUserId)

                // Обов'язково видаляємо користувача з кімнати, коли він закрив застосунок
                ChatSessionManager.removeConnection(chatId = chatId, connection = connection)
            }
        }

    }
}
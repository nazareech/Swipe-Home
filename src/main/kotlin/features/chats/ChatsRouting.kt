package com.swipehome.features.chats

import com.swipehome.database.chats.ChatActions.*
import com.swipehome.database.chats.ClientWsMessage
import com.swipehome.database.chats.GlobalConnectionManager
import com.swipehome.database.chats.Messages
import com.swipehome.database.chats.WsEvent
import com.swipehome.utils.TokenCheck
import io.ktor.server.application.Application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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

        put("/chats/{id_chat}/messages/{message_id}") {
            val controller = ChatsController(call)
            controller.editMessage()
        }

        delete("/chats/{id_chat}/messages/{message_id}") {
            val controller = ChatsController(call)
            controller.deleteMessage()
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

                        // Парсимо JSON, який надіслав клієнт
                        val clientMessage = try{
                            Json.decodeFromString<ClientWsMessage>(receivedText)
                        } catch (e: Exception){
                            print("Invalid JSON received from client: $receivedText")
                            continue // Пропускаємо, якщо клієнт надіслав щось незрозуміле
                        }

                        when (clientMessage.action){
                            // Надіслав повідомлення
                            SEND_MESSAGE -> {
                                if(clientMessage.content.isNullOrBlank()) continue

                                // Зберігаємо повідомлення в базу даних
                                val saveMessage = Messages.insertMessage(
                                    chatId = chatId,
                                    senderId = currentUserId,
                                    textContent = clientMessage.content
                                )

                                if (saveMessage != null) {
                                    val event = WsEvent(
                                        action = NEW,
                                        id_message = saveMessage.id_message,
                                        messages = saveMessage
                                    )
                                    ChatSessionManager.broadcastEvent(chatId = chatId, event = event)
                                }
                            }
                            // Почав друкувати
                            TYPING -> {
                                val event = WsEvent(
                                    action = TYPING,
                                    id_sender = currentUserId // Хто друкує
                                )
                                // Просто пересилаємо подію без запису в БД
                                ChatSessionManager.broadcastEvent(chatId = chatId, event = event)
                            }

                            // Перестав друкувати
                            STOP_TYPING -> {
                                val event = WsEvent(
                                    action = STOP_TYPING,
                                    id_sender = currentUserId
                                )
                                ChatSessionManager.broadcastEvent(chatId = chatId, event = event)
                            }

                            NEW -> TODO()
                            EDIT -> TODO()
                            DELETE -> TODO()
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
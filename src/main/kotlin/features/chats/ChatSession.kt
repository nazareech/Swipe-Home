package com.swipehome.features.chats

import com.swipehome.database.chats.WsEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

// Клас, що описує одне активне підключення
class Connection(val session: DefaultWebSocketServerSession, val userId: Int)

object ChatSessionManager {
    // Словник: ключ - id_chat, значення - список активних підключень у цьому чаті
    // Використовуємо потокобезпечні колекції, бо юзери можуть підключатися одночасно
    private val activeChats = ConcurrentHashMap<Int, MutableSet<Connection>>()

    // Додаємо користувача в кімнаті
    fun addConnection(chatId: Int, connection: Connection) {
        val connections = activeChats.computeIfAbsent(chatId) {
            Collections.synchronizedSet(LinkedHashSet())
        }
        connections.add(connection)
    }
    // Видаляємо користувача з кімнати (коли він закриває чат або пропадає інтернет)
    fun removeConnection(chatId: Int, connection: Connection) {
        val connections = activeChats[chatId]
        connections?.remove(connection)
        if (connections?.isEmpty() == true) {
            activeChats.remove(chatId) // Очищуємо пам'ять, якщо в чаті нікого немає
        }
    }

    // Отримуємо всіх, хто зараз онлайн у конкретному чаті
    fun getConnection(chatId: Int): Set<Connection> {
        return activeChats[chatId] ?: emptySet()
    }

    // Метод для миттєвої розсилки подій
    suspend fun broadcastEvent(chatId: Int, event: WsEvent) {
        val connections = activeChats[chatId] ?: return

        // Перетворюємо об'єкт подій на JSON-рядок
        val jsonMessage = Json.encodeToString(event)

        // Відправляємо кожному підключеному клієнту в цій кімнаті
        connections.forEach { connection ->
            try{
                connection.session.send(Frame.Text(jsonMessage))
            } catch (e: Exception){
                println("Failed to send event to user ${connection.userId}: ${e.localizedMessage}")
            }
        }
    }
}
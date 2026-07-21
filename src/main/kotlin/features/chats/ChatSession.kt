package com.swipehome.features.chats

import io.ktor.server.websocket.DefaultWebSocketServerSession
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
}
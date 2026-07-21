package com.swipehome.database.chats

import com.swipehome.database.users.Users
import java.util.concurrent.ConcurrentHashMap

object GlobalConnectionManager {
    // Зберігаємо ID користувача та кількість його активних з'єднань
    // (бо він може відкрити застосунок і з телефону, і з планшета одночасно)
    private val onlineUsers = ConcurrentHashMap<Int, Int>()

    fun userConnected(userId: Int){
        val currentConnections = onlineUsers.getOrDefault(userId, 0)
        onlineUsers[userId] = currentConnections + 1
    }

    fun userDisconnected(userId: Int){
        val currentConnections = onlineUsers.getOrDefault(userId, 0)
        if (currentConnections <= 1) {
            // Якщо це було останнє з'єднання, юзер повністю вийшов з мережі
            onlineUsers.remove(userId)

            // Оновлюємо "last_seen" у базі даних
            Users.updateLastSeen(userId)
        } else {
            onlineUsers[userId] = currentConnections - 1
        }
    }

    // Перевірка, чи користувач зараз онлайн
    fun isUserOnline(userId: Int): Boolean{
        return onlineUsers.containsKey(userId)
    }
}
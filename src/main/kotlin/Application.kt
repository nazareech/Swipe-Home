package com.swipehome

import com.swipehome.features.chats.configureChatsRouting
import com.swipehome.features.images.configureImagesRouting
import com.swipehome.features.login.configureLoginRouting
import com.swipehome.features.properties.configurePropertiesRouting
import com.swipehome.features.register.configureRegisterRouting
import com.swipehome.features.swipes.configureSwipesRouting
import io.ktor.server.application.Application

fun Application.rootModule() {

    // налаштування роутингу
    configureRouting()

    configureLoginRouting()

    configureRegisterRouting()

    // налаштування сереалізації (JSON)
    configureSerialization()

    // ініціалізація бази даних
    configureDatabases()

    // налаштування роутингу для пошуку карток по категоріях
    configurePropertiesRouting()

    // Налаштування роутингу для Свайпів
    configureSwipesRouting()

    // Налаштування роутингу для завантаження та роздачі зображень оголошень
    configureImagesRouting()

    // Налаштування роутингу для чатів
    configureChatsRouting()
}

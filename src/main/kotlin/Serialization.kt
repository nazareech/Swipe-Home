package com.swipehome

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.websocket.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds // Сервер кожні 15 секунд перевіряє, чи живий клієнт
        timeout = 15.seconds    // Скільки часу чекати відповідь
        maxFrameSize = Long.MAX_VALUE       // Максимальний розмір повідомлення
        masking = false
    }
}
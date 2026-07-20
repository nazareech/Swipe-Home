package com.swipehome.features.chats

import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

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
    }
}
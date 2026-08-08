package com.swipehome.features.autorization.register

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRegisterRouting() {
    routing {
        post("/register"){
            val registerController = RegisterController(call)
            registerController.registerNewUser()
        }
    }
}
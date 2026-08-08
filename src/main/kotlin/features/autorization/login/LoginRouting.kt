package com.swipehome.features.autorization.login

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureLoginRouting() {
    routing {
        post("/login"){
            val loginController = LoginController(call)
            loginController.performLogin()

        }
    }

    routing {
        post("/logout"){
            val loginController = LoginController(call)
            loginController.performLogout()
        }
    }
}
package com.swipehome.features.autorization

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureAuthRouting() {
    routing {
        post("/auth/forgot-password") {
            val controller = AuthController(call)
            controller.forgotPassword()
        }

        post("/auth/verify-code") {
            val controller = AuthController(call)
        }

        post("/auth/reset-password") {
            val controller = AuthController(call)
            controller.resetPassword()
        }
    }
}
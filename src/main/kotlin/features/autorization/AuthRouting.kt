package com.swipehome.features.autorization

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureAuthRouting() {
    routing {

        //================== Реєстрація ==================
        post("/register"){
            val registerController = AuthController(call)
            registerController.registerNewUser()
        }

        //================== Логування ==================
        post("/login"){
            val loginController = AuthController(call)
            loginController.performLogin()
        }

        post("/logout"){
            val loginController = AuthController(call)
            loginController.performLogout()
        }

        //================== Відновлення пароля ==================
        post("/auth/forgot-password") {
            val controller = AuthController(call)
            controller.forgotPassword()
        }

        post("/auth/verify-code") {
            val controller = AuthController(call)
            controller.verifyResetCode()
        }

        post("/auth/reset-password") {
            val controller = AuthController(call)
            controller.resetPassword()
        }
    }
}
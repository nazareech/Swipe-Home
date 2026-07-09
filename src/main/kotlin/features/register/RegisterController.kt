package com.swipehome.features.register

import com.swipehome.database.tokens.TokenDTO
import com.swipehome.database.tokens.Tokens
import com.swipehome.database.users.UserDTO
import com.swipehome.database.users.Users
import com.swipehome.utils.isValidEmail
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import java.time.OffsetDateTime
import java.util.UUID

class RegisterController(val call: ApplicationCall) {

    suspend fun registerNewUser() {
        val registerReceivedRemote = call.receive<RegisterReceivedRemote>()

        if(!registerReceivedRemote.email.isValidEmail()){
            call.respond(HttpStatusCode.BadRequest, "Invalid email")
            return
        }

        val existUser = Users.fetchUserByLogin(registerReceivedRemote.login)
        if(existUser != null) {
            call.respond(HttpStatusCode.Conflict, "User already exist")
            return
        }

        // 1. Створюємо користувача та ОДРАЗУ отримуємо його новий ID
        val newUserId = Users.insertAndGetId(
            UserDTO(
                login = registerReceivedRemote.login,
                username = registerReceivedRemote.username,
                password = registerReceivedRemote.password,
                email = registerReceivedRemote.email,
                phone = registerReceivedRemote.phone,
                is_verified_owner = registerReceivedRemote.is_verified_owner,
                is_admin = registerReceivedRemote.is_admin
            )
        )

        // 2. Створюємо токен для цього ID
        val token = UUID.randomUUID().toString()
        val expirationDate = OffsetDateTime.now().plusDays(30).toString()

        Tokens.insert(
            TokenDTO(
                login = registerReceivedRemote.login,
                token = token,
                id_user = newUserId, // Використовуємо ID, який повернула база
                expires_at = expirationDate
            )
        )

        call.respond(HttpStatusCode.OK, RegisterResponseRemote(token = token))
    }
}

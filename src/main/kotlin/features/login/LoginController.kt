package com.swipehome.features.login


import com.swipehome.database.tokens.TokenDTO
import com.swipehome.database.tokens.Tokens
import com.swipehome.database.users.Users
import com.swipehome.features.register.RegisterResponseRemote
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import java.time.OffsetDateTime
import java.util.UUID

class LoginController(private val call: ApplicationCall) {

    suspend fun performLogin(){
        val receive = call.receive<LoginReceiveRemote>()

        // Звертаємося до реальної бази даних
        val userDTO = Users.fetchUserByLogin(receive.login)

        // 1. Якщо юзера немає
        if(userDTO == null) {
            call.respond(HttpStatusCode.BadRequest, "User not found")
            return
        }
        // 2. Перевірка пароля
        if (userDTO.password == receive.password) {
            val token = UUID.randomUUID().toString()

            // Безпечно дістаємо ID користувача (Елвіс-оператор)
            val userId = userDTO.id_user ?: return call
                .respond(HttpStatusCode.InternalServerError,
                        "Database error: User ID is null")

            // Створюємо дадту
            val expirationDate = OffsetDateTime.now().plusDays(30).toString()

            Tokens.insert(
                TokenDTO(
                    login = userDTO.login,
                    token = token,
                    id_user = userId,
                    expires_at = expirationDate
                )
            )
            call.respond(HttpStatusCode.OK, RegisterResponseRemote(token = token))
        } else {
            call.respond(HttpStatusCode.BadRequest, "Invalid password")
        }
    }
}
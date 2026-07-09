package com.swipehome.features.swipes

import com.swipehome.database.swipes.SwipeDTO
import com.swipehome.database.swipes.Swipes
import com.swipehome.utils.TokenCheck
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond

class SwipesController(private val call: ApplicationCall) {

    suspend fun perfomSwipe(){
        // Отримуємо токен
        val authHeader = call.request.headers["Authorization"]
        val token = authHeader?.removePrefix("Bearer ") ?: ""

        val currentUserId = TokenCheck.getIDByToken(token)

        if (currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        val request = call.receive<SwipeRequest>()

        // Перевіряємо, чи дія дійсно "left" або "right"
        if (request.action != "left" && request.action != "right") {
            call.respond(HttpStatusCode.BadRequest, "Action must be 'left' or 'right'")
            return
        }

        // Створюємо DTO (id)_swipe та create_at генеруються самі)
        val swipeDTO = SwipeDTO(
            id_user = currentUserId,
            id_property = request.id_property,
            action = request.action
        )

        // Записуємо в базу даних
        Swipes.insert(swipeDTO)

        // Можна також реалізувати логіку "Метчу" тут, якщо action == "right"
        // (наприклад, перевірити, чи власник квартири теж лайкнув цього юзера)

        call.respond(HttpStatusCode.OK, "Property ${request.id_property} swiped ${request.action}!")

    }
}
package com.swipehome.features.properties

import com.swipehome.database.properties.Properties
import com.swipehome.database.properties.PropertyDTO
import com.swipehome.database.properties.models.CreatePropertyRequest
import com.swipehome.database.properties.models.FetchPropertyResponse
import com.swipehome.database.properties.models.PropertyResponse
import com.swipehome.database.properties.toPropertyDTO
import com.swipehome.utils.TokenCheck
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond

class PropertiesController(private val call: ApplicationCall) {

    suspend fun getProperties() {
        val authHeader = call.request.headers["Authorization"]
        val token = authHeader?.removePrefix("Bearer ") ?: ""

        val validUserId = TokenCheck.getIDByToken(token) // Припускаю, що ця функція є у твоєму TokenCheck

        if (validUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        val request = call.receive<FetchPropertyRequest>()

        // виклик методу ще не переглянутих оголошень
        val propertyDTOs = Properties.fetchUnswipedProperties(request, validUserId)

        // Трансформуємо внутрішні PropertyDTO у відповідь для клієнта PropertyResponse
        val responseList = propertyDTOs.map { dto -> rowToDTO(dto) }

        // Повертаємо обгорнутий об'єкт списку
        call.respond(HttpStatusCode.OK, FetchPropertyResponse(responseList))
    }

    suspend fun setProperties() {
        val authHeader = call.request.headers["Authorization"]
        val token = authHeader?.removePrefix("Bearer ") ?: ""

        // Отримуємо ID користувача
        val currentUserId = TokenCheck.getIDByToken(token)
        // Перевіряємо, чи є він власником
        val isOwner = TokenCheck.isTokenOwner(token)

        if (currentUserId == null || !isOwner) {
            call.respond(HttpStatusCode.Unauthorized, "The user does not have owner access")
            return
        }

        // Отримуємо чисті дані про квартиру від клієнта
        val request = call.receive<CreatePropertyRequest>()

        // Викликаємо наш безпечний маппер і передаємо туди надійний ID з токена
        val propertyDTO = request.toPropertyDTO(safeOwnerId = currentUserId)

        // Зберігаємо в базу (метод Properties.insert треба буде написати)
        Properties.insert(propertyDTO)

        // Обов'язково відповідаємо клієнту!
        call.respond(HttpStatusCode.Created, "Property successfully created!")
    }

    suspend fun getMatches() {
        //перевіряємо токен
        val authHeader = call.request.headers["Authorization"]
        val token = authHeader?.removePrefix("Bearer ") ?: ""

        val currentUserId = TokenCheck.getIDByToken(token) // Припускаю, що ця функція є у твоєму TokenCheck

        if (currentUserId == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        val matchedDTOs = Properties.fetchMatchedProperties(currentUserId)

        val responseList = matchedDTOs.map { dto ->
            rowToDTO(dto)
        }
        call.respond(HttpStatusCode.OK, FetchPropertyResponse(responseList))
    }

    private fun rowToDTO(dto: PropertyDTO): PropertyResponse {
        return PropertyResponse(
            id_property = dto.id_property ?: 0,
            id_owner = dto.id_owner,
            title = dto.title,
            description = dto.description,
            localization = dto.localization,
            price = dto.price,
            area = dto.area,
            rooms = dto.rooms,
            category = dto.category,
            subcategory = dto.subcategory,
            parking = dto.parking,
            pets_allowed = dto.pets_allowed,
            elevator = dto.elevator,
            furniture = dto.furniture,
            building_type = dto.building_type,
            status = dto.status,
            creates_at = dto.created_at ?: ""
        )
    }
}
package com.swipehome.features.properties

import com.swipehome.database.properties.Properties
import com.swipehome.database.properties.models.PropertyStatus
import com.swipehome.utils.TokenCheck
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

fun Application.configurePropertiesRouting(){

    routing {
        post("/properties/fetch") {
            val propertiesController = PropertiesController(call)
            propertiesController.getProperties()
        }

        post("/properties/create") {
            val propertiesController = PropertiesController(call)
            propertiesController.setProperties()
        }

        post("/properties/matches") {
            val propertiesController = PropertiesController(call)
            propertiesController.getMatches()
        }

        // Зміна статусу (PATCH для часткового оновлення, як от поля)
        patch("/properties/{id}/status") {
            val controller = PropertiesController(call)
            controller.changeStatus()
        }

        // Редагування квартири (PUT)
        put ("/properties/{id}"){
            val controller = PropertiesController(call)
            controller.editProperty()
        }

        // Метод DELETE, який робить під капотом Soft Delete
        delete("/properties/{id}"){
            val controller = PropertiesController(call)
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUserId = TokenCheck.getIDByToken(token)

            if (currentUserId != null ) {
                val propertyId  = call.parameters["id"]?.toIntOrNull()
                if (propertyId != null) {
                    val success = Properties.changePropertyStatus(propertyId, currentUserId, PropertyStatus.DELETED)
                    if (success) call.respond(HttpStatusCode.OK, mapOf("message" to "Property deleted"))
                    else call.respond(HttpStatusCode.Forbidden, "Access denied")
                }
            }
        }
    }
}
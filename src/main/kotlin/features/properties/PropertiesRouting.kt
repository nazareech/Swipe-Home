package com.swipehome.features.properties

import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.routing.post
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
    }
}
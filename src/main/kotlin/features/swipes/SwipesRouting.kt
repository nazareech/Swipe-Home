package com.swipehome.features.swipes

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureSwipesRouting() {
    routing {
        post("/properties/swipe") {
            val swipeController = SwipesController(call)
            swipeController.perfomSwipe()
        }
    }
}
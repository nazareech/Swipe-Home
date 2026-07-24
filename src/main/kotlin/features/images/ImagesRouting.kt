package com.swipehome.features.images

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import java.io.File

fun Application.configureImagesRouting() {
    routing {
        // Ендпоінт для завантаження файлів На сервер
        post("/properties/images/upload") {
            val controller = ImagesController(call)
            controller.uploadImage()
        }

        delete("/properties/images/{id_image}") {
            val controller = ImagesController(call)
            controller.deleteImage()
        }

        put("/properties/images/{id_property}/{id_new_image}") {
            val controller = ImagesController(call)
            controller.changeMainImage()
        }

        // Ендпоінт для роздачі фотографій з сервера клієнтам
        // Тепер за посиланням //http:localhost:8080/uploads/назва_файлу.jpg буде віддаватися картинка
        staticFiles("/uploads", File("uploads"))
    }
}
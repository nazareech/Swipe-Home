package com.swipehome.features.images

import com.swipehome.database.properties.Properties
import com.swipehome.utils.TokenCheck
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond

import com.swipehome.database.properties.PropertyImageDTO
import com.swipehome.database.properties.PropertyImages
import com.swipehome.database.properties.models.UpdatePropertyMainImageRequest
import com.swipehome.database.properties.models.UpdatePropertyStatusRequest
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ImagesController(private val call: ApplicationCall) {

    suspend fun uploadImage(){
        val authorization = call.request.headers["Authorization"]
        val token = authorization?.removePrefix("Bearer") ?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null){
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        // Отримуємо мультипарт дані
        val multipartData = call.receiveMultipart()

        var propertyId: Int? = null
        var isMain = false
        var savedFilesName: String? = null

        // Перебираємо всі частини запиту (текст і файли)
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    // зчитуємо текстові поля
                    when (part.name) {
                        "id_property" -> propertyId = part.value.toIntOrNull()
                        "is_main" -> isMain = part.value.toBooleanStrictOrNull() ?: false
                    }
                }

                is PartData.FileItem -> {

                    // Генеруємо унікальне і'мя файлу, щоб уникнути конфліктів
                    val originalName = part.originalFileName ?: "image.jpg"
                    val extension = File(originalName).extension.ifEmpty { "jpg" }
                    val uniqueName = "${UUID.randomUUID()}.$extension"
                    // Зберігаємо в змінну відносний шлях, щоб клієнт міг його завантажити
                    savedFilesName = "/application-number-$propertyId/$uniqueName"

                    // Зберігаємо файл у папку "uploads" на сервері
                    val uploadDir = File("uploads")
                    if (!uploadDir.exists()) uploadDir.mkdirs() // Створюємо папку якщо її ще не існує

                    // Створюємо підпапку для конкретного оголошення (Використовуємо конструкцію File(parent, child)
                    val ownDir = File("${uploadDir.absolutePath}/application-number-$propertyId")
                    if (!ownDir.exists()) ownDir.mkdirs()

                    // Створюємо кінцевий файл
                    val file = File(ownDir, uniqueName)

                    // Записуємо байти у файл (безпечно для пам'ять через Dispotchers.IO)
                    withContext(Dispatchers.IO){
                        part.streamProvider().use { input ->
                            file.outputStream().buffered().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
                else -> Unit
            }
            part.dispose()  // Обов'язково звільняємо пам'ять
        }

        //Перевіряємо, чи отримали все необхідне
        if (propertyId != null && savedFilesName != null) {

            // Перевірка, чи належить ця квартира поточному користувачеві
            val property = Properties.fetchPropertiesByID(propertyId!!)
            if (property == null || property.id_owner != currentUserId) {
                File("uploads", savedFilesName!!).delete()
                call.respond(HttpStatusCode.Forbidden, "You don`t have permission to upload images for this property")
                return
            }

            val dto = PropertyImageDTO(
                id_property = propertyId!!,
                image_url = savedFilesName!!, // Зберігаємо тільки ім'я файлу
                is_main = isMain
            )

            PropertyImages.insert(dto)
            call.respond(HttpStatusCode.Created, "Image $savedFilesName successfully uploaded")
        } else{
            call.respond(HttpStatusCode.BadRequest, "Missing id_property or image file")
        }
    }

    suspend fun deleteImage(){
        val authorization = call.request.headers["Authorization"]
        val token = authorization?.removePrefix("Bearer") ?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null){
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }

        // Отримуємо ID картинки з URL (/properties/images/5)
        val imagesId = call.parameters["id_image"]?.toIntOrNull()
        if(imagesId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing or invalid id_image")
            return
        }

        // Шукаємо інфу про картинку в базі даних
        val imageInfo = PropertyImages.getImageInfo(imagesId)
        if(imageInfo == null) {
            call.respond(HttpStatusCode.BadRequest, "Image not found in database")
            return
        }

        val propertyId = imageInfo.first
        val imageUrl = imageInfo.second

        // Перевіряємо, чи цей корисувач власник квартири
        // Використовуємо готовий метод з Properties
        val property = Properties.fetchPropertiesByID(propertyId)

        if(property == null || property.id_owner != currentUserId) {
            call.respond(HttpStatusCode.BadRequest, "You don`t have permission to delete this image")
            return
        }

        // Видаляємо файл з сервера
        val relativePath = imageUrl.removePrefix("/")
        val fileToDelete = File("uploads", relativePath)

        if (fileToDelete.exists()) {
            val isDeleted = fileToDelete.delete()
            if(!isDeleted) {
                // Якщо з якоїсь причини файл не видалився (наприклад, заблокований системою)
                println("Warning: Could not delete physical file: ${fileToDelete.absolutePath}")
            }
        }

        val dbDeleted = PropertyImages.deleteImage(imagesId)

        if (dbDeleted) {
           call.respond(HttpStatusCode.OK, "Image deleted successfully")
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Failed to delete image from database")
        }
    }

    suspend fun changeMainImage(){
        val authorization = call.request.headers["Authorization"]
        val token = authorization?.removePrefix("Bearer ") ?: ""
        val currentUserId = TokenCheck.getIDByToken(token)

        if(currentUserId == null){
            call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            return
        }
        val propertyId = call.parameters["id_property"]?.toIntOrNull() ?: return call.respond(HttpStatusCode.BadRequest, "Invalid property ID")
        val newImageId = call.parameters["id_new_image"]?.toIntOrNull()

        if(newImageId == null || propertyId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing or invalid property ID or new image ID")
            return
        }

        val property = Properties.fetchPropertiesByID(propertyId)
        if (property == null || property.id_owner != currentUserId) {
            call.respond(HttpStatusCode.BadRequest, "You don`t have permission to change this image")
            return
        }

        val success = PropertyImages.changeStatusImage(propertyId, newImageId)

        if (success) {
            call.respond(HttpStatusCode.OK, mapOf( "message" to "Main image changed successfully"))
        } else  {
            call.respond(HttpStatusCode.Forbidden, "Image not found or you are not the owner")
        }
    }
}
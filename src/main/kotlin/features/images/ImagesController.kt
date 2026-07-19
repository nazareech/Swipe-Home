package com.swipehome.features.images

import com.swipehome.utils.TokenCheck
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond

import com.swipehome.database.properties.PropertyImageDTO
import com.swipehome.database.properties.PropertyImages
import io.ktor.http.content.streamProvider
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
                    savedFilesName = "/application-number-$propertyId/$uniqueName"

                    // Зберігаємо файл у папку "uploads" на сервері
                    val uploadDir = File("uploads")
                    if (!uploadDir.exists()) uploadDir.mkdirs() // Створюємо папку якщо її ще не існує

                    val ownDir = File("${uploadDir.absolutePath}/application-number-$propertyId")
                    if (!ownDir.exists()) ownDir.mkdirs()

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
}
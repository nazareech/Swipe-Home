package com.swipehome.database.properties

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// DTO для передачі даних
data class PropertyImageDTO(
    val id_image: Int? = null,
    val id_property: Int,
    val image_url: String,
    val is_main: Boolean
)

// сама таблиця
object PropertyImages : Table("property_image") {
    val id_image = integer("id_image").autoIncrement()
    val id_property = integer("id_property").references(Properties.id_property)
    val image_url = varchar("image_url", 255)
    val is_main = bool("is_main")

    override val primaryKey = PrimaryKey(id_image)

    fun insert(imageDTO: PropertyImageDTO){
        transaction {
            PropertyImages.insert {
                it[id_property] = imageDTO.id_property
                it[image_url] = imageDTO.image_url
                it[is_main] = imageDTO.is_main
            }
        }
    }

    //
    fun getImageInfo(serchImageId: Int): Pair<Int, String>? {
        return transaction {
            PropertyImages.selectAll()
                .where{ id_image eq serchImageId }
                // Повертаємо пару (id_property, image_url)
                .map { it[id_property] to it[image_url] }
                .singleOrNull()
        }
    }

    // Видаляємо запис з бази даних
    fun deleteImage(imageToDelete: Int): Boolean{
        return transaction {
            // deletWhere
            val deleteRows = PropertyImages.deleteWhere { id_image eq imageToDelete }
            deleteRows > 0
        }
    }




    // Отримати всі фото для квартири
    fun fetchImagesForProperty(searchPropertyId: Int): List<String>{
        return transaction {
            PropertyImages
                .selectAll()
                .where { id_property eq searchPropertyId}
                .map { it[image_url]}
        }
    }
}
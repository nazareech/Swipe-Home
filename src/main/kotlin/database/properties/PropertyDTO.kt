package com.swipehome.database.properties

import com.swipehome.database.properties.models.CreatePropertyRequest
import com.swipehome.database.properties.models.CreatePropertyResponse
import kotlinx.serialization.Serializable

@Serializable
data class PropertyDTO( // Додано слово data
    val id_property: Int? = null, // БД згенерує сама
    val id_owner: Int,            // Беремо з токена, а не з запиту
    val title: String,
    val description: String,
    val localization: String,
    val price: Double,
    val area: Double,
    val rooms: Int,
    val category: String,
    val subcategory: String,
    val parking: String,
    val pets_allowed: Boolean,
    val elevator: Boolean,
    val furniture: Boolean,
    val building_type: String,
    val status: String,
    val images_map: Map<String, Boolean>? = null,  // Список зображень
    val created_at: String? = null // БД згенерує сама
)
fun CreatePropertyRequest.toPropertyDTO(safeOwnerId: Int): PropertyDTO {
    return PropertyDTO(
        id_property = null,         // БД згенерує сама
        id_owner = safeOwnerId,     // БЕЗПЕЧНИЙ ID, який ми передамо з контролера
        title = this.title,
        description = this.description,
        localization = this.localization,
        price = this.price,
        area = this.area,
        rooms = this.rooms,
        category = this.category,
        subcategory = this.subcategory,
        parking = this.parking,
        pets_allowed = this.pets_allowed,
        elevator = this.elevator,
        furniture = this.furniture,
        building_type = this.building_type,
        status = this.status,
        created_at = null           // БД згенерує сама
    )
}

fun PropertyDTO.toCreatePropertyResponse(): CreatePropertyResponse {
    return CreatePropertyResponse(
        // Тут ми вже БЕРЕМО згенеровані дані з DTO
        // Використовуємо Елвіс-оператор (?: 0) про всяк випадок, якщо id раптом null
        id_property = this.id_property ?: 0,
        id_owner = this.id_owner, // Тут safeOwnerId вже не потрібен, бо він є всередині DTO
        title = this.title,
        description = this.description,
        localization = this.localization,
        price = this.price,
        area = this.area,
        rooms = this.rooms,
        category = this.category,
        subcategory = this.subcategory,
        parking = this.parking,
        pets_allowed = this.pets_allowed,
        elevator = this.elevator,
        furniture = this.furniture,
        building_type = this.building_type,
        status = this.status,
        images_map = this.images_map,
        creates_at = this.created_at ?: "" // Повертаємо час створення
    )
}
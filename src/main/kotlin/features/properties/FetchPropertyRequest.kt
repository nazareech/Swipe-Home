package com.swipehome.features.properties

import kotlinx.serialization.Serializable

@Serializable
data class FetchPropertyRequest (
    val limit: Int = 10, // Завантаження по 10 карток
    val offset: Int = 0, // Зсув для наступної партії

    // Базові фільтри
    val category: String? = null,       // Квартира, кімната, будинок
    val subcategory: String? = null,    // Винайм, продаж
    val localization: String? = null,   // Місто / район

    // Діапазони
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val areaMin: Double? = null,
    val areaMax: Double? = null,
    val roomsMin: Int? = null,
    val roomsMax: Int? = null,

    // Специфічні фільтри (з документації)
    val parking: String? = null,        // Брак, в гаражі, на вулиці, під охороною
    val buildingType: String? = null,   // Новобудова, вторинний ринок тощо
    val petsAllowed: Boolean? = null,
    val elevator: Boolean? = null,
    val furniture: Boolean? = null
)
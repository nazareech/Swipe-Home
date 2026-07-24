package com.swipehome.database.properties.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PropertyStatus {
@SerialName("active") ACTIVE,
@SerialName("hidden") HIDDEN,
@SerialName("deleted") DELETED

}

@Serializable
data class UpdatePropertyStatusRequest(
    val status: PropertyStatus // "active", "hidden", "deleted"
)

@Serializable
data class EditPropertyRequest (
    val title: String?,
    val description: String?,
    val price: Double?,
    val localization: String?,
    val area: Double?,
    val rooms: Int?,
    val category: String?,
    val subcategory: String?,
    val parking: String?,
    val pets_allowed: Boolean?,
    val elevator: Boolean?,
    val furniture: Boolean?,
    val building_type: String?,
    val status: String?
)
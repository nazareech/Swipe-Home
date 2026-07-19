package com.swipehome.features.images

import com.swipehome.database.properties.PropertyImageDTO
import kotlinx.serialization.Serializable

@Serializable
data class FetchPropertyImageResponse(
    val properties: List<PropertyImageResponse>,
)

@Serializable
data class FetchPropertyImageRequest(
    val searchQuery: String,
)
@Serializable
data class PropertyImageResponse(
    val id_image: Int,
    val id_property: Int,
    val image_url: String,
    val is_main: Boolean
)
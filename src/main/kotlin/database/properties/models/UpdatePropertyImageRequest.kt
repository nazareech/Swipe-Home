package com.swipehome.database.properties.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePropertyMainImageRequest (
    val id_image: Int? = null,
    val id_property: Int,
    val image_url: String,
    val is_main: Boolean
)

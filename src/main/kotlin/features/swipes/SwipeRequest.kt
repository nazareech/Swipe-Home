package com.swipehome.features.swipes

import kotlinx.serialization.Serializable

@Serializable
data class SwipeRequest(
    val id_property: Int,
    val action: String // left - right
)

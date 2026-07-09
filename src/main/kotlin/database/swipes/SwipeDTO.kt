package com.swipehome.database.swipes

data class SwipeDTO(
    val id_swipe: Int? = null,
    val id_user: Int,
    val id_property: Int,
    val action: String,
    val created_at: String? = null
)

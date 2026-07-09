package com.swipehome.database.tokens

import kotlinx.serialization.Serializable

@Serializable
class TokenDTO(
    val id_token: Int? = null,
    val login: String,
    val token: String,
    val id_user: Int,
    val expires_at: String,
)

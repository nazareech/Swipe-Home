package com.swipehome.database.users

import kotlinx.serialization.Serializable

@Serializable
class UserDTO(
    val id_user: Int? = null,
    val login: String,
    val username: String,
    val email: String,
    val password: String,
    val phone: String,
    val is_verified_owner: Boolean,
    val is_admin: Boolean
    )
package com.swipehome.features.register

import kotlinx.serialization.Serializable

@Serializable
data class RegisterReceivedRemote(
    val login: String,
    val username: String,
    val email: String,
    val password: String,
    val phone: String,
    val is_verified_owner: Boolean,
    val is_admin: Boolean
)


@Serializable
data class RegisterResponseRemote(
    val token: String
)
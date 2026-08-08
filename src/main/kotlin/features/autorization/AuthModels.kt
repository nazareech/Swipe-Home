package com.swipehome.features.autorization

import kotlinx.serialization.Serializable

@Serializable
data class ForgotPasswordRequest(
    val email: String
)
@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: Int, // 6-значний код
    val new_password: String
)
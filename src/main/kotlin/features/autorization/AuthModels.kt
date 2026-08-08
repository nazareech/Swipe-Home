package com.swipehome.features.autorization

import kotlinx.serialization.Serializable

//================== Реєстрація ==================
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

//================== Логування ==================
@Serializable
data class LoginReceiveRemote(
    val login: String,
    val password: String,

    ) {
}
@Serializable
data class LoginResponseRemote(
    val token: String
)

//================== Відновлення пароля ==================
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
@Serializable
data class VerifyCodeRequest(
    val email: String,
    val code: Int
)
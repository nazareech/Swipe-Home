package com.swipehome.features.autorization

import com.swipehome.database.dao.PasswordResetsDao
import com.swipehome.database.users.Users
import com.swipehome.utils.CryptoUtils
import com.swipehome.utils.EmailService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthController(private val call: ApplicationCall) {

    // Запит на відновлення (Виправляння листа)
    suspend fun forgotPassword() {
        val request = call.receiveNullable<ForgotPasswordRequest>()
        if (request == null || request.email.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Email is required")
            return
        }

        // Знаходимо користувача за email
        val user = Users.getUserByEmail(request.email)

        if (user != null) {
            // генеруємо код та зберігаємо в бд
            val code = PasswordResetsDao.generateAndSaveCode(userId = user.id_user)

            // Відправляємо лист в окремому потоці (Dispatcher.IO)
            // Щоб не блокувати сервер, поки лист летить через Google
            CoroutineScope(Dispatchers.IO).launch {
                EmailService.sendResetCode(request.email, code)
            }
        }

        // Завжди повертаємо ОК, щоб хакери не знали, що існує такий email
        call.respond(HttpStatusCode.OK, mapOf("message" to "If this email is registered, a reset code has been sent."))
    }

    // Перевірка коду та встановлення нового пароля
    suspend fun resetPassword() {
        val request = call.receiveNullable<ResetPasswordRequest>()

        if (request == null || request.email.isBlank() || request.code == null || request.new_password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing required fields")
            return
        }

        val user = Users.getUserByEmail(request.email)
        if (user == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid request")
            return
        }

        // Перевіряємо, чи збігається код і чи не вийшов час
        val isCodeValid = PasswordResetsDao.verifyCode(userId = user.id_user, code = request.code)
        if (!isCodeValid) {
            call.respond(HttpStatusCode.BadRequest, "Invalid or expired code")
            return
        }

        //
        val hashedNewPassword = CryptoUtils.encrypt(request.new_password)

        val success = Users.updatePassword(userId = user.id_user, newPassword =  hashedNewPassword)
        if (success) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Password successfully reset"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, ("Failed to reset password"))
        }
    }
}
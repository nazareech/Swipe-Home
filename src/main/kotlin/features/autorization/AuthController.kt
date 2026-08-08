package com.swipehome.features.autorization

import com.swipehome.database.dao.PasswordResetsDao
import com.swipehome.database.tokens.TokenDTO
import com.swipehome.database.tokens.Tokens
import com.swipehome.database.users.UserDTO
import com.swipehome.database.users.Users
import com.swipehome.utils.CryptoUtils
import com.swipehome.utils.EmailService
import com.swipehome.utils.isValidEmail
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.util.UUID

class AuthController(private val call: ApplicationCall) {

    //================== Реєстрація ==================
    suspend fun registerNewUser() {
        val registerReceivedRemote = call.receive<RegisterReceivedRemote>()

        if(!registerReceivedRemote.email.isValidEmail()){
            call.respond(HttpStatusCode.BadRequest, "Invalid email")
            return
        }

        val existUser = Users.fetchUserByLogin(registerReceivedRemote.login)
        if(existUser != null) {
            call.respond(HttpStatusCode.Conflict, "User already exist")
            return
        }

        // Шифруємо пароль перед записом до бази
        val hashedPassword = CryptoUtils.encrypt(registerReceivedRemote.password)
        // Створюємо користувача та ОДРАЗУ отримуємо його новий ID
        val newUserId = Users.insertAndGetId(
            UserDTO(
                login = registerReceivedRemote.login,
                username = registerReceivedRemote.username,
                password = hashedPassword,
                email = registerReceivedRemote.email,
                phone = registerReceivedRemote.phone,
                is_verified_owner = registerReceivedRemote.is_verified_owner,
                is_admin = registerReceivedRemote.is_admin,
            )
        )

        // Створюємо токен для цього ID
        val token = UUID.randomUUID().toString()
        val expirationDate = OffsetDateTime.now().plusDays(30).toString()

        Tokens.insert(
            TokenDTO(
                login = registerReceivedRemote.login,
                token = token,
                id_user = newUserId, // Використовуємо ID, який повернула база
                expires_at = expirationDate
            )
        )

        call.respond(HttpStatusCode.OK, RegisterResponseRemote(token = token))
    }

    //================== Логування ==================
    suspend fun performLogin(){
        val receive = call.receive<LoginReceiveRemote>()

        // Звертаємося до реальної бази даних
        val userDTO = Users.fetchUserByLogin(receive.login)

        // Якщо юзера немає
        if(userDTO == null) {
            call.respond(HttpStatusCode.BadRequest, "User not found")
            return
        }

        // Розшифровуємо пароль
        val decryptPassword = CryptoUtils.decrypt(userDTO.password )
        // Перевірка пароля
        if (receive.password == decryptPassword) {
            val token = UUID.randomUUID().toString()

            // Безпечно дістаємо ID користувача (Елвіс-оператор)
            val userId = userDTO.id_user ?: return call
                .respond(HttpStatusCode.InternalServerError,
                    "Database error: User ID is null")

            // Створюємо дадту
            val expirationDate = OffsetDateTime.now().plusDays(30).toString()

            Tokens.insert(
                TokenDTO(
                    login = userDTO.login,
                    token = token,
                    id_user = userId,
                    expires_at = expirationDate
                )
            )
            call.respond(HttpStatusCode.OK, RegisterResponseRemote(token = token))
        } else {
            call.respond(HttpStatusCode.BadRequest, "Invalid password")
        }
    }

    suspend fun performLogout(){
        val authHeader = call.request.headers["Authorization"]
        val token = authHeader?.removePrefix("Bearer ") ?: ""

        if(token.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Token is blank")
            return
        }

        // Видаляємо токен з бази даних
        val isDeleted = Tokens.deleteToken(token)

        if(isDeleted) {
            call.respond(HttpStatusCode.OK, "Successfully logged out. Token Deleted")
        } else {
            call.respond(HttpStatusCode.BadRequest, "Token not found")
        }
    }

    //================== Відновлення пароля ==================
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

    // Перевіряємо код відновлення
    suspend fun verifyResetCode() {
        val request = call.receiveNullable<VerifyCodeRequest>()

        if (request == null || request.email.isBlank() || request.code == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing Email or code")
            return
        }

        val user = Users.getUserByEmail(request.email)
        if (user == null) {
            call.respond(HttpStatusCode.BadRequest, "No user with this email address '${request.email}' was found in the database")
        }

        val isCodeValid = PasswordResetsDao.isCodeValidOnly(user?.id_user, request.code)

        if (isCodeValid) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Code is valid"))
        } else {
            call.respond(HttpStatusCode.BadRequest, ("Invalid or expired code"))
        }
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
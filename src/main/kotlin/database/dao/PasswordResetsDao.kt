package com.swipehome.database.dao

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset

object PasswordResetsDao {

    fun generateAndSaveCode(userId: Int?): Int {
        // Генеруємо випадковий 6-значний код
        val code = (100000..999999).random()

        transaction {
            // Видаляємо попередні коди, щоб був тільки один активний
            PasswordResets.deleteAllCodesById(userId)

            PasswordResets.putNewResetCode(userId, code)
        }
        return code
    }

    fun verifyCode(userId: Int?, code: Int?): Boolean{
        // ЩОБ ЗНАТИ ТОЧНО: Додаємо прінт для дебагу
        println("DEBUG: Перевіряємо код $code для юзера $userId")

        if(userId == null || code == null) return false

        return transaction {
            val resetRecord = PasswordResets.selectAll()
                .where { (PasswordResets.id_user eq userId) and (PasswordResets.reset_code eq code) }
                .singleOrNull()

            if (resetRecord != null){
                val expiredAt = resetRecord[PasswordResets.expired_at]
                val currentTime = OffsetDateTime.now(ZoneOffset.UTC)

                println("==== DEBUG PASSWORD RESET ====")
                println("Input code: $code")
                println("DB code:    ${resetRecord[PasswordResets.reset_code]}")
                println("Current (UTC): ${currentTime.toInstant()}")
                println("Expired (UTC): ${expiredAt.toInstant()}")
                println("Is Before?:    ${currentTime.toInstant().isBefore(expiredAt.toInstant())}")
                println("==============================")

                println("DEBUG: Час вийшов? Поточний час: $currentTime, Час в базі: $expiredAt")
                // Беремо поточний час у UTC

                // Перевіряємо, чи час дії ще не вийшов порівнюємо через toInstant(), щоб повністю проігнорувати різницю
                // часових поясів (+02:00 чи Z) і порівняти саме фізичний момент часу
                if (currentTime.toInstant().isBefore(expiredAt.toInstant())) {
                    // Код вірний! Одразу видаляємо його, щоб не можна було вписати двічі
                    PasswordResets.deleteWhere {
                        PasswordResets.id_passwd_reset eq resetRecord[PasswordResets.id_passwd_reset]
                    }
                    return@transaction true
                }
            }
            false
        }
    }

    fun isCodeValidOnly(userId: Int?, code: Int?): Boolean{
        if (userId == null || code == null) return false

        return transaction {
            val resetRecord = PasswordResets.selectAll()
                .where { (PasswordResets.reset_code eq code) and (PasswordResets.id_user eq userId) }
                .singleOrNull()

            if (resetRecord != null){
                val expiredAt = resetRecord[PasswordResets.expired_at]
                val currentTime = OffsetDateTime.now(ZoneOffset.UTC)

                if (currentTime.toInstant().isBefore(expiredAt.toInstant())){
                    return@transaction true
                }
            }
            false
        }
    }
}
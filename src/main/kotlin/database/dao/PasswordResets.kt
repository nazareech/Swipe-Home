package com.swipehome.database.dao

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset

object PasswordResets : Table("password_resets") {
    val id_passwd_reset = integer("id_passwd_reset").autoIncrement()
    val id_user = integer("id_user")
    val reset_code = integer("reset_code")
    val expired_at = timestampWithTimeZone("expired_at").clientDefault { OffsetDateTime.now(ZoneOffset.UTC) }

    override val primaryKey = PrimaryKey(id_passwd_reset)


    fun deleteAllCodesById(userId: Int?): Boolean{
        if(userId == null) return false

        return transaction {
            val deletedRows = PasswordResets.deleteWhere { id_user eq userId}
            deletedRows > 0
        }
    }

    fun putNewResetCode(userId: Int?, code: Int): Boolean{
        if(userId == null) return false
        // Час дії - 15 хвилин від поточного моменту
        val expirationTime = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15)

        // Беремо поточний час у мілісекундах + 15 хвилин (в мілісекундах)
//        val expirationTime = System.currentTimeMillis() + (15 * 60 * 1000)

        return transaction {
            val resaltOperation = PasswordResets.insert {
                it[id_user] = userId
                it[reset_code] = code
                it[expired_at] = expirationTime
            }

            if (resaltOperation != null){
                return@transaction true
            }
            false
        }
    }
}
package com.swipehome.database.tokens

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

object Tokens: Table("tokens") {
    val id_token = integer("id_token").autoIncrement()
    val login = varchar("login", 25)
    val token = varchar("token", 50)
    val id_user = integer("id_user")
    val expires_at = timestampWithTimeZone("expires_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(Tokens.id_token)

    fun insert(tokenDTO: TokenDTO){
        transaction {
            Tokens.insert {
                it[login] = tokenDTO.login
                it[token] = tokenDTO.token
                it[id_user] = tokenDTO.id_user
                it[expires_at] = OffsetDateTime.parse(tokenDTO.expires_at)
            }
        }
    }

    fun deleteToken(tokenToDelete: String): Boolean
    {
        return transaction {
            // Видаляємо рядок де токен збігається з переданим
            val deletedRows = Tokens.deleteWhere { Tokens.token eq tokenToDelete }
            deletedRows > 0 // Поверне true, якщо видалить хоч один рядок
        }
    }

    fun deleteExpiredTokens() {
        transaction {
            val now = OffsetDateTime.now()
            Tokens.deleteWhere { Tokens.expires_at less now }
        }
    }

    fun fetchTokenByLogin(searchLogin: String): TokenDTO? {
        return transaction {
            val resultRow = Tokens.selectAll().where { Tokens.login eq searchLogin }.singleOrNull()

            resultRow?.let {
                TokenDTO(
                    id_token = it[Tokens.id_token],
                    login = it[Tokens.login],
                    token = it[Tokens.token],
                    id_user = it[Tokens.id_user],
                    expires_at = it[Tokens.expires_at].toString()
                )
            }
        }
    }

    fun searchToken(searchToken: String): TokenDTO? {
        return transaction {
            val resultRow = Tokens.selectAll().where { Tokens.token eq searchToken }.singleOrNull()

            resultRow?.let {
                TokenDTO(
                    id_token = it[Tokens.id_token],
                    login = it[Tokens.login],
                    token = it[Tokens.token],
                    id_user = it[Tokens.id_user],
                    expires_at = it[Tokens.expires_at].toString()
                )
            }
        }
    }
}
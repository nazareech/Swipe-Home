package com.swipehome.database.users

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime

object Users: Table("users") {
    val id_user = integer("id_user").autoIncrement()
    val login = varchar("login", 25)
    val username = varchar("username", 30)
    val password = varchar("password", 25)
    val email = varchar("email", 25)
    val phone = varchar("phone", 25)
    val is_verified_owner = bool("is_verified_owner")
    val is_admin = bool("is_admin")
    val last_seen = timestampWithTimeZone("last_seen").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id_user)

    // Зберігає юзера і одразу віддає його ID
    fun insertAndGetId(userDTO: UserDTO): Int {
        return transaction {
            val insertStatement = Users.insert {
                it[login] = userDTO.login
                it[username] = userDTO.username
                it[password] = userDTO.password
                it[email] = userDTO.email
                it[phone] = userDTO.phone
                it[is_verified_owner] = userDTO.is_verified_owner
                it[is_admin] = userDTO.is_admin
                it[last_seen] = OffsetDateTime.now()
            }
            // Повертаємо щойно згенерований id_user
            insertStatement[Users.id_user]
        }
    }

    fun fetchUserByLogin(searchLogin: String): UserDTO? {
        return transaction {
            val resultRow = Users.selectAll().where { Users.login eq searchLogin }.singleOrNull()
            resultRow?.let {
                UserDTO(
                    id_user = it[Users.id_user],
                    login = it[Users.login],
                    username = it[Users.username],
                    password = it[Users.password],
                    email = it[Users.email],
                    phone = it[Users.phone],
                    is_verified_owner = it[Users.is_verified_owner],
                    is_admin = it[Users.is_admin],
                    last_seen = it[Users.last_seen].toString()
                )
            }
        }
    }

    fun updateLastSeen(userId: Int) {
        transaction {
            Users.update ({ Users.id_user eq userId }) {
                it[last_seen] = OffsetDateTime.now()
            }
        }
    }

    fun isVerifiedOwner(idUser: Int): Boolean{

        return true
    }

}
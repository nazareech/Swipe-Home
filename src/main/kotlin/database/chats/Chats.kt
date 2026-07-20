package com.swipehome.database.chats

import com.swipehome.database.properties.Properties
import com.swipehome.database.users.Users
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime

object Chats : Table("chats") {
    val id_chat = integer("id_chat").autoIncrement()
    val id_seeker = integer("id_seeker").references(Users.id_user)
    val id_owner = integer("id_owner").references(Users.id_user)
    val id_property = integer("id_property").references(Properties.id_property)
    val created_at = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id_chat)

    fun insertAndGetId(seekerId: Int, ownerId: Int, propertyId: Int){
        return transaction {


        }
    }

    fun createOrGetChat(seekerId: Int, ownerId: Int, propertyId: Int): Int {
        return transaction {
            val existingChat = Chats.selectAll().where{
                (id_seeker eq seekerId) and (id_owner eq ownerId) and (id_property eq propertyId)
            }.singleOrNull()

            if (existingChat != null){
                return@transaction existingChat[id_chat]
            }

            // Створюємо новий чат, якщо його ще немає
            val insertStatement = Chats.insert {
                it[id_seeker] = seekerId
                it[id_owner] = ownerId
                it[id_property] = propertyId
            }
            insertStatement[Chats.id_chat]
        }
    }

    fun fetchChatForUser(userId: Int?): List<ChatDTO> {
        if (userId == null) return listOf()

        return transaction {
            Chats.selectAll().where {
                (id_seeker eq userId) or (id_owner eq userId)
            }.map {
                ChatDTO(
                    id_chat = it[id_chat],
                    id_seeker = it[id_seeker],
                    id_owner = it[id_owner],
                    id_property = it[id_property],
                    created_at = it[created_at].toString()
                )
            }
        }
    }
}
package com.swipehome.database.chats

import com.swipehome.database.properties.Properties
import com.swipehome.database.users.Users
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.OffsetDateTime

object Chats : Table("chats") {
    val id_chat = integer("id_chat").autoIncrement()
    val id_seeker = integer("id_seeker").references(Users.id_user)
    val id_owner = integer("id_owner").references(Users.id_user)
    val id_property = integer("id_property").references(Properties.id_property)
    val created_at = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id_chat)
}
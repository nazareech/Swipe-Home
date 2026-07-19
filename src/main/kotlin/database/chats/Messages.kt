package com.swipehome.database.chats

import com.swipehome.database.users.Users
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.OffsetDateTime

object Messages : Table("messages")  {
    val id_messages = integer("id_messages").autoIncrement()
    val id_chat = integer("id_chat").references(Chats.id_chat)
    val id_sender = integer("id_sender").references(Users.id_user)
    val content = text("content")
    val sent_at = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id_messages)
}
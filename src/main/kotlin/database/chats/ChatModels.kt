package com.swipehome.database.chats

import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ChatDTO(
    val id_chat: Int,
    val id_seeker: Int,
    val id_owner: Int,
    val id_property: Int,
    val created_at: String
)

@Serializable
data class CreateChatRequest(
    val id_owner: Int,
    val id_property: Int
)

@Serializable
data class CreateChatResponse(
    val id_chat: Int,
    val messages: String
)

@Serializable
data class MessageDTO(
    val id_message: Int,
    val id_chat: Int,
    val id_sender: Int,
    val content: String,
    val sent_at: String
)

@Serializable
data class SendMessageRequest(
    val content: String
)
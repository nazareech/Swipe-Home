package com.swipehome.database.chats

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
enum class ChatActions {
    @SerialName("new") NEW,
    @SerialName("edit") EDIT,
    @SerialName("delete") DELETE,
    @SerialName("typing") TYPING,
    @SerialName("stop_typing") STOP_TYPING,
    @SerialName("send_message") SEND_MESSAGE
}

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

@Serializable
data class EditMessageRequest(
    val new_content: String
)

@Serializable
data class WsEvent(
    val action: ChatActions,                 // new, edit, delete
    val id_message: Int? = null,
    val id_sender: Int? = null,             // Щоб знати яку аватарку анімувати
    val content: String? = null,            // для "edit"
    val messages: MessageDTO? = null        // Для "new", щоб клієнт отримав всю інформацію повідомлення

)

@Serializable
data class ClientWsMessage(
    val action: ChatActions,         // Може бути "send_message", "typing", "stop_typing"
    val content: String? = null // Текст повідомлення (якщо action == "send_message")
)
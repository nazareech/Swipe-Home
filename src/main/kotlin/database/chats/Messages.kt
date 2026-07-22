package com.swipehome.database.chats

import com.swipehome.database.users.Users
import com.swipehome.utils.CryptoUtils
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime

object Messages : Table("messages")  {
    val id_message = integer("id_message").autoIncrement()
    val id_chat = integer("id_chat").references(Chats.id_chat)
    val id_sender = integer("id_sender").references(Users.id_user)
    val content = text("content")
    val sent_at = timestampWithTimeZone("sent_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id_message)

    // Збереження нового повідомлення
    fun insertMessage(chatId: Int, senderId: Int, textContent: String): MessageDTO? {
        // Зашифровуємо повідомлення в базу даних
        val encryptedContent = CryptoUtils.encrypt(textContent)

        return transaction {
            val insertStatement = Messages.insert {
                it[id_chat] = chatId
                it[id_sender] = senderId
                it[content] = encryptedContent
            }

            // Після збереження одразу читаємо його з бази, щоб повернути клієнту разом із id_message та часом
            val newId = insertStatement[Messages.id_message]

            Messages.selectAll().where { id_message eq newId }.singleOrNull()?.let {
                MessageDTO(
                    id_message = it[Messages.id_message],
                    id_chat = it[Messages.id_chat],
                    id_sender = it[Messages.id_sender],
                    // Розшифровуємо повідомлення клієнту
                    content = CryptoUtils.decrypt(it[Messages.content]),
                    sent_at = it[Messages.sent_at].toString()
                )
            }
        }
    }

    // Отримання історії повідомлень
    fun fetchChatHistory(chatId: Int): List<MessageDTO> {
        return transaction {
            Messages.selectAll().where { Messages.id_chat eq chatId  }
                // Сортуємо від найстаршого повідомлення
                .orderBy(Messages.sent_at to SortOrder.ASC)
                .map {
                    MessageDTO(
                        id_message = it[Messages.id_message],
                        id_chat = it[Messages.id_chat],
                        id_sender = it[Messages.id_sender],
                        content = CryptoUtils.decrypt(it[Messages.content]),
                        sent_at = it[Messages.sent_at].toString()
                    )
                }
        }
    }

    //Редагування повідомлень
    fun editMessage(messageId: Int, senderId: Int, newText: String): Boolean{
        val encryptedContent = CryptoUtils.encrypt(newText)

        return transaction {
            val updatedRows = Messages.update ({
                (Messages.id_message eq messageId) and (Messages.id_sender eq senderId)
            }){
                it[content] = encryptedContent
            }
            updatedRows > 0 // Повертаємо true, якщо запит успішно оновлений
        }
    }

    //Видалення повідомлень
    fun deleteMessage(messageId: Int, senderId: Int): Boolean{
        return transaction {
            val deletedRows = Messages.deleteWhere{
                (Messages.id_message eq messageId) and (Messages.id_sender eq senderId)
            }
            deletedRows > 0 // Повертаємо true, якщо виконано видалення
        }
    }
}
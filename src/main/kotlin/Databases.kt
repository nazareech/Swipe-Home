package com.swipehome

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

import com.swipehome.database.users.Users
import com.swipehome.database.tokens.Tokens
import com.swipehome.database.properties.Properties
import com.swipehome.database.properties.PropertyImages
import com.swipehome.database.swipes.Swipes
import com.swipehome.database.chats.Chats
import com.swipehome.database.chats.Messages

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/swipehome",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "root"
    )

    // Створення таблиць якщо їх ще немає
    transaction {
        SchemaUtils.create(
            Users,
            Tokens,
            Properties,
            PropertyImages,
            Swipes,
            Chats,
            Messages
        )
    }

}
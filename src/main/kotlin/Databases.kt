package com.swipehome

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabases() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/swipehome",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "root"
    )

}
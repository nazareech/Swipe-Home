package com.swipehome

import io.ktor.server.engine.*
import io.ktor.server.application.*
import io.ktor.server.cio.CIO

fun main(args: Array<String>) {
    embeddedServer(
        factory = CIO,
        port = 8080,
        host = "0.0.0.0",
        module = Application::rootModule
    ).start(wait = true)
}

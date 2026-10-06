package com.barbershop

import com.barbershop.infrastructure.database.DatabaseFactory
import com.barbershop.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init(environment.config)

    configureDependencyInjection()
    configureSerialization()
    configureSecurity()
    configureStatusPages()
    configureRouting()
}

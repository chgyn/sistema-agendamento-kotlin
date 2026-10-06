package com.barbershop.plugins

import com.barbershop.presentation.routes.adminRoutes
import com.barbershop.presentation.routes.authRoutes
import com.barbershop.presentation.routes.publicRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthStatusResponse(val status: String, val timestamp: Long)

fun Application.configureRouting() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    routing {
        // Healthcheck
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthStatusResponse("UP", System.currentTimeMillis()))
        }

        // Rotas REST da API
        authRoutes()
        publicRoutes()
        adminRoutes()

        // Servir artefatos estáticos da SPA Vue 3
        staticResources("/", "static") {
            default("index.html")
        }

        // Fallback para SPA Vue (qualquer caminho não-api que não seja arquivo serve o index.html)
        get("/{...}") {
            val path = call.request.local.uri
            if (!path.startsWith("/api/")) {
                val indexStream = this::class.java.classLoader.getResourceAsStream("static/index.html")
                if (indexStream != null) {
                    val html = indexStream.bufferedReader().use { it.readText() }
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.respondText("Barbershop Monolith API running. Build frontend to see UI.", ContentType.Text.Plain)
                }
            }
        }
    }
}

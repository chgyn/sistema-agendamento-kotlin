package com.barbershop.presentation.routes

import com.barbershop.application.dtos.LoginRequest
import com.barbershop.application.usecases.AuthenticateUserUseCase
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.barbershop.plugins.koinInject
import kotlinx.serialization.Serializable

@Serializable
data class UserMeResponse(
    val id: String?,
    val email: String?,
    val role: String?,
    val tenantId: String?
)

fun Route.authRoutes() {
    val authenticateUserUseCase by koinInject<AuthenticateUserUseCase>()

    route("/api/v1/auth") {
        post("/login") {
            val req = call.receive<LoginRequest>()
            val response = authenticateUserUseCase.execute(req)
            call.respond(HttpStatusCode.OK, response)
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                val email = principal?.payload?.getClaim("email")?.asString()
                val role = principal?.payload?.getClaim("role")?.asString()
                val tenantId = principal?.payload?.getClaim("tenantId")?.asString()

                call.respond(
                    HttpStatusCode.OK,
                    UserMeResponse(
                        id = userId,
                        email = email,
                        role = role,
                        tenantId = tenantId
                    )
                )
            }
        }
    }
}

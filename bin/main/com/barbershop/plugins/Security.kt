package com.barbershop.plugins

import com.auth0.jwt.JWT
import com.barbershop.domain.errors.UnauthorizedBusinessException
import com.barbershop.infrastructure.security.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    val jwtConfig by koinInject<JwtConfig>()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(
                JWT.require(jwtConfig.algorithm)
                    .withAudience(jwtConfig.audience)
                    .withIssuer(jwtConfig.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.subject != null && credential.payload.getClaim("email").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                throw UnauthorizedBusinessException("Token de acesso inválido ou expirado.")
            }
        }
    }
}

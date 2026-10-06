package com.barbershop.plugins

import com.barbershop.application.dtos.ErrorResponse
import com.barbershop.domain.errors.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("com.barbershop.StatusPages")

    install(StatusPages) {
        exception<EntityNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(code = "NOT_FOUND", message = cause.message ?: "Recurso não encontrado.")
            )
        }

        exception<ScheduleConflictException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse(code = "SCHEDULE_CONFLICT", message = cause.message ?: "Conflito de agendamento.")
            )
        }

        exception<InvalidCancellationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "INVALID_CANCELLATION", message = cause.message ?: "Cancelamento inválido.")
            )
        }

        exception<UnauthorizedBusinessException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ErrorResponse(code = "FORBIDDEN", message = cause.message ?: "Acesso não autorizado.")
            )
        }

        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "VALIDATION_ERROR", message = cause.message ?: "Dados inválidos.")
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "BAD_REQUEST", message = cause.message ?: "Parâmetros inválidos.")
            )
        }

        exception<Throwable> { call, cause ->
            logger.error("Erro interno não tratado na requisição [{}]: {}", call.request.local.uri, cause.message, cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(code = "INTERNAL_SERVER_ERROR", message = "Ocorreu um erro interno no servidor.")
            )
        }
    }
}

package com.barbershop.presentation.routes

import com.barbershop.application.dtos.CreatePublicAppointmentRequest
import com.barbershop.application.usecases.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.barbershop.plugins.koinInject
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class MessageResponse(val message: String)

fun Route.publicRoutes() {
    val getTenantDetailsUseCase by koinInject<GetPublicTenantDetailsUseCase>()
    val calculateSlotsUseCase by koinInject<CalculateAvailableSlotsUseCase>()
    val createAppointmentUseCase by koinInject<CreatePublicAppointmentUseCase>()
    val getAppointmentByTokenUseCase by koinInject<GetAppointmentByTokenUseCase>()
    val cancelAppointmentUseCase by koinInject<CancelAppointmentUseCase>()

    route("/api/v1/public") {
        get("/tenants/{slug}") {
            val slug = call.parameters["slug"] ?: throw IllegalArgumentException("Slug é obrigatório.")
            val response = getTenantDetailsUseCase.execute(slug)
            call.respond(HttpStatusCode.OK, response)
        }

        get("/tenants/{slug}/availability") {
            val slug = call.parameters["slug"] ?: throw IllegalArgumentException("Slug é obrigatório.")
            val serviceIdStr = call.request.queryParameters["serviceId"]
                ?: throw IllegalArgumentException("Parâmetro 'serviceId' é obrigatório.")
            val dateStr = call.request.queryParameters["date"]
                ?: throw IllegalArgumentException("Parâmetro 'date' é obrigatório (YYYY-MM-DD).")
            val profIdStr = call.request.queryParameters["professionalId"]

            val serviceId = UUID.fromString(serviceIdStr)
            val date = LocalDate.parse(dateStr)
            val profId = profIdStr?.let { UUID.fromString(it) }

            val slots = calculateSlotsUseCase.execute(slug, serviceId, date, profId)
            call.respond(HttpStatusCode.OK, slots)
        }

        post("/tenants/{slug}/appointments") {
            val slug = call.parameters["slug"] ?: throw IllegalArgumentException("Slug é obrigatório.")
            val req = call.receive<CreatePublicAppointmentRequest>()
            val response = createAppointmentUseCase.execute(slug, req)
            call.respond(HttpStatusCode.Created, response)
        }

        get("/appointments/{token}") {
            val token = call.parameters["token"] ?: throw IllegalArgumentException("Token é obrigatório.")
            val details = getAppointmentByTokenUseCase.execute(token)
            call.respond(HttpStatusCode.OK, details)
        }

        post("/appointments/{token}/cancel") {
            val token = call.parameters["token"] ?: throw IllegalArgumentException("Token é obrigatório.")
            cancelAppointmentUseCase.execute(token)
            call.respond(HttpStatusCode.OK, MessageResponse("Agendamento cancelado com sucesso."))
        }
    }
}

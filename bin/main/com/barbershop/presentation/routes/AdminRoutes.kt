package com.barbershop.presentation.routes

import com.barbershop.application.dtos.*
import com.barbershop.application.usecases.*
import com.barbershop.domain.errors.UnauthorizedBusinessException
import com.barbershop.domain.model.UserRole
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.barbershop.plugins.koinInject
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class AdminActionResponse(val message: String)

fun Route.adminRoutes() {
    val listAppointmentsUseCase by koinInject<ListAdminAppointmentsUseCase>()
    val updateStatusUseCase by koinInject<UpdateAppointmentStatusUseCase>()
    val manageProfessionalsUseCase by koinInject<ManageProfessionalsUseCase>()
    val manageServicesUseCase by koinInject<ManageServicesUseCase>()
    val createTenantUseCase by koinInject<CreateTenantUseCase>()

    authenticate("auth-jwt") {
        route("/api/v1/admin") {

            // --- Super Admin: Gerenciamento de Barbearias (Tenants) ---
            get("/tenants") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != UserRole.SUPER_ADMIN.name) {
                    throw UnauthorizedBusinessException("Apenas o Super Administrador pode listar estabelecimentos.")
                }
                val tenants = createTenantUseCase.listAll()
                call.respond(HttpStatusCode.OK, tenants)
            }

            post("/tenants") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != UserRole.SUPER_ADMIN.name) {
                    throw UnauthorizedBusinessException("Apenas o Super Administrador pode criar novos estabelecimentos.")
                }
                val req = call.receive<CreateTenantRequest>()
                val created = createTenantUseCase.execute(req)
                call.respond(HttpStatusCode.Created, created)
            }

            // --- Tenant Admin & Operador: Agendamentos ---
            get("/appointments") {
                val principal = call.principal<JWTPrincipal>()
                val tenantIdStr = principal?.payload?.getClaim("tenantId")?.asString()
                    ?: throw UnauthorizedBusinessException("Usuário sem estabelecimento vinculado.")
                val tenantId = UUID.fromString(tenantIdStr)

                val dateStr = call.request.queryParameters["date"] ?: LocalDate.now().toString()
                val date = LocalDate.parse(dateStr)

                val list = listAppointmentsUseCase.execute(tenantId, date)
                call.respond(HttpStatusCode.OK, list)
            }

            patch("/appointments/{id}/status") {
                val principal = call.principal<JWTPrincipal>()
                val tenantIdStr = principal?.payload?.getClaim("tenantId")?.asString()
                val role = principal?.payload?.getClaim("role")?.asString()

                val apptId = UUID.fromString(
                    call.parameters["id"] ?: throw IllegalArgumentException("ID do agendamento é obrigatório.")
                )
                val req = call.receive<UpdateAppointmentStatusRequest>()

                val tenantId = if (role == UserRole.SUPER_ADMIN.name) null else tenantIdStr?.let { UUID.fromString(it) }
                val updated = updateStatusUseCase.execute(apptId, tenantId, req.status)
                call.respond(HttpStatusCode.OK, updated)
            }

            // --- Profissionais e Grades ---
            get("/professionals") {
                val principal = call.principal<JWTPrincipal>()
                val tenantIdStr = principal?.payload?.getClaim("tenantId")?.asString()
                    ?: throw UnauthorizedBusinessException("Usuário sem estabelecimento vinculado.")
                val tenantId = UUID.fromString(tenantIdStr)

                val list = manageProfessionalsUseCase.list(tenantId)
                call.respond(HttpStatusCode.OK, list)
            }

            post("/professionals") {
                val principal = call.principal<JWTPrincipal>()
                val tenantIdStr = principal?.payload?.getClaim("tenantId")?.asString()
                    ?: throw UnauthorizedBusinessException("Usuário sem estabelecimento vinculado.")
                val tenantId = UUID.fromString(tenantIdStr)

                val req = call.receive<CreateProfessionalRequest>()
                val created = manageProfessionalsUseCase.create(tenantId, req)
                call.respond(HttpStatusCode.Created, created)
            }

            get("/professionals/{id}/schedule") {
                val profId = UUID.fromString(
                    call.parameters["id"] ?: throw IllegalArgumentException("ID do profissional é obrigatório.")
                )
                val schedule = manageProfessionalsUseCase.getWorkingHours(profId)
                call.respond(HttpStatusCode.OK, schedule)
            }

            put("/professionals/{id}/schedule") {
                val profId = UUID.fromString(
                    call.parameters["id"] ?: throw IllegalArgumentException("ID do profissional é obrigatório.")
                )
                val req = call.receive<SetWorkingHoursRequest>()
                manageProfessionalsUseCase.setWorkingHours(profId, req.hours)
                call.respond(HttpStatusCode.OK, AdminActionResponse("Horários atualizados com sucesso."))
            }

            // --- Serviços ---
            get("/services") {
                val principal = call.principal<JWTPrincipal>()
                val tenantIdStr = principal?.payload?.getClaim("tenantId")?.asString()
                    ?: throw UnauthorizedBusinessException("Usuário sem estabelecimento vinculado.")
                val tenantId = UUID.fromString(tenantIdStr)

                val list = manageServicesUseCase.list(tenantId)
                call.respond(HttpStatusCode.OK, list)
            }

            post("/services") {
                val principal = call.principal<JWTPrincipal>()
                val tenantIdStr = principal?.payload?.getClaim("tenantId")?.asString()
                    ?: throw UnauthorizedBusinessException("Usuário sem estabelecimento vinculado.")
                val tenantId = UUID.fromString(tenantIdStr)

                val req = call.receive<CreateServiceRequest>()
                val created = manageServicesUseCase.create(tenantId, req)
                call.respond(HttpStatusCode.Created, created)
            }
        }
    }
}

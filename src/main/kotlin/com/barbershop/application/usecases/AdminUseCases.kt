package com.barbershop.application.usecases

import com.barbershop.application.dtos.*
import com.barbershop.domain.errors.EntityNotFoundException
import com.barbershop.domain.errors.ValidationException
import com.barbershop.domain.model.*
import com.barbershop.domain.ports.*
import com.barbershop.infrastructure.security.PasswordHasher
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class ListAdminAppointmentsUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val tenantRepository: TenantRepository,
    private val serviceRepository: ServiceRepository,
    private val professionalRepository: ProfessionalRepository
) {
    suspend fun execute(tenantId: UUID, date: LocalDate): List<AdminAppointmentDto> {
        val tenant = tenantRepository.findById(tenantId)
            ?: throw EntityNotFoundException("Estabelecimento não encontrado.")

        val zoneId = ZoneId.of(tenant.timezone)
        val start = date.atStartOfDay(zoneId).toOffsetDateTime()
        val end = date.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime()

        val appointments = appointmentRepository.findByTenantAndPeriod(tenantId, start, end)
        if (appointments.isEmpty()) return emptyList()

        val servicesMap = serviceRepository.findByTenantId(tenantId, onlyActive = false).associateBy { it.id }
        val professionalsMap = professionalRepository.findByTenantId(tenantId, onlyActive = false).associateBy { it.id }

        return appointments.map { appt ->
            val sName = servicesMap[appt.serviceId]?.name ?: "Serviço Desconhecido"
            val pName = professionalsMap[appt.professionalId]?.name ?: "Profissional Desconhecido"

            AdminAppointmentDto(
                id = appt.id.toString(),
                customerName = appt.customerName,
                customerPhone = appt.customerPhone,
                customerEmail = appt.customerEmail,
                serviceId = appt.serviceId.toString(),
                serviceName = sName,
                professionalId = appt.professionalId.toString(),
                professionalName = pName,
                startTime = appt.startTime.toString(),
                endTime = appt.endTime.toString(),
                status = appt.status.name,
                notes = appt.notes
            )
        }
    }
}

class UpdateAppointmentStatusUseCase(
    private val appointmentRepository: AppointmentRepository
) {
    suspend fun execute(appointmentId: UUID, tenantId: UUID?, newStatusStr: String): AdminAppointmentDto {
        val appt = appointmentRepository.findById(appointmentId)
            ?: throw EntityNotFoundException("Agendamento não encontrado.")

        if (tenantId != null && appt.tenantId != tenantId) {
            throw ValidationException("Agendamento não pertence ao estabelecimento atual.")
        }

        val newStatus = try {
            AppointmentStatus.valueOf(newStatusStr.uppercase())
        } catch (_: Exception) {
            throw ValidationException("Status inválido. Permitidos: SCHEDULED, CONFIRMED, COMPLETED, CANCELLED.")
        }

        val updated = appointmentRepository.updateStatus(appointmentId, newStatus)

        return AdminAppointmentDto(
            id = updated.id.toString(),
            customerName = updated.customerName,
            customerPhone = updated.customerPhone,
            customerEmail = updated.customerEmail,
            serviceId = updated.serviceId.toString(),
            serviceName = "",
            professionalId = updated.professionalId.toString(),
            professionalName = "",
            startTime = updated.startTime.toString(),
            endTime = updated.endTime.toString(),
            status = updated.status.name,
            notes = updated.notes
        )
    }
}

class ManageProfessionalsUseCase(
    private val professionalRepository: ProfessionalRepository,
    private val workingHoursRepository: WorkingHoursRepository
) {
    suspend fun list(tenantId: UUID): List<PublicProfessionalDto> {
        return professionalRepository.findByTenantId(tenantId, onlyActive = false).map {
            PublicProfessionalDto(
                id = it.id.toString(),
                name = it.name,
                bio = it.bio,
                serviceIds = it.serviceIds.map { sId -> sId.toString() }
            )
        }
    }

    suspend fun create(tenantId: UUID, request: CreateProfessionalRequest): PublicProfessionalDto {
        val profId = UUID.randomUUID()
        val prof = Professional(
            id = profId,
            tenantId = tenantId,
            name = request.name.trim(),
            email = request.email?.trim(),
            phone = request.phone?.trim(),
            bio = request.bio?.trim(),
            active = true
        )
        val saved = professionalRepository.save(prof)

        for (sIdStr in request.serviceIds) {
            val sId = UUID.fromString(sIdStr)
            professionalRepository.associateService(profId, sId)
        }

        return PublicProfessionalDto(
            id = saved.id.toString(),
            name = saved.name,
            bio = saved.bio,
            serviceIds = request.serviceIds
        )
    }

    suspend fun setWorkingHours(professionalId: UUID, hours: List<WorkingHourInputDto>) {
        val workingHoursList = hours.map {
            WorkingHours(
                id = UUID.randomUUID(),
                professionalId = professionalId,
                dayOfWeek = it.dayOfWeek,
                startTime = LocalTime.parse(it.startTime),
                endTime = LocalTime.parse(it.endTime),
                breakStart = it.breakStart?.let { b -> LocalTime.parse(b) },
                breakEnd = it.breakEnd?.let { b -> LocalTime.parse(b) },
                slotDurationMinutes = it.slotDurationMinutes
            )
        }
        workingHoursRepository.saveAll(professionalId, workingHoursList)
    }

    suspend fun getWorkingHours(professionalId: UUID): List<WorkingHourResponseDto> {
        return workingHoursRepository.findByProfessionalId(professionalId).map {
            WorkingHourResponseDto(
                id = it.id.toString(),
                dayOfWeek = it.dayOfWeek,
                startTime = it.startTime.toString(),
                endTime = it.endTime.toString(),
                breakStart = it.breakStart?.toString(),
                breakEnd = it.breakEnd?.toString(),
                slotDurationMinutes = it.slotDurationMinutes
            )
        }
    }
}

class ManageServicesUseCase(
    private val serviceRepository: ServiceRepository
) {
    suspend fun list(tenantId: UUID): List<PublicServiceDto> {
        return serviceRepository.findByTenantId(tenantId, onlyActive = false).map {
            PublicServiceDto(
                id = it.id.toString(),
                name = it.name,
                description = it.description,
                durationMinutes = it.durationMinutes,
                price = it.price.toDouble()
            )
        }
    }

    suspend fun create(tenantId: UUID, request: CreateServiceRequest): PublicServiceDto {
        val service = ServiceItem(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            name = request.name.trim(),
            description = request.description?.trim(),
            durationMinutes = request.durationMinutes,
            price = BigDecimal.valueOf(request.price),
            active = true
        )
        val saved = serviceRepository.save(service)
        return PublicServiceDto(
            id = saved.id.toString(),
            name = saved.name,
            description = saved.description,
            durationMinutes = saved.durationMinutes,
            price = saved.price.toDouble()
        )
    }
}

class CreateTenantUseCase(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository
) {
    suspend fun execute(request: CreateTenantRequest): TenantAdminDto {
        if (tenantRepository.existsBySlug(request.slug)) {
            throw ValidationException("Já existe um estabelecimento com este slug.")
        }
        if (userRepository.existsByEmail(request.adminEmail)) {
            throw ValidationException("Já existe um usuário com este e-mail.")
        }

        val tenantId = UUID.randomUUID()
        val tenant = Tenant(
            id = tenantId,
            name = request.name.trim(),
            slug = request.slug.trim().lowercase(),
            phone = request.phone?.trim(),
            email = request.email?.trim(),
            active = true
        )
        val savedTenant = tenantRepository.save(tenant)

        val adminUser = User(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            name = request.adminName.trim(),
            email = request.adminEmail.trim().lowercase(),
            passwordHash = PasswordHasher.hash(request.adminPassword),
            role = UserRole.TENANT_ADMIN,
            active = true
        )
        userRepository.save(adminUser)

        return TenantAdminDto(
            id = savedTenant.id.toString(),
            name = savedTenant.name,
            slug = savedTenant.slug,
            phone = savedTenant.phone,
            email = savedTenant.email,
            active = savedTenant.active,
            createdAt = savedTenant.createdAt.toString()
        )
    }

    suspend fun listAll(): List<TenantAdminDto> {
        return tenantRepository.findAll().map {
            TenantAdminDto(
                id = it.id.toString(),
                name = it.name,
                slug = it.slug,
                phone = it.phone,
                email = it.email,
                active = it.active,
                createdAt = it.createdAt.toString()
            )
        }
    }
}

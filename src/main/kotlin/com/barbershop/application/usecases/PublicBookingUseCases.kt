package com.barbershop.application.usecases

import com.barbershop.application.dtos.*
import com.barbershop.domain.errors.EntityNotFoundException
import com.barbershop.domain.errors.InvalidCancellationException
import com.barbershop.domain.errors.ScheduleConflictException
import com.barbershop.domain.errors.ValidationException
import com.barbershop.domain.model.Appointment
import com.barbershop.domain.model.AppointmentStatus
import com.barbershop.domain.ports.*
import com.barbershop.infrastructure.security.TokenGenerator
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class GetPublicTenantDetailsUseCase(
    private val tenantRepository: TenantRepository,
    private val serviceRepository: ServiceRepository,
    private val professionalRepository: ProfessionalRepository
) {
    suspend fun execute(slug: String): PublicTenantResponse {
        val tenant = tenantRepository.findBySlug(slug)
            ?: throw EntityNotFoundException("Estabelecimento com slug '$slug' não encontrado.")

        if (!tenant.active) {
            throw EntityNotFoundException("Estabelecimento temporariamente indisponível.")
        }

        val services = serviceRepository.findByTenantId(tenant.id, onlyActive = true).map {
            PublicServiceDto(
                id = it.id.toString(),
                name = it.name,
                description = it.description,
                durationMinutes = it.durationMinutes,
                price = it.price.toDouble()
            )
        }

        val professionals = professionalRepository.findByTenantId(tenant.id, onlyActive = true).map {
            PublicProfessionalDto(
                id = it.id.toString(),
                name = it.name,
                bio = it.bio,
                serviceIds = it.serviceIds.map { sId -> sId.toString() }
            )
        }

        return PublicTenantResponse(
            id = tenant.id.toString(),
            name = tenant.name,
            slug = tenant.slug,
            phone = tenant.phone,
            email = tenant.email,
            timezone = tenant.timezone,
            services = services,
            professionals = professionals
        )
    }
}

class CalculateAvailableSlotsUseCase(
    private val tenantRepository: TenantRepository,
    private val serviceRepository: ServiceRepository,
    private val professionalRepository: ProfessionalRepository,
    private val workingHoursRepository: WorkingHoursRepository,
    private val appointmentRepository: AppointmentRepository
) {
    suspend fun execute(
        slug: String,
        serviceId: UUID,
        date: LocalDate,
        professionalId: UUID?
    ): List<AvailableSlotDto> {
        val tenant = tenantRepository.findBySlug(slug)
            ?: throw EntityNotFoundException("Estabelecimento não encontrado.")

        val service = serviceRepository.findById(serviceId)
            ?: throw EntityNotFoundException("Serviço não encontrado.")

        val professionals = if (professionalId != null) {
            val prof = professionalRepository.findById(professionalId)
                ?: throw EntityNotFoundException("Profissional não encontrado.")
            listOf(prof)
        } else {
            professionalRepository.findByTenantAndService(tenant.id, serviceId)
        }

        val zoneId = ZoneId.of(tenant.timezone)
        val now = OffsetDateTime.now(zoneId)
        val dayOfWeek = date.dayOfWeek.value // 1 (Segunda) a 7 (Domingo)

        val availableSlots = mutableListOf<AvailableSlotDto>()

        for (prof in professionals) {
            val workingHours = workingHoursRepository.findByProfessionalAndDay(prof.id, dayOfWeek)
                ?: continue // Não atende neste dia da semana

            val dayStart = date.atStartOfDay(zoneId).toOffsetDateTime()
            val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime()

            val existingAppointments = appointmentRepository.findByProfessionalAndPeriod(prof.id, dayStart, dayEnd)
            val scheduleBlocks = workingHoursRepository.findBlocksByProfessionalAndPeriod(prof.id, dayStart, dayEnd)

            var currentSlotStart = date.atTime(workingHours.startTime).atZone(zoneId).toOffsetDateTime()
            val dayWorkEnd = date.atTime(workingHours.endTime).atZone(zoneId).toOffsetDateTime()
            val durationMinutes = service.durationMinutes.toLong()

            while (currentSlotStart.plusMinutes(durationMinutes) <= dayWorkEnd) {
                val currentSlotEnd = currentSlotStart.plusMinutes(durationMinutes)

                // 1. Checar se está dentro do horário de pausa/almoço
                var conflictsWithBreak = false
                if (workingHours.breakStart != null && workingHours.breakEnd != null) {
                    val breakStart = date.atTime(workingHours.breakStart).atZone(zoneId).toOffsetDateTime()
                    val breakEnd = date.atTime(workingHours.breakEnd).atZone(zoneId).toOffsetDateTime()
                    if (currentSlotStart < breakEnd && currentSlotEnd > breakStart) {
                        conflictsWithBreak = true
                    }
                }

                // 2. Checar se o horário já passou (no passado)
                val isInPast = currentSlotStart <= now

                // 3. Checar colisão com agendamentos existentes
                val conflictsWithAppt = existingAppointments.any { appt ->
                    currentSlotStart < appt.endTime && currentSlotEnd > appt.startTime
                }

                // 4. Checar colisão com bloqueios de agenda
                val conflictsWithBlock = scheduleBlocks.any { block ->
                    currentSlotStart < block.endTime && currentSlotEnd > block.startTime
                }

                if (!conflictsWithBreak && !isInPast && !conflictsWithAppt && !conflictsWithBlock) {
                    availableSlots.add(
                        AvailableSlotDto(
                            startTime = currentSlotStart.toString(),
                            endTime = currentSlotEnd.toString(),
                            professionalId = prof.id.toString()
                        )
                    )
                }

                // Avança pelo intervalo mínimo configurado (ex: 30 minutos)
                currentSlotStart = currentSlotStart.plusMinutes(workingHours.slotDurationMinutes.toLong())
            }
        }

        return availableSlots.sortedBy { it.startTime }
    }
}

class CreatePublicAppointmentUseCase(
    private val tenantRepository: TenantRepository,
    private val serviceRepository: ServiceRepository,
    private val professionalRepository: ProfessionalRepository,
    private val appointmentRepository: AppointmentRepository,
    private val notificationPort: NotificationPort
) {
    private val logger = LoggerFactory.getLogger(CreatePublicAppointmentUseCase::class.java)

    suspend fun execute(slug: String, request: CreatePublicAppointmentRequest): AppointmentConfirmationResponse {
        val tenant = tenantRepository.findBySlug(slug)
            ?: throw EntityNotFoundException("Estabelecimento com slug '$slug' não encontrado.")

        val serviceId = try {
            UUID.fromString(request.serviceId)
        } catch (_: Exception) {
            throw ValidationException("ID de serviço inválido.")
        }

        val professionalId = try {
            UUID.fromString(request.professionalId)
        } catch (_: Exception) {
            throw ValidationException("ID de profissional inválido.")
        }

        val service = serviceRepository.findById(serviceId)
            ?: throw EntityNotFoundException("Serviço não encontrado.")

        val professional = professionalRepository.findById(professionalId)
            ?: throw EntityNotFoundException("Profissional não encontrado.")

        if (professional.tenantId != tenant.id || service.tenantId != tenant.id) {
            throw ValidationException("Profissional ou serviço não pertencem a este estabelecimento.")
        }

        val startTime = try {
            OffsetDateTime.parse(request.startTime)
        } catch (_: Exception) {
            throw ValidationException("Formato de data/hora de início inválido. Utilize o padrão ISO-8601.")
        }

        if (startTime <= OffsetDateTime.now()) {
            throw ValidationException("O horário do agendamento deve ser futuro.")
        }

        val endTime = startTime.plusMinutes(service.durationMinutes.toLong())

        // 1. Verificação ativa de conflito de agenda no repositório
        val overlapping = appointmentRepository.countOverlapping(professionalId, startTime, endTime)
        if (overlapping > 0) {
            throw ScheduleConflictException("O horário selecionado já foi reservado. Escolha outro horário.")
        }

        // 2. Geração do token criptograficamente seguro para cancelamento
        val cancellationToken = TokenGenerator.generateCancellationToken()

        val appointment = Appointment(
            id = UUID.randomUUID(),
            tenantId = tenant.id,
            professionalId = professionalId,
            serviceId = serviceId,
            customerName = request.customerName.trim(),
            customerEmail = request.customerEmail.trim(),
            customerPhone = request.customerPhone.trim(),
            startTime = startTime,
            endTime = endTime,
            status = AppointmentStatus.SCHEDULED,
            cancellationToken = cancellationToken,
            notes = request.notes?.trim()
        )

        val created = try {
            appointmentRepository.create(appointment)
        } catch (e: Exception) {
            logger.warn("Conflito detectado pelo banco de dados para agendamento do prof {}: {}", professionalId, e.message)
            throw ScheduleConflictException("O horário selecionado acabou de ser ocupado. Por favor, tente outro horário.")
        }

        // 3. Notificação assíncrona (desacoplada de I/O de rede imediato)
        notificationPort.sendAppointmentConfirmation(created, tenant, service, professional)

        return AppointmentConfirmationResponse(
            id = created.id.toString(),
            customerName = created.customerName,
            customerEmail = created.customerEmail,
            customerPhone = created.customerPhone,
            serviceName = service.name,
            professionalName = professional.name,
            startTime = created.startTime.toString(),
            endTime = created.endTime.toString(),
            status = created.status.name,
            cancellationToken = created.cancellationToken
        )
    }
}

class GetAppointmentByTokenUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val tenantRepository: TenantRepository,
    private val serviceRepository: ServiceRepository,
    private val professionalRepository: ProfessionalRepository
) {
    suspend fun execute(token: String): AppointmentDetailsDto {
        val appt = appointmentRepository.findByCancellationToken(token)
            ?: throw EntityNotFoundException("Agendamento não encontrado para este código.")

        val tenant = tenantRepository.findById(appt.tenantId)
            ?: throw EntityNotFoundException("Estabelecimento não encontrado.")
        val service = serviceRepository.findById(appt.serviceId)
            ?: throw EntityNotFoundException("Serviço não encontrado.")
        val prof = professionalRepository.findById(appt.professionalId)
            ?: throw EntityNotFoundException("Profissional não encontrado.")

        return AppointmentDetailsDto(
            id = appt.id.toString(),
            tenantName = tenant.name,
            serviceName = service.name,
            professionalName = prof.name,
            customerName = appt.customerName,
            startTime = appt.startTime.toString(),
            endTime = appt.endTime.toString(),
            status = appt.status.name
        )
    }
}

class CancelAppointmentUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val tenantRepository: TenantRepository,
    private val notificationPort: NotificationPort
) {
    suspend fun execute(token: String) {
        val appt = appointmentRepository.findByCancellationToken(token)
            ?: throw EntityNotFoundException("Agendamento não encontrado para este código.")

        if (appt.status == AppointmentStatus.CANCELLED) {
            throw InvalidCancellationException("Este agendamento já se encontra cancelado.")
        }

        if (appt.status == AppointmentStatus.COMPLETED) {
            throw InvalidCancellationException("Não é possível cancelar um agendamento já concluído.")
        }

        appointmentRepository.updateStatus(appt.id, AppointmentStatus.CANCELLED)

        val tenant = tenantRepository.findById(appt.tenantId)
        if (tenant != null) {
            notificationPort.sendAppointmentCancellation(appt, tenant)
        }
    }
}

package com.barbershop.domain.ports

import com.barbershop.domain.model.*
import java.time.OffsetDateTime
import java.util.UUID

interface TenantRepository {
    suspend fun findById(id: UUID): Tenant?
    suspend fun findBySlug(slug: String): Tenant?
    suspend fun findAll(): List<Tenant>
    suspend fun save(tenant: Tenant): Tenant
    suspend fun existsBySlug(slug: String): Boolean
}

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findByTenantId(tenantId: UUID): List<User>
    suspend fun save(user: User): User
    suspend fun existsByEmail(email: String): Boolean
}

interface ProfessionalRepository {
    suspend fun findById(id: UUID): Professional?
    suspend fun findByTenantId(tenantId: UUID, onlyActive: Boolean = true): List<Professional>
    suspend fun findByTenantAndService(tenantId: UUID, serviceId: UUID): List<Professional>
    suspend fun save(professional: Professional): Professional
    suspend fun associateService(professionalId: UUID, serviceId: UUID)
    suspend fun dissociateService(professionalId: UUID, serviceId: UUID)
}

interface ServiceRepository {
    suspend fun findById(id: UUID): ServiceItem?
    suspend fun findByTenantId(tenantId: UUID, onlyActive: Boolean = true): List<ServiceItem>
    suspend fun save(service: ServiceItem): ServiceItem
}

interface WorkingHoursRepository {
    suspend fun findByProfessionalId(professionalId: UUID): List<WorkingHours>
    suspend fun findByProfessionalAndDay(professionalId: UUID, dayOfWeek: Int): WorkingHours?
    suspend fun saveAll(professionalId: UUID, workingHours: List<WorkingHours>)
    suspend fun findBlocksByProfessionalAndPeriod(
        professionalId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime
    ): List<ScheduleBlock>
    suspend fun addBlock(block: ScheduleBlock): ScheduleBlock
}

interface AppointmentRepository {
    suspend fun findById(id: UUID): Appointment?
    suspend fun findByCancellationToken(token: String): Appointment?
    suspend fun findByTenantAndPeriod(
        tenantId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime
    ): List<Appointment>
    suspend fun findByProfessionalAndPeriod(
        professionalId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime
    ): List<Appointment>
    suspend fun create(appointment: Appointment): Appointment
    suspend fun updateStatus(id: UUID, status: AppointmentStatus): Appointment
    suspend fun countOverlapping(
        professionalId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime,
        excludeAppointmentId: UUID? = null
    ): Long
}

interface NotificationPort {
    suspend fun sendAppointmentConfirmation(appointment: Appointment, tenant: Tenant, service: ServiceItem, professional: Professional)
    suspend fun sendAppointmentCancellation(appointment: Appointment, tenant: Tenant)
}

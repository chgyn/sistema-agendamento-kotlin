package com.barbershop.domain.model

import java.math.BigDecimal
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

enum class UserRole {
    SUPER_ADMIN,
    TENANT_ADMIN,
    OPERATOR
}

enum class AppointmentStatus {
    SCHEDULED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}

data class Tenant(
    val id: UUID,
    val name: String,
    val slug: String,
    val phone: String?,
    val email: String?,
    val timezone: String = "America/Sao_Paulo",
    val active: Boolean = true,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)

data class User(
    val id: UUID,
    val tenantId: UUID?,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val active: Boolean = true,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

data class Professional(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val email: String?,
    val phone: String?,
    val bio: String?,
    val active: Boolean = true,
    val serviceIds: List<UUID> = emptyList(),
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

data class ServiceItem(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: BigDecimal,
    val active: Boolean = true,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

data class WorkingHours(
    val id: UUID,
    val professionalId: UUID,
    val dayOfWeek: Int, // 1=Segunda, 7=Domingo
    val startTime: LocalTime,
    val endTime: LocalTime,
    val breakStart: LocalTime?,
    val breakEnd: LocalTime?,
    val slotDurationMinutes: Int = 30
)

data class ScheduleBlock(
    val id: UUID,
    val professionalId: UUID,
    val reason: String,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime
)

data class TimeSlot(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val professionalId: UUID,
    val available: Boolean
)

data class Appointment(
    val id: UUID,
    val tenantId: UUID,
    val professionalId: UUID,
    val serviceId: UUID,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val cancellationToken: String,
    val notes: String? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)

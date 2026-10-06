package com.barbershop.application.dtos

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// --- Autenticação ---
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserProfileDto,
    val tenantSlug: String?
)

@Serializable
data class UserProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val tenantId: String?
)

// --- Área Pública do Agendamento ---
@Serializable
data class PublicTenantResponse(
    val id: String,
    val name: String,
    val slug: String,
    val phone: String?,
    val email: String?,
    val timezone: String,
    val services: List<PublicServiceDto>,
    val professionals: List<PublicProfessionalDto>
)

@Serializable
data class PublicServiceDto(
    val id: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: Double
)

@Serializable
data class PublicProfessionalDto(
    val id: String,
    val name: String,
    val bio: String?,
    val serviceIds: List<String>
)

@Serializable
data class AvailableSlotDto(
    val startTime: String,
    val endTime: String,
    val professionalId: String
)

@Serializable
data class CreatePublicAppointmentRequest(
    val professionalId: String,
    val serviceId: String,
    val startTime: String, // ISO-8601 (ex: "2026-10-10T14:30:00-03:00")
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val notes: String? = null
)

@Serializable
data class AppointmentConfirmationResponse(
    val id: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val serviceName: String,
    val professionalName: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val cancellationToken: String
)

@Serializable
data class AppointmentDetailsDto(
    val id: String,
    val tenantName: String,
    val serviceName: String,
    val professionalName: String,
    val customerName: String,
    val startTime: String,
    val endTime: String,
    val status: String
)

@Serializable
data class CancelAppointmentRequest(
    val reason: String? = null
)

// --- Painel Administrativo ---
@Serializable
data class AdminAppointmentDto(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val serviceId: String,
    val serviceName: String,
    val professionalId: String,
    val professionalName: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val notes: String?
)

@Serializable
data class UpdateAppointmentStatusRequest(
    val status: String
)

@Serializable
data class CreateProfessionalRequest(
    val name: String,
    val email: String?,
    val phone: String?,
    val bio: String?,
    val serviceIds: List<String> = emptyList()
)

@Serializable
data class WorkingHourInputDto(
    val dayOfWeek: Int, // 1..7
    val startTime: String, // "09:00"
    val endTime: String,   // "18:00"
    val breakStart: String? = null,
    val breakEnd: String? = null,
    val slotDurationMinutes: Int = 30
)

@Serializable
data class SetWorkingHoursRequest(
    val hours: List<WorkingHourInputDto>
)

@Serializable
data class WorkingHourResponseDto(
    val id: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val breakStart: String?,
    val breakEnd: String?,
    val slotDurationMinutes: Int
)

@Serializable
data class CreateServiceRequest(
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: Double
)

@Serializable
data class CreateTenantRequest(
    val name: String,
    val slug: String,
    val phone: String?,
    val email: String?,
    val adminName: String,
    val adminEmail: String,
    val adminPassword: String
)

@Serializable
data class TenantAdminDto(
    val id: String,
    val name: String,
    val slug: String,
    val phone: String?,
    val email: String?,
    val active: Boolean,
    val createdAt: String
)

// --- Padronização de Respostas e Erros ---
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: List<String> = emptyList()
)

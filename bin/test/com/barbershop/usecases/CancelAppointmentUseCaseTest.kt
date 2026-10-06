package com.barbershop.usecases

import com.barbershop.application.usecases.CancelAppointmentUseCase
import com.barbershop.domain.errors.EntityNotFoundException
import com.barbershop.domain.errors.InvalidCancellationException
import com.barbershop.domain.model.Appointment
import com.barbershop.domain.model.AppointmentStatus
import com.barbershop.domain.model.Tenant
import com.barbershop.domain.ports.AppointmentRepository
import com.barbershop.domain.ports.NotificationPort
import com.barbershop.domain.ports.TenantRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class CancelAppointmentUseCaseTest {
    private val appointmentRepository = mockk<AppointmentRepository>()
    private val tenantRepository = mockk<TenantRepository>()
    private val notificationPort = mockk<NotificationPort>(relaxed = true)

    private lateinit var useCase: CancelAppointmentUseCase

    private val token = "secure-cancellation-token-1234"
    private val tenantId = UUID.randomUUID()
    private val apptId = UUID.randomUUID()

    private val fakeTenant = Tenant(
        id = tenantId,
        name = "Barbearia Modelo",
        slug = "barbearia-modelo",
        phone = "11988887777",
        email = "modelo@barbearia.com",
        active = true
    )

    @BeforeEach
    fun setUp() {
        useCase = CancelAppointmentUseCase(appointmentRepository, tenantRepository, notificationPort)
    }

    @Test
    fun `deve cancelar agendamento com sucesso usando token valido`() = runBlocking {
        val appt = Appointment(
            id = apptId,
            tenantId = tenantId,
            professionalId = UUID.randomUUID(),
            serviceId = UUID.randomUUID(),
            customerName = "Lucas Lima",
            customerEmail = "lucas@email.com",
            customerPhone = "11999998888",
            startTime = OffsetDateTime.now().plusDays(1),
            endTime = OffsetDateTime.now().plusDays(1).plusMinutes(30),
            status = AppointmentStatus.SCHEDULED,
            cancellationToken = token
        )

        coEvery { appointmentRepository.findByCancellationToken(token) } returns appt
        coEvery { appointmentRepository.updateStatus(apptId, AppointmentStatus.CANCELLED) } returns appt.copy(status = AppointmentStatus.CANCELLED)
        coEvery { tenantRepository.findById(tenantId) } returns fakeTenant

        useCase.execute(token)

        coVerify(exactly = 1) { appointmentRepository.updateStatus(apptId, AppointmentStatus.CANCELLED) }
        coVerify(exactly = 1) { notificationPort.sendAppointmentCancellation(any(), any()) }
    }

    @Test
    fun `deve lancar InvalidCancellationException ao tentar cancelar agendamento ja cancelado`() = runBlocking {
        val alreadyCancelledAppt = Appointment(
            id = apptId,
            tenantId = tenantId,
            professionalId = UUID.randomUUID(),
            serviceId = UUID.randomUUID(),
            customerName = "Lucas Lima",
            customerEmail = "lucas@email.com",
            customerPhone = "11999998888",
            startTime = OffsetDateTime.now().plusDays(1),
            endTime = OffsetDateTime.now().plusDays(1).plusMinutes(30),
            status = AppointmentStatus.CANCELLED,
            cancellationToken = token
        )

        coEvery { appointmentRepository.findByCancellationToken(token) } returns alreadyCancelledAppt

        assertThrows(InvalidCancellationException::class.java) {
            runBlocking {
                useCase.execute(token)
            }
        }

        coVerify(exactly = 0) { appointmentRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `deve lancar EntityNotFoundException quando token nao for encontrado`() = runBlocking {
        coEvery { appointmentRepository.findByCancellationToken("token-inexistente") } returns null

        assertThrows(EntityNotFoundException::class.java) {
            runBlocking {
                useCase.execute("token-inexistente")
            }
        }
    }
}

package com.barbershop.usecases

import com.barbershop.application.dtos.CreatePublicAppointmentRequest
import com.barbershop.application.usecases.CreatePublicAppointmentUseCase
import com.barbershop.domain.errors.EntityNotFoundException
import com.barbershop.domain.errors.ScheduleConflictException
import com.barbershop.domain.model.Appointment
import com.barbershop.domain.model.AppointmentStatus
import com.barbershop.domain.model.Professional
import com.barbershop.domain.model.ServiceItem
import com.barbershop.domain.model.Tenant
import com.barbershop.domain.ports.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class CreatePublicAppointmentUseCaseTest {
    private val tenantRepository = mockk<TenantRepository>()
    private val serviceRepository = mockk<ServiceRepository>()
    private val professionalRepository = mockk<ProfessionalRepository>()
    private val appointmentRepository = mockk<AppointmentRepository>()
    private val notificationPort = mockk<NotificationPort>(relaxed = true)

    private lateinit var useCase: CreatePublicAppointmentUseCase

    private val tenantId = UUID.randomUUID()
    private val profId = UUID.randomUUID()
    private val serviceId = UUID.randomUUID()
    private val slug = "barbearia-vintage"

    private val fakeTenant = Tenant(
        id = tenantId,
        name = "Barbearia Vintage",
        slug = slug,
        phone = "11999998888",
        email = "contato@vintage.com",
        timezone = "America/Sao_Paulo",
        active = true
    )

    private val fakeService = ServiceItem(
        id = serviceId,
        tenantId = tenantId,
        name = "Corte Masculino",
        description = "Corte com tesoura e máquina",
        durationMinutes = 30,
        price = BigDecimal("45.00"),
        active = true
    )

    private val fakeProf = Professional(
        id = profId,
        tenantId = tenantId,
        name = "Carlos Barbeiro",
        email = "carlos@vintage.com",
        phone = "11988887777",
        bio = "Especialista em corte clássico",
        active = true,
        serviceIds = listOf(serviceId)
    )

    @BeforeEach
    fun setUp() {
        useCase = CreatePublicAppointmentUseCase(
            tenantRepository,
            serviceRepository,
            professionalRepository,
            appointmentRepository,
            notificationPort
        )
    }

    @Test
    fun `deve criar agendamento com sucesso quando horario estiver disponivel`() = runBlocking {
        val futureTime = OffsetDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0)
        val req = CreatePublicAppointmentRequest(
            professionalId = profId.toString(),
            serviceId = serviceId.toString(),
            startTime = futureTime.toString(),
            customerName = "João Silva",
            customerEmail = "joao@email.com",
            customerPhone = "11999990000",
            notes = "Sem navalha no pescoço"
        )

        coEvery { tenantRepository.findBySlug(slug) } returns fakeTenant
        coEvery { serviceRepository.findById(serviceId) } returns fakeService
        coEvery { professionalRepository.findById(profId) } returns fakeProf
        coEvery { appointmentRepository.countOverlapping(profId, any(), any()) } returns 0L
        coEvery { appointmentRepository.create(any()) } answers { firstArg() }

        val result = useCase.execute(slug, req)

        assertNotNull(result.id)
        assertEquals("João Silva", result.customerName)
        assertEquals("Corte Masculino", result.serviceName)
        assertEquals("Carlos Barbeiro", result.professionalName)
        assertEquals(AppointmentStatus.SCHEDULED.name, result.status)
        assertNotNull(result.cancellationToken)

        coVerify(exactly = 1) { appointmentRepository.create(any()) }
        coVerify(exactly = 1) { notificationPort.sendAppointmentConfirmation(any(), any(), any(), any()) }
    }

    @Test
    fun `deve lancar ScheduleConflictException quando houver sobreposicao de horarios`() = runBlocking {
        val futureTime = OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
        val req = CreatePublicAppointmentRequest(
            professionalId = profId.toString(),
            serviceId = serviceId.toString(),
            startTime = futureTime.toString(),
            customerName = "Pedro Santos",
            customerEmail = "pedro@email.com",
            customerPhone = "11988880000"
        )

        coEvery { tenantRepository.findBySlug(slug) } returns fakeTenant
        coEvery { serviceRepository.findById(serviceId) } returns fakeService
        coEvery { professionalRepository.findById(profId) } returns fakeProf
        coEvery { appointmentRepository.countOverlapping(profId, any(), any()) } returns 1L

        assertThrows(ScheduleConflictException::class.java) {
            runBlocking {
                useCase.execute(slug, req)
            }
        }

        coVerify(exactly = 0) { appointmentRepository.create(any()) }
    }

    @Test
    fun `deve lancar EntityNotFoundException quando estabelecimento nao for encontrado`() = runBlocking {
        coEvery { tenantRepository.findBySlug("inexistente") } returns null

        val req = CreatePublicAppointmentRequest(
            professionalId = profId.toString(),
            serviceId = serviceId.toString(),
            startTime = OffsetDateTime.now().plusDays(1).toString(),
            customerName = "Cliente",
            customerEmail = "cli@email.com",
            customerPhone = "11900000000"
        )

        assertThrows(EntityNotFoundException::class.java) {
            runBlocking {
                useCase.execute("inexistente", req)
            }
        }
    }
}

package com.barbershop.usecases

import com.barbershop.application.usecases.CalculateAvailableSlotsUseCase
import com.barbershop.domain.model.*
import com.barbershop.domain.ports.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class CalculateAvailableSlotsUseCaseTest {
    private val tenantRepository = mockk<TenantRepository>()
    private val serviceRepository = mockk<ServiceRepository>()
    private val professionalRepository = mockk<ProfessionalRepository>()
    private val workingHoursRepository = mockk<WorkingHoursRepository>()
    private val appointmentRepository = mockk<AppointmentRepository>()

    private lateinit var useCase: CalculateAvailableSlotsUseCase

    private val tenantId = UUID.randomUUID()
    private val profId = UUID.randomUUID()
    private val serviceId = UUID.randomUUID()
    private val slug = "vintage-saloon"

    private val fakeTenant = Tenant(
        id = tenantId,
        name = "Vintage Saloon",
        slug = slug,
        phone = "11988887777",
        email = "vintage@email.com",
        timezone = "America/Sao_Paulo",
        active = true
    )

    private val fakeService = ServiceItem(
        id = serviceId,
        tenantId = tenantId,
        name = "Barba",
        description = "Barboterapia",
        durationMinutes = 30,
        price = BigDecimal("35.00"),
        active = true
    )

    private val fakeProf = Professional(
        id = profId,
        tenantId = tenantId,
        name = "Marcos Barbeiro",
        email = "marcos@vintage.com",
        phone = "11999991111",
        bio = "Barbeiro",
        active = true,
        serviceIds = listOf(serviceId)
    )

    @BeforeEach
    fun setUp() {
        useCase = CalculateAvailableSlotsUseCase(
            tenantRepository,
            serviceRepository,
            professionalRepository,
            workingHoursRepository,
            appointmentRepository
        )
    }

    @Test
    fun `deve calcular slots livres desconsiderando intervalo de almoco e agendamentos existentes`() = runBlocking {
        val futureDate = LocalDate.now().plusDays(5)
        val zoneId = ZoneId.of("America/Sao_Paulo")

        // Trabalha das 09:00 às 12:00, almoço 12:00 às 13:00, depois 13:00 às 15:00
        val workingHours = WorkingHours(
            id = UUID.randomUUID(),
            professionalId = profId,
            dayOfWeek = futureDate.dayOfWeek.value,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(15, 0),
            breakStart = LocalTime.of(12, 0),
            breakEnd = LocalTime.of(13, 0),
            slotDurationMinutes = 30
        )

        // Agendamento existente das 09:30 às 10:00
        val existingApptStart = futureDate.atTime(9, 30).atZone(zoneId).toOffsetDateTime()
        val existingApptEnd = futureDate.atTime(10, 0).atZone(zoneId).toOffsetDateTime()
        val existingAppt = Appointment(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            professionalId = profId,
            serviceId = serviceId,
            customerName = "Cliente Antigo",
            customerEmail = "antigo@email.com",
            customerPhone = "11988880000",
            startTime = existingApptStart,
            endTime = existingApptEnd,
            status = AppointmentStatus.CONFIRMED,
            cancellationToken = "token123"
        )

        coEvery { tenantRepository.findBySlug(slug) } returns fakeTenant
        coEvery { serviceRepository.findById(serviceId) } returns fakeService
        coEvery { professionalRepository.findById(profId) } returns fakeProf
        coEvery { workingHoursRepository.findByProfessionalAndDay(profId, futureDate.dayOfWeek.value) } returns workingHours
        coEvery { appointmentRepository.findByProfessionalAndPeriod(profId, any(), any()) } returns listOf(existingAppt)
        coEvery { workingHoursRepository.findBlocksByProfessionalAndPeriod(profId, any(), any()) } returns emptyList()

        val slots = useCase.execute(slug, serviceId, futureDate, profId)

        // O slot de 09:30 NÃO deve estar presente (está ocupado pelo existingAppt)
        val hasOccupiedSlot = slots.any { it.startTime.contains("09:30") }
        assertFalse(hasOccupiedSlot, "Slot das 09:30 deveria estar ocupado")

        // O slot de 12:00 ou 12:30 NÃO deve estar presente (horário de almoço)
        val hasLunchSlot = slots.any { it.startTime.contains("12:00") || it.startTime.contains("12:30") }
        assertFalse(hasLunchSlot, "Horários de almoço não devem ser retornados como slots válidos")

        // O slot de 09:00 e 10:00 DEVEM estar presentes
        val has09Slot = slots.any { it.startTime.contains("09:00") }
        val has10Slot = slots.any { it.startTime.contains("10:00") }
        assertTrue(has09Slot, "Slot das 09:00 deveria estar disponível")
        assertTrue(has10Slot, "Slot das 10:00 deveria estar disponível")
    }
}

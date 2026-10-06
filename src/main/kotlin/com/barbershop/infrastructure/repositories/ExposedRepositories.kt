package com.barbershop.infrastructure.repositories

import com.barbershop.domain.model.*
import com.barbershop.domain.ports.*
import com.barbershop.infrastructure.database.DatabaseFactory.dbQuery
import com.barbershop.infrastructure.database.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ExposedTenantRepository : TenantRepository {
    override suspend fun findById(id: UUID): Tenant? = dbQuery {
        TenantsTable.selectAll()
            .where { TenantsTable.id eq id }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun findBySlug(slug: String): Tenant? = dbQuery {
        TenantsTable.selectAll()
            .where { TenantsTable.slug eq slug }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun findAll(): List<Tenant> = dbQuery {
        TenantsTable.selectAll()
            .orderBy(TenantsTable.name, SortOrder.ASC)
            .map { toDomain(it) }
    }

    override suspend fun save(tenant: Tenant): Tenant = dbQuery {
        val existing = TenantsTable.selectAll().where { TenantsTable.id eq tenant.id }.singleOrNull()
        if (existing == null) {
            TenantsTable.insert {
                it[id] = tenant.id
                it[name] = tenant.name
                it[slug] = tenant.slug
                it[phone] = tenant.phone
                it[email] = tenant.email
                it[timezone] = tenant.timezone
                it[active] = tenant.active
            }
        } else {
            TenantsTable.update({ TenantsTable.id eq tenant.id }) {
                it[name] = tenant.name
                it[slug] = tenant.slug
                it[phone] = tenant.phone
                it[email] = tenant.email
                it[timezone] = tenant.timezone
                it[active] = tenant.active
                it[updatedAt] = OffsetDateTime.now()
            }
        }
        tenant
    }

    override suspend fun existsBySlug(slug: String): Boolean = dbQuery {
        TenantsTable.selectAll().where { TenantsTable.slug eq slug }.count() > 0
    }

    private fun toDomain(row: ResultRow): Tenant = Tenant(
        id = row[TenantsTable.id],
        name = row[TenantsTable.name],
        slug = row[TenantsTable.slug],
        phone = row[TenantsTable.phone],
        email = row[TenantsTable.email],
        timezone = row[TenantsTable.timezone],
        active = row[TenantsTable.active],
        createdAt = row[TenantsTable.createdAt],
        updatedAt = row[TenantsTable.updatedAt]
    )
}

class ExposedUserRepository : UserRepository {
    override suspend fun findById(id: UUID): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun findByTenantId(tenantId: UUID): List<User> = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.tenantId eq tenantId }
            .map { toDomain(it) }
    }

    override suspend fun save(user: User): User = dbQuery {
        val existing = UsersTable.selectAll().where { UsersTable.id eq user.id }.singleOrNull()
        if (existing == null) {
            UsersTable.insert {
                it[id] = user.id
                it[tenantId] = user.tenantId
                it[name] = user.name
                it[email] = user.email
                it[passwordHash] = user.passwordHash
                it[role] = user.role.name
                it[active] = user.active
            }
        } else {
            UsersTable.update({ UsersTable.id eq user.id }) {
                it[tenantId] = user.tenantId
                it[name] = user.name
                it[email] = user.email
                it[passwordHash] = user.passwordHash
                it[role] = user.role.name
                it[active] = user.active
                it[updatedAt] = OffsetDateTime.now()
            }
        }
        user
    }

    override suspend fun existsByEmail(email: String): Boolean = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }.count() > 0
    }

    private fun toDomain(row: ResultRow): User = User(
        id = row[UsersTable.id],
        tenantId = row[UsersTable.tenantId],
        name = row[UsersTable.name],
        email = row[UsersTable.email],
        passwordHash = row[UsersTable.passwordHash],
        role = UserRole.valueOf(row[UsersTable.role]),
        active = row[UsersTable.active],
        createdAt = row[UsersTable.createdAt]
    )
}

class ExposedProfessionalRepository : ProfessionalRepository {
    override suspend fun findById(id: UUID): Professional? = dbQuery {
        val row = ProfessionalsTable.selectAll()
            .where { ProfessionalsTable.id eq id }
            .singleOrNull() ?: return@dbQuery null

        val serviceIds = ProfessionalServicesTable
            .selectAll()
            .where { ProfessionalServicesTable.professionalId eq id }
            .map { it[ProfessionalServicesTable.serviceId] }

        toDomain(row, serviceIds)
    }

    override suspend fun findByTenantId(tenantId: UUID, onlyActive: Boolean): List<Professional> = dbQuery {
        val query = ProfessionalsTable.selectAll().where { ProfessionalsTable.tenantId eq tenantId }
        if (onlyActive) {
            query.andWhere { ProfessionalsTable.active eq true }
        }
        val professionalsRows = query.orderBy(ProfessionalsTable.name, SortOrder.ASC).toList()
        if (professionalsRows.isEmpty()) return@dbQuery emptyList()

        val profIds = professionalsRows.map { it[ProfessionalsTable.id] }
        val servicesMap = ProfessionalServicesTable.selectAll()
            .where { ProfessionalServicesTable.professionalId inList profIds }
            .groupBy({ it[ProfessionalServicesTable.professionalId] }, { it[ProfessionalServicesTable.serviceId] })

        professionalsRows.map { row ->
            toDomain(row, servicesMap[row[ProfessionalsTable.id]] ?: emptyList())
        }
    }

    override suspend fun findByTenantAndService(tenantId: UUID, serviceId: UUID): List<Professional> = dbQuery {
        (ProfessionalsTable innerJoin ProfessionalServicesTable)
            .selectAll()
            .where {
                (ProfessionalsTable.tenantId eq tenantId) and
                (ProfessionalsTable.active eq true) and
                (ProfessionalServicesTable.serviceId eq serviceId)
            }
            .orderBy(ProfessionalsTable.name, SortOrder.ASC)
            .map { toDomain(it, listOf(serviceId)) }
    }

    override suspend fun save(professional: Professional): Professional = dbQuery {
        val existing = ProfessionalsTable.selectAll().where { ProfessionalsTable.id eq professional.id }.singleOrNull()
        if (existing == null) {
            ProfessionalsTable.insert {
                it[id] = professional.id
                it[tenantId] = professional.tenantId
                it[name] = professional.name
                it[email] = professional.email
                it[phone] = professional.phone
                it[bio] = professional.bio
                it[active] = professional.active
            }
        } else {
            ProfessionalsTable.update({ ProfessionalsTable.id eq professional.id }) {
                it[name] = professional.name
                it[email] = professional.email
                it[phone] = professional.phone
                it[bio] = professional.bio
                it[active] = professional.active
            }
        }
        professional
    }

    override suspend fun associateService(professionalId: UUID, serviceId: UUID): Unit = dbQuery {
        ProfessionalServicesTable.insertIgnore {
            it[this.professionalId] = professionalId
            it[this.serviceId] = serviceId
        }
    }

    override suspend fun dissociateService(professionalId: UUID, serviceId: UUID): Unit = dbQuery {
        ProfessionalServicesTable.deleteWhere {
            (ProfessionalServicesTable.professionalId eq professionalId) and
            (ProfessionalServicesTable.serviceId eq serviceId)
        }
    }

    private fun toDomain(row: ResultRow, serviceIds: List<UUID>): Professional = Professional(
        id = row[ProfessionalsTable.id],
        tenantId = row[ProfessionalsTable.tenantId],
        name = row[ProfessionalsTable.name],
        email = row[ProfessionalsTable.email],
        phone = row[ProfessionalsTable.phone],
        bio = row[ProfessionalsTable.bio],
        active = row[ProfessionalsTable.active],
        serviceIds = serviceIds,
        createdAt = row[ProfessionalsTable.createdAt]
    )
}

class ExposedServiceRepository : ServiceRepository {
    override suspend fun findById(id: UUID): ServiceItem? = dbQuery {
        ServicesTable.selectAll()
            .where { ServicesTable.id eq id }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun findByTenantId(tenantId: UUID, onlyActive: Boolean): List<ServiceItem> = dbQuery {
        val query = ServicesTable.selectAll().where { ServicesTable.tenantId eq tenantId }
        if (onlyActive) {
            query.andWhere { ServicesTable.active eq true }
        }
        query.orderBy(ServicesTable.name, SortOrder.ASC).map { toDomain(it) }
    }

    override suspend fun save(service: ServiceItem): ServiceItem = dbQuery {
        val existing = ServicesTable.selectAll().where { ServicesTable.id eq service.id }.singleOrNull()
        if (existing == null) {
            ServicesTable.insert {
                it[id] = service.id
                it[tenantId] = service.tenantId
                it[name] = service.name
                it[description] = service.description
                it[durationMinutes] = service.durationMinutes
                it[price] = service.price
                it[active] = service.active
            }
        } else {
            ServicesTable.update({ ServicesTable.id eq service.id }) {
                it[name] = service.name
                it[description] = service.description
                it[durationMinutes] = service.durationMinutes
                it[price] = service.price
                it[active] = service.active
            }
        }
        service
    }

    private fun toDomain(row: ResultRow): ServiceItem = ServiceItem(
        id = row[ServicesTable.id],
        tenantId = row[ServicesTable.tenantId],
        name = row[ServicesTable.name],
        description = row[ServicesTable.description],
        durationMinutes = row[ServicesTable.durationMinutes],
        price = row[ServicesTable.price],
        active = row[ServicesTable.active],
        createdAt = row[ServicesTable.createdAt]
    )
}

class ExposedWorkingHoursRepository : WorkingHoursRepository {
    override suspend fun findByProfessionalId(professionalId: UUID): List<WorkingHours> = dbQuery {
        WorkingHoursTable.selectAll()
            .where { WorkingHoursTable.professionalId eq professionalId }
            .orderBy(WorkingHoursTable.dayOfWeek, SortOrder.ASC)
            .map { toDomain(it) }
    }

    override suspend fun findByProfessionalAndDay(professionalId: UUID, dayOfWeek: Int): WorkingHours? = dbQuery {
        WorkingHoursTable.selectAll()
            .where {
                (WorkingHoursTable.professionalId eq professionalId) and
                (WorkingHoursTable.dayOfWeek eq dayOfWeek.toShort())
            }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun saveAll(professionalId: UUID, workingHours: List<WorkingHours>): Unit = dbQuery {
        WorkingHoursTable.deleteWhere { WorkingHoursTable.professionalId eq professionalId }
        for (wh in workingHours) {
            WorkingHoursTable.insert {
                it[id] = wh.id
                it[this.professionalId] = professionalId
                it[dayOfWeek] = wh.dayOfWeek.toShort()
                it[startTime] = wh.startTime
                it[endTime] = wh.endTime
                it[breakStart] = wh.breakStart
                it[breakEnd] = wh.breakEnd
                it[slotDurationMinutes] = wh.slotDurationMinutes.toShort()
            }
        }
    }

    override suspend fun findBlocksByProfessionalAndPeriod(
        professionalId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime
    ): List<ScheduleBlock> = dbQuery {
        ScheduleBlocksTable.selectAll()
            .where {
                (ScheduleBlocksTable.professionalId eq professionalId) and
                (ScheduleBlocksTable.startTime less end) and
                (ScheduleBlocksTable.endTime greater start)
            }
            .map {
                ScheduleBlock(
                    id = it[ScheduleBlocksTable.id],
                    professionalId = it[ScheduleBlocksTable.professionalId],
                    reason = it[ScheduleBlocksTable.reason],
                    startTime = it[ScheduleBlocksTable.startTime],
                    endTime = it[ScheduleBlocksTable.endTime]
                )
            }
    }

    override suspend fun addBlock(block: ScheduleBlock): ScheduleBlock = dbQuery {
        ScheduleBlocksTable.insert {
            it[id] = block.id
            it[professionalId] = block.professionalId
            it[reason] = block.reason
            it[startTime] = block.startTime
            it[endTime] = block.endTime
        }
        block
    }

    private fun toDomain(row: ResultRow): WorkingHours = WorkingHours(
        id = row[WorkingHoursTable.id],
        professionalId = row[WorkingHoursTable.professionalId],
        dayOfWeek = row[WorkingHoursTable.dayOfWeek].toInt(),
        startTime = row[WorkingHoursTable.startTime],
        endTime = row[WorkingHoursTable.endTime],
        breakStart = row[WorkingHoursTable.breakStart],
        breakEnd = row[WorkingHoursTable.breakEnd],
        slotDurationMinutes = row[WorkingHoursTable.slotDurationMinutes].toInt()
    )
}

class ExposedAppointmentRepository : AppointmentRepository {
    override suspend fun findById(id: UUID): Appointment? = dbQuery {
        AppointmentsTable.selectAll()
            .where { AppointmentsTable.id eq id }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun findByCancellationToken(token: String): Appointment? = dbQuery {
        AppointmentsTable.selectAll()
            .where { AppointmentsTable.cancellationToken eq token }
            .map { toDomain(it) }
            .singleOrNull()
    }

    override suspend fun findByTenantAndPeriod(
        tenantId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime
    ): List<Appointment> = dbQuery {
        AppointmentsTable.selectAll()
            .where {
                (AppointmentsTable.tenantId eq tenantId) and
                (AppointmentsTable.startTime greaterEq start) and
                (AppointmentsTable.startTime less end)
            }
            .orderBy(AppointmentsTable.startTime, SortOrder.ASC)
            .map { toDomain(it) }
    }

    override suspend fun findByProfessionalAndPeriod(
        professionalId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime
    ): List<Appointment> = dbQuery {
        AppointmentsTable.selectAll()
            .where {
                (AppointmentsTable.professionalId eq professionalId) and
                (AppointmentsTable.startTime less end) and
                (AppointmentsTable.endTime greater start) and
                (AppointmentsTable.status inList listOf("SCHEDULED", "CONFIRMED"))
            }
            .orderBy(AppointmentsTable.startTime, SortOrder.ASC)
            .map { toDomain(it) }
    }

    override suspend fun create(appointment: Appointment): Appointment = dbQuery {
        AppointmentsTable.insert {
            it[id] = appointment.id
            it[tenantId] = appointment.tenantId
            it[professionalId] = appointment.professionalId
            it[serviceId] = appointment.serviceId
            it[customerName] = appointment.customerName
            it[customerEmail] = appointment.customerEmail
            it[customerPhone] = appointment.customerPhone
            it[startTime] = appointment.startTime
            it[endTime] = appointment.endTime
            it[status] = appointment.status.name
            it[cancellationToken] = appointment.cancellationToken
            it[notes] = appointment.notes
        }
        appointment
    }

    override suspend fun updateStatus(id: UUID, status: AppointmentStatus): Appointment = dbQuery {
        AppointmentsTable.update({ AppointmentsTable.id eq id }) {
            it[AppointmentsTable.status] = status.name
            it[updatedAt] = OffsetDateTime.now()
        }
        AppointmentsTable.selectAll().where { AppointmentsTable.id eq id }.map { toDomain(it) }.single()
    }

    override suspend fun countOverlapping(
        professionalId: UUID,
        start: OffsetDateTime,
        end: OffsetDateTime,
        excludeAppointmentId: UUID?
    ): Long = dbQuery {
        val query = AppointmentsTable.selectAll().where {
            (AppointmentsTable.professionalId eq professionalId) and
            (AppointmentsTable.startTime less end) and
            (AppointmentsTable.endTime greater start) and
            (AppointmentsTable.status inList listOf("SCHEDULED", "CONFIRMED"))
        }
        if (excludeAppointmentId != null) {
            query.andWhere { AppointmentsTable.id neq excludeAppointmentId }
        }
        query.count()
    }

    private fun toDomain(row: ResultRow): Appointment = Appointment(
        id = row[AppointmentsTable.id],
        tenantId = row[AppointmentsTable.tenantId],
        professionalId = row[AppointmentsTable.professionalId],
        serviceId = row[AppointmentsTable.serviceId],
        customerName = row[AppointmentsTable.customerName],
        customerEmail = row[AppointmentsTable.customerEmail],
        customerPhone = row[AppointmentsTable.customerPhone],
        startTime = row[AppointmentsTable.startTime],
        endTime = row[AppointmentsTable.endTime],
        status = AppointmentStatus.valueOf(row[AppointmentsTable.status]),
        cancellationToken = row[AppointmentsTable.cancellationToken],
        notes = row[AppointmentsTable.notes],
        createdAt = row[AppointmentsTable.createdAt],
        updatedAt = row[AppointmentsTable.updatedAt]
    )
}

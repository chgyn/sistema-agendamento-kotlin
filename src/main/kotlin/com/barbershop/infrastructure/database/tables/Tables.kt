package com.barbershop.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.OffsetDateTime

object TenantsTable : Table("tenants") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 150)
    val slug = varchar("slug", 100).uniqueIndex()
    val phone = varchar("phone", 30).nullable()
    val email = varchar("email", 150).nullable()
    val timezone = varchar("timezone", 50).default("America/Sao_Paulo")
    val active = bool("active").default(true)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val updatedAt = timestampWithTimeZone("updated_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val tenantId = reference("tenant_id", TenantsTable.id).nullable()
    val name = varchar("name", 120)
    val email = varchar("email", 150).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 30)
    val active = bool("active").default(true)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val updatedAt = timestampWithTimeZone("updated_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

object ProfessionalsTable : Table("professionals") {
    val id = uuid("id").autoGenerate()
    val tenantId = reference("tenant_id", TenantsTable.id)
    val name = varchar("name", 120)
    val email = varchar("email", 150).nullable()
    val phone = varchar("phone", 30).nullable()
    val bio = text("bio").nullable()
    val active = bool("active").default(true)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

object ServicesTable : Table("services") {
    val id = uuid("id").autoGenerate()
    val tenantId = reference("tenant_id", TenantsTable.id)
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val durationMinutes = integer("duration_minutes")
    val price = decimal("price", 10, 2)
    val active = bool("active").default(true)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

object ProfessionalServicesTable : Table("professional_services") {
    val professionalId = reference("professional_id", ProfessionalsTable.id)
    val serviceId = reference("service_id", ServicesTable.id)

    override val primaryKey = PrimaryKey(professionalId, serviceId)
}

object WorkingHoursTable : Table("working_hours") {
    val id = uuid("id").autoGenerate()
    val professionalId = reference("professional_id", ProfessionalsTable.id)
    val dayOfWeek = short("day_of_week")
    val startTime = time("start_time")
    val endTime = time("end_time")
    val breakStart = time("break_start").nullable()
    val breakEnd = time("break_end").nullable()
    val slotDurationMinutes = short("slot_duration_minutes").default(30)

    override val primaryKey = PrimaryKey(id)
}

object ScheduleBlocksTable : Table("schedule_blocks") {
    val id = uuid("id").autoGenerate()
    val professionalId = reference("professional_id", ProfessionalsTable.id)
    val reason = varchar("reason", 255)
    val startTime = timestampWithTimeZone("start_time")
    val endTime = timestampWithTimeZone("end_time")

    override val primaryKey = PrimaryKey(id)
}

object AppointmentsTable : Table("appointments") {
    val id = uuid("id").autoGenerate()
    val tenantId = reference("tenant_id", TenantsTable.id)
    val professionalId = reference("professional_id", ProfessionalsTable.id)
    val serviceId = reference("service_id", ServicesTable.id)
    val customerName = varchar("customer_name", 120)
    val customerEmail = varchar("customer_email", 150)
    val customerPhone = varchar("customer_phone", 30)
    val startTime = timestampWithTimeZone("start_time")
    val endTime = timestampWithTimeZone("end_time")
    val status = varchar("status", 30).default("SCHEDULED")
    val cancellationToken = varchar("cancellation_token", 64).uniqueIndex()
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val updatedAt = timestampWithTimeZone("updated_at").clientDefault { OffsetDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}

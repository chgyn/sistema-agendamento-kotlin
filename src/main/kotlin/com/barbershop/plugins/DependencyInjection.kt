package com.barbershop.plugins

import com.barbershop.application.usecases.*
import com.barbershop.domain.ports.*
import com.barbershop.infrastructure.jobs.AsyncNotificationService
import com.barbershop.infrastructure.repositories.*
import com.barbershop.infrastructure.security.JwtConfig
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

val appModule = module {
    // Segurança & JWT
    single {
        JwtConfig(
            secret = "barbershop-ultra-secure-jwt-secret-key-2026-production",
            issuer = "https://barbershop.local",
            audience = "barbershop-users",
            realm = "BarbershopRealm"
        )
    }

    // Notificações Assíncronas (Structured Concurrency)
    single<NotificationPort> { AsyncNotificationService() }

    // Repositórios Exposed
    single<TenantRepository> { ExposedTenantRepository() }
    single<UserRepository> { ExposedUserRepository() }
    single<ProfessionalRepository> { ExposedProfessionalRepository() }
    single<ServiceRepository> { ExposedServiceRepository() }
    single<WorkingHoursRepository> { ExposedWorkingHoursRepository() }
    single<AppointmentRepository> { ExposedAppointmentRepository() }

    // Casos de Uso
    single { AuthenticateUserUseCase(get(), get(), get()) }
    single { GetPublicTenantDetailsUseCase(get(), get(), get()) }
    single { CalculateAvailableSlotsUseCase(get(), get(), get(), get(), get()) }
    single { CreatePublicAppointmentUseCase(get(), get(), get(), get(), get()) }
    single { GetAppointmentByTokenUseCase(get(), get(), get(), get()) }
    single { CancelAppointmentUseCase(get(), get(), get()) }
    single { ListAdminAppointmentsUseCase(get(), get(), get(), get()) }
    single { UpdateAppointmentStatusUseCase(get()) }
    single { ManageProfessionalsUseCase(get(), get()) }
    single { ManageServicesUseCase(get()) }
    single { CreateTenantUseCase(get(), get()) }
}

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}

inline fun <reified T : Any> koinInject(): Lazy<T> = lazy {
    org.koin.core.context.GlobalContext.get().get<T>()
}


package com.barbershop.application.usecases

import com.barbershop.application.dtos.LoginRequest
import com.barbershop.application.dtos.LoginResponse
import com.barbershop.application.dtos.UserProfileDto
import com.barbershop.domain.errors.EntityNotFoundException
import com.barbershop.domain.errors.ValidationException
import com.barbershop.domain.model.UserRole
import com.barbershop.domain.ports.TenantRepository
import com.barbershop.domain.ports.UserRepository
import com.barbershop.infrastructure.security.JwtConfig
import com.barbershop.infrastructure.security.PasswordHasher

class AuthenticateUserUseCase(
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository,
    private val jwtConfig: JwtConfig
) {
    suspend fun execute(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw EntityNotFoundException("Credenciais inválidas.")

        if (!user.active) {
            throw ValidationException("Usuário inativo. Contate o administrador.")
        }

        if (!PasswordHasher.verify(request.password, user.passwordHash)) {
            throw EntityNotFoundException("Credenciais inválidas.")
        }

        val token = jwtConfig.generateToken(user)

        var tenantSlug: String? = null
        if (user.tenantId != null) {
            val tenant = tenantRepository.findById(user.tenantId)
            tenantSlug = tenant?.slug
        }

        return LoginResponse(
            token = token,
            user = UserProfileDto(
                id = user.id.toString(),
                name = user.name,
                email = user.email,
                role = user.role.name,
                tenantId = user.tenantId?.toString()
            ),
            tenantSlug = tenantSlug
        )
    }
}

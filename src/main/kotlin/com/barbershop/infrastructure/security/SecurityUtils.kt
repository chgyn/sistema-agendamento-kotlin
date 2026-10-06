package com.barbershop.infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.barbershop.domain.model.User
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

object PasswordHasher {
    private const val BCRYPT_LOG_ROUNDS = 12

    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_LOG_ROUNDS))
    }

    fun verify(password: String, hashed: String): Boolean {
        return try {
            BCrypt.checkpw(password, hashed)
        } catch (_: Exception) {
            false
        }
    }
}

object TokenGenerator {
    private val secureRandom = SecureRandom()
    private val hexDigits = "0123456789abcdef".toCharArray()

    fun generateCancellationToken(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        val sb = StringBuilder(48)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            sb.append(hexDigits[i ushr 4])
            sb.append(hexDigits[i and 0x0F])
        }
        return sb.toString()
    }
}

class JwtConfig(
    private val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    private val validityDurationMs: Long = 36_000_000L // 10 horas
) {
    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun generateToken(user: User): String {
        val now = Date()
        val expiresAt = Date(now.time + validityDurationMs)

        val builder = JWT.create()
            .withSubject(user.id.toString())
            .withIssuer(issuer)
            .withAudience(audience)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)

        if (user.tenantId != null) {
            builder.withClaim("tenantId", user.tenantId.toString())
        }

        return builder.sign(algorithm)
    }
}

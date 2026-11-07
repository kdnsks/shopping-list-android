package ru.kdnsks.shoppinglist.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.Date

class JwtConfig(
    secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    private val ttlHours: Long,
) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    val ttlSeconds: Long get() = ttlHours * 3600

    fun createToken(userId: Long, email: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(email)
        .withClaim(USER_ID_CLAIM, userId)
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + ttlSeconds * 1000))
        .sign(algorithm)

    companion object {
        const val USER_ID_CLAIM = "userId"
    }
}

/** Идентификатор пользователя из проверенного JWT-токена. */
val ApplicationCall.userId: Long
    get() = principal<JWTPrincipal>()!!.payload.getClaim(JwtConfig.USER_ID_CLAIM).asLong()

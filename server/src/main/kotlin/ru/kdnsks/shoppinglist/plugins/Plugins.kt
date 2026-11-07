package ru.kdnsks.shoppinglist.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import ru.kdnsks.shoppinglist.auth.JwtConfig
import ru.kdnsks.shoppinglist.dto.ErrorResponse
import ru.kdnsks.shoppinglist.util.AppException

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        format { call -> "${call.request.httpMethod.value} ${call.request.path()} -> ${call.response.status()}" }
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            explicitNulls = true
            encodeDefaults = true // иначе поля со значением по умолчанию (tokenType) не попадают в ответ
        })
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AppException> { call, e ->
            call.respond(e.status, ErrorResponse(e.code, e.message))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "Некорректный формат запроса"))
        }
        exception<Throwable> { call, e ->
            call.application.environment.log.error("Unhandled error", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Внутренняя ошибка сервера"))
        }
    }
}

fun Application.configureSecurity(jwt: JwtConfig) {
    authentication {
        jwt("auth-jwt") {
            realm = jwt.realm
            verifier(jwt.verifier)
            validate { credential ->
                if (credential.payload.getClaim(JwtConfig.USER_ID_CLAIM).asLong() != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Требуется авторизация: токен отсутствует, неверен или истёк"))
            }
        }
    }
}

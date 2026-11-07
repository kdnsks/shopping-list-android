package ru.kdnsks.shoppinglist.util

import io.ktor.http.HttpStatusCode

open class AppException(val status: HttpStatusCode, val code: String, override val message: String) : RuntimeException(message)

class ValidationException(message: String) : AppException(HttpStatusCode.BadRequest, "validation_error", message)
class NotFoundException(message: String) : AppException(HttpStatusCode.NotFound, "not_found", message)
class ConflictException(message: String) : AppException(HttpStatusCode.Conflict, "conflict", message)
class UnauthorizedException(message: String) : AppException(HttpStatusCode.Unauthorized, "unauthorized", message)

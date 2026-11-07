package ru.kdnsks.shoppinglist.service

import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.mindrot.jbcrypt.BCrypt
import ru.kdnsks.shoppinglist.auth.JwtConfig
import ru.kdnsks.shoppinglist.db.Categories
import ru.kdnsks.shoppinglist.db.Users
import ru.kdnsks.shoppinglist.db.dbQuery
import ru.kdnsks.shoppinglist.dto.LoginRequest
import ru.kdnsks.shoppinglist.dto.RegisterRequest
import ru.kdnsks.shoppinglist.dto.TokenResponse
import ru.kdnsks.shoppinglist.dto.UserDto
import ru.kdnsks.shoppinglist.util.ConflictException
import ru.kdnsks.shoppinglist.util.NotFoundException
import ru.kdnsks.shoppinglist.util.UnauthorizedException
import java.time.OffsetDateTime

class AuthService(private val jwt: JwtConfig) {

    /** Категории, которые создаются каждому новому пользователю. */
    private val defaultCategories = listOf(
        "Овощи и фрукты", "Молочные продукты", "Мясо и рыба", "Хлеб и выпечка",
        "Бакалея", "Напитки", "Бытовая химия", "Прочее",
    )

    suspend fun register(req: RegisterRequest): TokenResponse {
        val email = Validation.email(req.email)
        Validation.password(req.password)
        val hash = BCrypt.hashpw(req.password, BCrypt.gensalt(10))
        val userId = dbQuery {
            if (Users.selectAll().where { Users.email eq email }.any())
                throw ConflictException("Пользователь с таким e-mail уже зарегистрирован")
            val id = Users.insertAndGetId {
                it[Users.email] = email
                it[passwordHash] = hash
                it[createdAt] = OffsetDateTime.now()
            }.value
            Categories.batchInsert(defaultCategories.withIndex()) { (index, name) ->
                this[Categories.userId] = id
                this[Categories.name] = name
                this[Categories.sortOrder] = index + 1
            }
            id
        }
        return token(userId, email)
    }

    suspend fun login(req: LoginRequest): TokenResponse {
        val email = req.email.trim().lowercase()
        val user = dbQuery {
            Users.selectAll().where { Users.email eq email }.singleOrNull()
        }
        // Одинаковый ответ для неверного e-mail и неверного пароля: не раскрываем, какие e-mail зарегистрированы
        if (user == null || !BCrypt.checkpw(req.password, user[Users.passwordHash]))
            throw UnauthorizedException("Неверный e-mail или пароль")
        return token(user[Users.id].value, email)
    }

    suspend fun me(userId: Long): UserDto = dbQuery {
        Users.selectAll().where { Users.id eq userId }.singleOrNull()
            ?.let { UserDto(it[Users.id].value, it[Users.email]) }
            ?: throw NotFoundException("Пользователь не найден")
    }

    private fun token(userId: Long, email: String) = TokenResponse(
        token = jwt.createToken(userId, email),
        expiresIn = jwt.ttlSeconds,
        user = UserDto(userId, email),
    )
}

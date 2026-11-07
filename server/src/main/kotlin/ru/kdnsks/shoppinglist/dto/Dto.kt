package ru.kdnsks.shoppinglist.dto

import kotlinx.serialization.Serializable
import ru.kdnsks.shoppinglist.util.BigDecimalSerializer
import java.math.BigDecimal

typealias Money = @Serializable(with = BigDecimalSerializer::class) BigDecimal

@Serializable
data class ErrorResponse(val error: String, val message: String)

// ---------- Аутентификация ----------
@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class UserDto(val id: Long, val email: String)

@Serializable
data class TokenResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserDto,
)

// ---------- Категории ----------
@Serializable
data class CategoryRequest(val name: String, val sortOrder: Int? = null)

@Serializable
data class CategoryDto(val id: Long, val name: String, val sortOrder: Int)

// ---------- Списки ----------
@Serializable
data class ListRequest(val name: String, val budget: Money? = null)

@Serializable
data class ListDto(
    val id: Long,
    val name: String,
    val budget: Money?,
    val itemsCount: Int,
    val total: Money,
    val createdAt: String,
    val updatedAt: String,
)

// ---------- Позиции ----------
@Serializable
data class ItemRequest(
    val name: String,
    val quantity: Money = BigDecimal.ONE,
    val unit: String = "шт",
    val price: Money? = null,
    val categoryId: Long? = null,
)

@Serializable
data class PurchasedRequest(val purchased: Boolean)

@Serializable
data class ItemDto(
    val id: Long,
    val listId: Long,
    val categoryId: Long?,
    val categoryName: String,
    val name: String,
    val quantity: Money,
    val unit: String,
    val price: Money?,
    val lineTotal: Money?,
    val purchased: Boolean,
)

// ---------- Итоги (ключевая функция) ----------
@Serializable
data class CategoryTotalDto(val categoryId: Long?, val categoryName: String, val total: Money)

@Serializable
data class SummaryDto(
    val listId: Long,
    val itemsCount: Int,
    val itemsWithoutPrice: Int,
    val total: Money,
    val inCart: Money,
    val remaining: Money,
    val budget: Money?,
    val budgetRemaining: Money?,
    val overBudget: Boolean,
    val byCategory: List<CategoryTotalDto>,
)

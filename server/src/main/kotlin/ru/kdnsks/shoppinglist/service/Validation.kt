package ru.kdnsks.shoppinglist.service

import ru.kdnsks.shoppinglist.dto.ItemRequest
import ru.kdnsks.shoppinglist.util.ValidationException
import java.math.BigDecimal

object Validation {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    /** Единицы измерения; для штучных разрешено только целое количество. */
    val UNITS = setOf("шт", "уп", "кг", "г", "л", "мл")
    private val PIECE_UNITS = setOf("шт", "уп")

    fun email(value: String): String {
        val email = value.trim().lowercase()
        if (!EMAIL.matches(email)) throw ValidationException("Некорректный e-mail")
        return email
    }

    fun password(value: String) {
        if (value.length < 8) throw ValidationException("Пароль должен содержать не менее 8 символов")
    }

    fun name(value: String, field: String, max: Int): String {
        val name = value.trim()
        if (name.isEmpty()) throw ValidationException("Поле «$field» не может быть пустым")
        if (name.length > max) throw ValidationException("Поле «$field» длиннее $max символов")
        return name
    }

    fun money(value: BigDecimal?, field: String): BigDecimal? {
        if (value == null) return null
        if (value.signum() < 0) throw ValidationException("Поле «$field» не может быть отрицательным")
        if (value.stripTrailingZeros().scale() > 2) throw ValidationException("Поле «$field»: не более 2 знаков после запятой")
        return value.setScale(2)
    }

    fun item(req: ItemRequest): ItemRequest {
        val unit = req.unit.trim().lowercase()
        if (unit !in UNITS) throw ValidationException("Единица измерения должна быть одной из: ${UNITS.joinToString()}")
        val qty = req.quantity
        if (qty.signum() <= 0) throw ValidationException("Количество должно быть больше нуля")
        if (qty.stripTrailingZeros().scale() > 3) throw ValidationException("Количество: не более 3 знаков после запятой")
        if (unit in PIECE_UNITS && qty.stripTrailingZeros().scale() > 0)
            throw ValidationException("Для единицы «$unit» количество должно быть целым")
        return req.copy(
            name = name(req.name, "Название", 150),
            unit = unit,
            quantity = qty.setScale(3),
            price = money(req.price, "Цена"),
        )
    }
}

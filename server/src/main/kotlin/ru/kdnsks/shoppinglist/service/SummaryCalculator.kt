package ru.kdnsks.shoppinglist.service

import ru.kdnsks.shoppinglist.dto.CategoryTotalDto
import ru.kdnsks.shoppinglist.dto.SummaryDto
import java.math.BigDecimal
import java.math.RoundingMode

/** Позиция списка в виде, нужном для расчёта итогов. */
data class ItemForSummary(
    val quantity: BigDecimal,
    val price: BigDecimal?,
    val purchased: Boolean,
    val categoryId: Long?,
    val categoryName: String,
    val categorySortOrder: Int,
)

/**
 * Бизнес-логика подсчёта суммы (истории US-10 – US-13).
 * Стоимость позиции = цена × количество, округлённая до копеек по правилу HALF_UP.
 * Позиции без цены в суммы не входят и считаются отдельно.
 */
object SummaryCalculator {
    const val NO_CATEGORY = "Без категории"

    fun lineTotal(price: BigDecimal?, quantity: BigDecimal): BigDecimal? =
        price?.multiply(quantity)?.setScale(2, RoundingMode.HALF_UP)

    fun calculate(listId: Long, budget: BigDecimal?, items: List<ItemForSummary>): SummaryDto {
        val zero = BigDecimal.ZERO.setScale(2)
        val priced = items.filter { it.price != null }
        val total = priced.fold(zero) { acc, i -> acc + lineTotal(i.price, i.quantity)!! }
        val inCart = priced.filter { it.purchased }.fold(zero) { acc, i -> acc + lineTotal(i.price, i.quantity)!! }

        val byCategory = priced
            .groupBy { Triple(it.categoryId, it.categoryName, it.categorySortOrder) }
            .map { (key, group) ->
                key to group.fold(zero) { acc, i -> acc + lineTotal(i.price, i.quantity)!! }
            }
            // «Без категории» всегда в конце, остальные — в порядке справочника
            .sortedWith(compareBy({ it.first.first == null }, { it.first.third }, { it.first.second }))
            .map { (key, sum) -> CategoryTotalDto(key.first, key.second, sum) }

        val budgetRemaining = budget?.setScale(2)?.subtract(total)
        return SummaryDto(
            listId = listId,
            itemsCount = items.size,
            itemsWithoutPrice = items.size - priced.size,
            total = total,
            inCart = inCart,
            remaining = total - inCart,
            budget = budget?.setScale(2),
            budgetRemaining = budgetRemaining,
            overBudget = budgetRemaining != null && budgetRemaining.signum() < 0,
            byCategory = byCategory,
        )
    }
}

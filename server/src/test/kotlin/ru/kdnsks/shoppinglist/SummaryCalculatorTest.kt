package ru.kdnsks.shoppinglist

import ru.kdnsks.shoppinglist.dto.ItemRequest
import ru.kdnsks.shoppinglist.service.ItemForSummary
import ru.kdnsks.shoppinglist.service.SummaryCalculator
import ru.kdnsks.shoppinglist.service.Validation
import ru.kdnsks.shoppinglist.util.ValidationException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SummaryCalculatorTest {
    private fun item(qty: String, price: String?, bought: Boolean = false, cat: Long? = 1, name: String = "Молочные продукты", order: Int = 2) =
        ItemForSummary(BigDecimal(qty), price?.let(::BigDecimal), bought, cat, name, order)

    @Test
    fun `итог равен сумме цена на количество с округлением до копеек`() {
        // 0.333 кг × 99.99 = 33.29667 → 33.30; 3 шт × 0.10 = 0.30
        val s = SummaryCalculator.calculate(1, null, listOf(item("0.333", "99.99"), item("3", "0.10")))
        assertEquals(BigDecimal("33.60"), s.total)
    }

    @Test
    fun `в корзине плюс осталось равно итогу`() {
        val s = SummaryCalculator.calculate(1, null, listOf(item("2", "89.90", bought = true), item("1", "120.00")))
        assertEquals(BigDecimal("179.80"), s.inCart)
        assertEquals(BigDecimal("120.00"), s.remaining)
        assertEquals(s.total, s.inCart + s.remaining)
    }

    @Test
    fun `позиции без цены не входят в итог и считаются отдельно`() {
        val s = SummaryCalculator.calculate(1, null, listOf(item("1", null), item("1", "50")))
        assertEquals(BigDecimal("50.00"), s.total)
        assertEquals(1, s.itemsWithoutPrice)
    }

    @Test
    fun `итоги по категориям в сумме дают общий итог, без категории в конце`() {
        val s = SummaryCalculator.calculate(1, null, listOf(
            item("1", "10", cat = null, name = SummaryCalculator.NO_CATEGORY, order = Int.MAX_VALUE),
            item("1", "20", cat = 1, name = "Овощи и фрукты", order = 1),
            item("2", "15", cat = 2, name = "Молочные продукты", order = 2),
        ))
        assertEquals(listOf("Овощи и фрукты", "Молочные продукты", SummaryCalculator.NO_CATEGORY), s.byCategory.map { it.categoryName })
        assertEquals(s.total, s.byCategory.fold(BigDecimal.ZERO.setScale(2)) { a, c -> a + c.total })
    }

    @Test
    fun `превышение бюджета определяется`() {
        val s = SummaryCalculator.calculate(1, BigDecimal("100"), listOf(item("3", "40")))
        assertTrue(s.overBudget)
        assertEquals(BigDecimal("-20.00"), s.budgetRemaining)
    }

    @Test
    fun `для штучного товара дробное количество запрещено`() {
        assertFailsWith<ValidationException> { Validation.item(ItemRequest(name = "Хлеб", quantity = BigDecimal("1.5"), unit = "шт")) }
        Validation.item(ItemRequest(name = "Яблоки", quantity = BigDecimal("1.5"), unit = "кг"))
    }
}

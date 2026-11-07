package ru.kdnsks.shoppinglist.service

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.kdnsks.shoppinglist.db.Categories
import ru.kdnsks.shoppinglist.db.ListItems
import ru.kdnsks.shoppinglist.db.ShoppingLists
import ru.kdnsks.shoppinglist.db.dbQuery
import ru.kdnsks.shoppinglist.dto.ItemDto
import ru.kdnsks.shoppinglist.dto.ItemRequest
import ru.kdnsks.shoppinglist.util.NotFoundException
import ru.kdnsks.shoppinglist.util.ValidationException
import java.time.OffsetDateTime

class ItemService {

    /** Позиции списка, сгруппированные по категориям; купленные — в конце своей категории (US-06, US-07). */
    suspend fun list(userId: Long, listId: Long): List<ItemDto> = dbQuery {
        ListService.findOwned(userId, listId)
        ListItems.join(Categories, JoinType.LEFT, ListItems.categoryId, Categories.id)
            .selectAll().where { ListItems.listId eq listId }
            .orderBy(ListItems.createdAt to SortOrder.ASC)
            .map { it to (it.getOrNull(Categories.sortOrder) ?: Int.MAX_VALUE) }
            .sortedWith(compareBy({ it.second }, { it.first[ListItems.isPurchased] }))
            .map { toDto(it.first) }
    }

    suspend fun create(userId: Long, listId: Long, raw: ItemRequest): ItemDto {
        val req = Validation.item(raw)
        return dbQuery {
            ListService.findOwned(userId, listId)
            checkCategory(userId, req.categoryId)
            val id = ListItems.insertAndGetId {
                it[ListItems.listId] = listId
                it[categoryId] = req.categoryId
                it[name] = req.name
                it[quantity] = req.quantity
                it[unit] = req.unit
                it[price] = req.price
                it[isPurchased] = false
                it[createdAt] = OffsetDateTime.now()
            }.value
            touchList(listId)
            toDto(findOwned(userId, id))
        }
    }

    suspend fun update(userId: Long, id: Long, raw: ItemRequest): ItemDto {
        val req = Validation.item(raw)
        return dbQuery {
            val item = findOwned(userId, id)
            checkCategory(userId, req.categoryId)
            ListItems.update({ ListItems.id eq id }) {
                it[categoryId] = req.categoryId
                it[name] = req.name
                it[quantity] = req.quantity
                it[unit] = req.unit
                it[price] = req.price
            }
            touchList(item[ListItems.listId].value)
            toDto(findOwned(userId, id))
        }
    }

    suspend fun setPurchased(userId: Long, id: Long, purchased: Boolean): ItemDto = dbQuery {
        val item = findOwned(userId, id)
        ListItems.update({ ListItems.id eq id }) { it[isPurchased] = purchased }
        touchList(item[ListItems.listId].value)
        toDto(findOwned(userId, id))
    }

    suspend fun delete(userId: Long, id: Long) = dbQuery {
        val item = findOwned(userId, id)
        ListItems.deleteWhere { ListItems.id eq id }
        touchList(item[ListItems.listId].value)
        Unit
    }

    /** Позиция доступна, только если её список принадлежит пользователю. */
    private fun findOwned(userId: Long, id: Long): ResultRow =
        ListItems.join(ShoppingLists, JoinType.INNER, ListItems.listId, ShoppingLists.id)
            .join(Categories, JoinType.LEFT, ListItems.categoryId, Categories.id)
            .selectAll().where { (ListItems.id eq id) and (ShoppingLists.userId eq userId) }
            .singleOrNull() ?: throw NotFoundException("Позиция не найдена")

    private fun checkCategory(userId: Long, categoryId: Long?) {
        if (categoryId == null) return
        val ok = Categories.selectAll().where { (Categories.id eq categoryId) and (Categories.userId eq userId) }.any()
        if (!ok) throw ValidationException("Категория с id=$categoryId не найдена")
    }

    private fun touchList(listId: Long) {
        ShoppingLists.update({ ShoppingLists.id eq listId }) { it[updatedAt] = OffsetDateTime.now() }
    }

    private fun toDto(r: ResultRow) = ItemDto(
        id = r[ListItems.id].value,
        listId = r[ListItems.listId].value,
        categoryId = r[ListItems.categoryId]?.value,
        categoryName = r.getOrNull(Categories.name) ?: SummaryCalculator.NO_CATEGORY,
        name = r[ListItems.name],
        quantity = r[ListItems.quantity].stripTrailingZeros(), // «2» вместо «2.000»
        unit = r[ListItems.unit],
        price = r[ListItems.price],
        lineTotal = SummaryCalculator.lineTotal(r[ListItems.price], r[ListItems.quantity]),
        purchased = r[ListItems.isPurchased],
    )
}

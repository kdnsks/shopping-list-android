package ru.kdnsks.shoppinglist.service

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.kdnsks.shoppinglist.db.Categories
import ru.kdnsks.shoppinglist.db.ListItems
import ru.kdnsks.shoppinglist.db.ShoppingLists
import ru.kdnsks.shoppinglist.db.dbQuery
import ru.kdnsks.shoppinglist.dto.ListDto
import ru.kdnsks.shoppinglist.dto.ListRequest
import ru.kdnsks.shoppinglist.dto.SummaryDto
import ru.kdnsks.shoppinglist.util.NotFoundException
import java.time.OffsetDateTime

class ListService {

    /** Все списки пользователя: позиции загружаются одним запросом, а не отдельным запросом на каждый список. */
    suspend fun list(userId: Long): List<ListDto> = dbQuery {
        val lists = ShoppingLists.selectAll().where { ShoppingLists.userId eq userId }
            .orderBy(ShoppingLists.createdAt to SortOrder.DESC)
            .toList()
        val itemsByList = itemsForSummary(lists.map { it[ShoppingLists.id].value })
        lists.map { toDto(it, itemsByList[it[ShoppingLists.id].value].orEmpty()) }
    }

    suspend fun get(userId: Long, id: Long): ListDto = dbQuery { toDto(findOwned(userId, id)) }

    suspend fun create(userId: Long, req: ListRequest): ListDto {
        val name = Validation.name(req.name, "Название списка", 100)
        val budget = Validation.money(req.budget, "Бюджет")
        return dbQuery {
            val now = OffsetDateTime.now()
            val id = ShoppingLists.insertAndGetId {
                it[ShoppingLists.userId] = userId
                it[ShoppingLists.name] = name
                it[ShoppingLists.budget] = budget
                it[createdAt] = now
                it[updatedAt] = now
            }.value
            toDto(findOwned(userId, id))
        }
    }

    suspend fun update(userId: Long, id: Long, req: ListRequest): ListDto {
        val name = Validation.name(req.name, "Название списка", 100)
        val budget = Validation.money(req.budget, "Бюджет")
        return dbQuery {
            findOwned(userId, id)
            ShoppingLists.update({ ShoppingLists.id eq id }) {
                it[ShoppingLists.name] = name
                it[ShoppingLists.budget] = budget
                it[updatedAt] = OffsetDateTime.now()
            }
            toDto(findOwned(userId, id))
        }
    }

    /** Позиции удаляются вместе со списком (ON DELETE CASCADE). */
    suspend fun delete(userId: Long, id: Long) = dbQuery {
        findOwned(userId, id)
        ShoppingLists.deleteWhere { (ShoppingLists.id eq id) and (ShoppingLists.userId eq userId) }
        Unit
    }

    /** Копия списка со всеми позициями; отметки «куплено» сбрасываются (US-03). */
    suspend fun copy(userId: Long, id: Long): ListDto = dbQuery {
        val source = findOwned(userId, id)
        val now = OffsetDateTime.now()
        val newId = ShoppingLists.insertAndGetId {
            it[ShoppingLists.userId] = userId
            it[name] = (source[ShoppingLists.name] + " (копия)").take(100)
            it[budget] = source[ShoppingLists.budget]
            it[createdAt] = now
            it[updatedAt] = now
        }.value
        val items = ListItems.selectAll().where { ListItems.listId eq id }.toList()
        ListItems.batchInsert(items) { row ->
            this[ListItems.listId] = newId
            this[ListItems.categoryId] = row[ListItems.categoryId]
            this[ListItems.name] = row[ListItems.name]
            this[ListItems.quantity] = row[ListItems.quantity]
            this[ListItems.unit] = row[ListItems.unit]
            this[ListItems.price] = row[ListItems.price]
            this[ListItems.isPurchased] = false
            this[ListItems.createdAt] = now
        }
        toDto(findOwned(userId, newId))
    }

    suspend fun summary(userId: Long, id: Long): SummaryDto = dbQuery {
        val list = findOwned(userId, id)
        SummaryCalculator.calculate(id, list[ShoppingLists.budget], itemsForSummary(listOf(id))[id].orEmpty())
    }

    private fun itemsForSummary(listIds: List<Long>): Map<Long, List<ItemForSummary>> =
        if (listIds.isEmpty()) emptyMap()
        else ListItems.join(Categories, JoinType.LEFT, ListItems.categoryId, Categories.id)
            .selectAll().where { ListItems.listId inList listIds }
            .groupBy({ it[ListItems.listId].value }) {
                ItemForSummary(
                    quantity = it[ListItems.quantity],
                    price = it[ListItems.price],
                    purchased = it[ListItems.isPurchased],
                    categoryId = it[ListItems.categoryId]?.value,
                    categoryName = it.getOrNull(Categories.name) ?: SummaryCalculator.NO_CATEGORY,
                    categorySortOrder = it.getOrNull(Categories.sortOrder) ?: Int.MAX_VALUE,
                )
            }

    private fun toDto(r: ResultRow, preloaded: List<ItemForSummary>? = null): ListDto {
        val id = r[ShoppingLists.id].value
        val items = preloaded ?: itemsForSummary(listOf(id))[id].orEmpty()
        return ListDto(
            id = id,
            name = r[ShoppingLists.name],
            budget = r[ShoppingLists.budget],
            itemsCount = items.size,
            total = SummaryCalculator.calculate(id, null, items).total,
            createdAt = r[ShoppingLists.createdAt].toString(),
            updatedAt = r[ShoppingLists.updatedAt].toString(),
        )
    }

    companion object {
        /** Список, принадлежащий пользователю; чужой список выглядит как несуществующий. */
        fun findOwned(userId: Long, id: Long): ResultRow =
            ShoppingLists.selectAll().where { (ShoppingLists.id eq id) and (ShoppingLists.userId eq userId) }.singleOrNull()
                ?: throw NotFoundException("Список не найден")
    }
}

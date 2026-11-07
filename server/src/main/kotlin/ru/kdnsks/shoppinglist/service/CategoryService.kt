package ru.kdnsks.shoppinglist.service

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import ru.kdnsks.shoppinglist.db.Categories
import ru.kdnsks.shoppinglist.db.dbQuery
import ru.kdnsks.shoppinglist.dto.CategoryDto
import ru.kdnsks.shoppinglist.dto.CategoryRequest
import ru.kdnsks.shoppinglist.util.ConflictException
import ru.kdnsks.shoppinglist.util.NotFoundException

class CategoryService {

    suspend fun list(userId: Long): List<CategoryDto> = dbQuery {
        Categories.selectAll().where { Categories.userId eq userId }
            .orderBy(Categories.sortOrder to SortOrder.ASC, Categories.name to SortOrder.ASC)
            .map(::toDto)
    }

    suspend fun create(userId: Long, req: CategoryRequest): CategoryDto {
        val name = Validation.name(req.name, "Название категории", 100)
        return dbQuery {
            ensureUnique(userId, name, exceptId = null)
            val order = req.sortOrder ?: ((Categories.select(Categories.sortOrder.max())
                .where { Categories.userId eq userId }.single()[Categories.sortOrder.max()] ?: 0) + 1)
            val id = Categories.insertAndGetId {
                it[Categories.userId] = userId
                it[Categories.name] = name
                it[sortOrder] = order
            }.value
            CategoryDto(id, name, order)
        }
    }

    suspend fun update(userId: Long, id: Long, req: CategoryRequest): CategoryDto {
        val name = Validation.name(req.name, "Название категории", 100)
        return dbQuery {
            val current = find(userId, id)
            ensureUnique(userId, name, exceptId = id)
            val order = req.sortOrder ?: current[Categories.sortOrder]
            Categories.update({ Categories.id eq id }) {
                it[Categories.name] = name
                it[sortOrder] = order
            }
            CategoryDto(id, name, order)
        }
    }

    /** Товары удалённой категории попадают в «Без категории» (ON DELETE SET NULL). */
    suspend fun delete(userId: Long, id: Long) = dbQuery {
        find(userId, id)
        Categories.deleteWhere { (Categories.id eq id) and (Categories.userId eq userId) }
        Unit
    }

    private fun find(userId: Long, id: Long): ResultRow =
        Categories.selectAll().where { (Categories.id eq id) and (Categories.userId eq userId) }.singleOrNull()
            ?: throw NotFoundException("Категория не найдена")

    private fun ensureUnique(userId: Long, name: String, exceptId: Long?) {
        val clash = Categories.selectAll().where { (Categories.userId eq userId) and (Categories.name eq name) }
            .any { it[Categories.id].value != exceptId }
        if (clash) throw ConflictException("Категория «$name» уже существует")
    }

    private fun toDto(r: ResultRow) = CategoryDto(r[Categories.id].value, r[Categories.name], r[Categories.sortOrder])
}

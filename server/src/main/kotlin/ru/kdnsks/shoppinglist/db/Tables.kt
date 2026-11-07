package ru.kdnsks.shoppinglist.db

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Users : LongIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val createdAt = timestampWithTimeZone("created_at")
}

object Categories : LongIdTable("categories") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val sortOrder = integer("sort_order")
}

object ShoppingLists : LongIdTable("shopping_lists") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val budget = decimal("budget", 12, 2).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object ListItems : LongIdTable("list_items") {
    val listId = reference("list_id", ShoppingLists, onDelete = ReferenceOption.CASCADE)
    val categoryId = optReference("category_id", Categories, onDelete = ReferenceOption.SET_NULL)
    val name = varchar("name", 150)
    val quantity = decimal("quantity", 10, 3)
    val unit = varchar("unit", 10)
    val price = decimal("price", 12, 2).nullable()
    val isPurchased = bool("is_purchased")
    val createdAt = timestampWithTimeZone("created_at")
}

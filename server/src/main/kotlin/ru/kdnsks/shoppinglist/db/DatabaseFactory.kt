package ru.kdnsks.shoppinglist.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {
    fun init(url: String, user: String, password: String) {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
        })
        // Миграции схемы БД (src/main/resources/db/migration) применяются при старте сервера
        Flyway.configure().dataSource(dataSource).load().migrate()
        Database.connect(dataSource)
    }
}

/** Выполняет блок в транзакции БД, не блокируя потоки обработки запросов. */
suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

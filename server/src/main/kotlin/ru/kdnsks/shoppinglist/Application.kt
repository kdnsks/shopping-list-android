package ru.kdnsks.shoppinglist

import io.ktor.server.application.Application
import ru.kdnsks.shoppinglist.auth.JwtConfig
import ru.kdnsks.shoppinglist.db.DatabaseFactory
import ru.kdnsks.shoppinglist.plugins.configureMonitoring
import ru.kdnsks.shoppinglist.plugins.configureRouting
import ru.kdnsks.shoppinglist.plugins.configureSecurity
import ru.kdnsks.shoppinglist.plugins.configureSerialization
import ru.kdnsks.shoppinglist.plugins.configureStatusPages

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val config = environment.config
    DatabaseFactory.init(
        url = config.property("database.url").getString(),
        user = config.property("database.user").getString(),
        password = config.property("database.password").getString(),
    )
    val jwt = JwtConfig(
        secret = config.property("jwt.secret").getString(),
        issuer = config.property("jwt.issuer").getString(),
        audience = config.property("jwt.audience").getString(),
        realm = config.property("jwt.realm").getString(),
        ttlHours = config.property("jwt.ttlHours").getString().toLong(),
    )

    configureMonitoring()
    configureSerialization()
    configureStatusPages()
    configureSecurity(jwt)
    configureRouting(jwt)
}

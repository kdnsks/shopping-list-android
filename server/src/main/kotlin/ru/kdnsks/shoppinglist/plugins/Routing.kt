package ru.kdnsks.shoppinglist.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import ru.kdnsks.shoppinglist.auth.JwtConfig
import ru.kdnsks.shoppinglist.routes.authRoutes
import ru.kdnsks.shoppinglist.routes.categoryRoutes
import ru.kdnsks.shoppinglist.routes.itemRoutes
import ru.kdnsks.shoppinglist.routes.listRoutes
import ru.kdnsks.shoppinglist.service.AuthService
import ru.kdnsks.shoppinglist.service.CategoryService
import ru.kdnsks.shoppinglist.service.ItemService
import ru.kdnsks.shoppinglist.service.ListService

@Serializable
data class HealthResponse(val status: String)

fun Application.configureRouting(jwt: JwtConfig) {
    val authService = AuthService(jwt)
    val categoryService = CategoryService()
    val listService = ListService()
    val itemService = ItemService()

    routing {
        // Документация API: http://localhost:8080/swagger
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/api/health") { call.respond(HttpStatusCode.OK, HealthResponse("ok")) }

        authRoutes(authService)
        authenticate("auth-jwt") {
            categoryRoutes(categoryService)
            listRoutes(listService)
            itemRoutes(itemService)
        }
    }
}

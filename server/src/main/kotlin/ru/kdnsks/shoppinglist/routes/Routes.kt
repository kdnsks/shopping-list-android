package ru.kdnsks.shoppinglist.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ru.kdnsks.shoppinglist.auth.userId
import ru.kdnsks.shoppinglist.dto.CategoryRequest
import ru.kdnsks.shoppinglist.dto.ItemRequest
import ru.kdnsks.shoppinglist.dto.ListRequest
import ru.kdnsks.shoppinglist.dto.LoginRequest
import ru.kdnsks.shoppinglist.dto.PurchasedRequest
import ru.kdnsks.shoppinglist.dto.RegisterRequest
import ru.kdnsks.shoppinglist.service.AuthService
import ru.kdnsks.shoppinglist.service.CategoryService
import ru.kdnsks.shoppinglist.service.ItemService
import ru.kdnsks.shoppinglist.service.ListService
import ru.kdnsks.shoppinglist.util.ValidationException

private fun RoutingCall.idParam(name: String = "id"): Long =
    parameters[name]?.toLongOrNull() ?: throw ValidationException("Некорректный идентификатор: $name")

fun Route.authRoutes(service: AuthService) = route("/api/auth") {
    post("/register") { call.respond(HttpStatusCode.Created, service.register(call.receive<RegisterRequest>())) }
    post("/login") { call.respond(service.login(call.receive<LoginRequest>())) }
    authenticate("auth-jwt") {
        get("/me") { call.respond(service.me(call.userId)) }
    }
}

fun Route.categoryRoutes(service: CategoryService) = route("/api/categories") {
    get { call.respond(service.list(call.userId)) }
    post { call.respond(HttpStatusCode.Created, service.create(call.userId, call.receive<CategoryRequest>())) }
    put("/{id}") { call.respond(service.update(call.userId, call.idParam(), call.receive<CategoryRequest>())) }
    delete("/{id}") {
        service.delete(call.userId, call.idParam())
        call.respond(HttpStatusCode.NoContent)
    }
}

fun Route.listRoutes(service: ListService) = route("/api/lists") {
    get { call.respond(service.list(call.userId)) }
    post { call.respond(HttpStatusCode.Created, service.create(call.userId, call.receive<ListRequest>())) }
    get("/{id}") { call.respond(service.get(call.userId, call.idParam())) }
    put("/{id}") { call.respond(service.update(call.userId, call.idParam(), call.receive<ListRequest>())) }
    delete("/{id}") {
        service.delete(call.userId, call.idParam())
        call.respond(HttpStatusCode.NoContent)
    }
    post("/{id}/copy") { call.respond(HttpStatusCode.Created, service.copy(call.userId, call.idParam())) }
    get("/{id}/summary") { call.respond(service.summary(call.userId, call.idParam())) }
}

fun Route.itemRoutes(service: ItemService) {
    route("/api/lists/{listId}/items") {
        get { call.respond(service.list(call.userId, call.idParam("listId"))) }
        post { call.respond(HttpStatusCode.Created, service.create(call.userId, call.idParam("listId"), call.receive<ItemRequest>())) }
    }
    route("/api/items/{id}") {
        put { call.respond(service.update(call.userId, call.idParam(), call.receive<ItemRequest>())) }
        patch("/purchased") { call.respond(service.setPurchased(call.userId, call.idParam(), call.receive<PurchasedRequest>().purchased)) }
        delete {
            service.delete(call.userId, call.idParam())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

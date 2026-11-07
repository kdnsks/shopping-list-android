# shopping-list-android

Мобильное приложение «Список покупок» для Android: списки с группировкой по категориям и подсчётом итоговой суммы. Учебная практика РТУ МИРЭА, вариант № 12.

## Архитектура

| Часть | Технологии |
|---|---|
| Сервер (`server/`) | Kotlin, Ktor, Exposed, Flyway, JWT |
| База данных | PostgreSQL 16 |
| Запуск | Docker Compose |
| Клиент | Android, Kotlin, Jetpack Compose (организационный этап) |

## Запуск сервера

1. Скопируйте `.env.example` в `.env` и задайте `JWT_SECRET`.
2. Выполните:
   ```
   docker compose up -d --build
   docker compose ps
   ```
3. Проверка: http://localhost:8080/api/health
4. Документация API (Swagger UI): http://localhost:8080/swagger
5. Коллекция запросов Postman: `postman/shopping-list.postman_collection.json`

База данных доступна на `localhost:5433` (база `shopping_list`, пользователь `shop`).

## API

| Метод | Адрес | Назначение |
|---|---|---|
| GET | /api/health | проверка работы сервера |
| POST | /api/auth/register | регистрация |
| POST | /api/auth/login | вход, получение JWT-токена |
| GET | /api/auth/me | текущий пользователь |
| GET | /api/categories | категории пользователя |
| POST | /api/categories | создать категорию |
| PUT | /api/categories/{id} | изменить категорию |
| DELETE | /api/categories/{id} | удалить категорию |
| GET | /api/lists | списки пользователя |
| POST | /api/lists | создать список |
| GET | /api/lists/{id} | получить список |
| PUT | /api/lists/{id} | изменить список |
| DELETE | /api/lists/{id} | удалить список |
| POST | /api/lists/{id}/copy | копировать список |
| GET | /api/lists/{id}/summary | итоги: сумма, «в корзине», «осталось», по категориям |
| GET | /api/lists/{listId}/items | позиции списка |
| POST | /api/lists/{listId}/items | добавить товар |
| PUT | /api/items/{id} | изменить товар |
| PATCH | /api/items/{id}/purchased | отметить «куплено» |
| DELETE | /api/items/{id} | удалить товар |

Все адреса, кроме `/api/health`, `/api/auth/register` и `/api/auth/login`, требуют заголовок `Authorization: Bearer <токен>`.

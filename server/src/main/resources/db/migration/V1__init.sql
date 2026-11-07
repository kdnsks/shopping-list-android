-- Схема базы данных приложения «Список покупок»

-- Пользователи
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Категории товаров (у каждого пользователя свой справочник)
CREATE TABLE categories (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name)
);

-- Списки покупок
CREATE TABLE shopping_lists (
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100)  NOT NULL,
    budget     NUMERIC(12, 2) CHECK (budget >= 0),
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Позиции (товары) списка
CREATE TABLE list_items (
    id           BIGSERIAL      PRIMARY KEY,
    list_id      BIGINT         NOT NULL REFERENCES shopping_lists (id) ON DELETE CASCADE,
    category_id  BIGINT         REFERENCES categories (id) ON DELETE SET NULL,
    name         VARCHAR(150)   NOT NULL,
    quantity     NUMERIC(10, 3) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit         VARCHAR(10)    NOT NULL DEFAULT 'шт',
    price        NUMERIC(12, 2) CHECK (price >= 0),
    is_purchased BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_categories_user   ON categories (user_id);
CREATE INDEX idx_lists_user        ON shopping_lists (user_id);
CREATE INDEX idx_items_list        ON list_items (list_id);
CREATE INDEX idx_items_category    ON list_items (category_id);

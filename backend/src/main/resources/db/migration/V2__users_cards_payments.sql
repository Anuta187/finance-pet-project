-- v2: пользователи и роли, категории расходов, карты, автоплатежи.

-- Пользователи. Пароль хранится только в виде BCrypt-хеша.
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(100)             NOT NULL,
    password_hash VARCHAR(100)             NOT NULL,
    full_name     VARCHAR(100)             NOT NULL,
    role          VARCHAR(16)              NOT NULL,
    blocked       BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CLIENT', 'ADMIN'))
);

-- Счёт принадлежит пользователю. NULL допускается только для счетов из v1:
-- при старте приложения они передаются демо-клиенту (SeedService).
ALTER TABLE accounts ADD COLUMN user_id BIGINT REFERENCES users (id);
CREATE INDEX idx_accounts_user ON accounts (user_id);

-- Справочник категорий для «Оплаты услуг».
CREATE TABLE categories (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(32) NOT NULL,
    name       VARCHAR(50) NOT NULL,
    color      VARCHAR(7)  NOT NULL,
    sort_order INT         NOT NULL,
    CONSTRAINT uq_categories_code UNIQUE (code)
);

INSERT INTO categories (code, name, color, sort_order) VALUES
    ('MOBILE',        'Мобильная связь',         '#6366F1', 1),
    ('INTERNET',      'Интернет и ТВ',           '#0EA5E9', 2),
    ('UTILITIES',     'ЖКХ',                     '#F59E0B', 3),
    ('TRANSPORT',     'Транспорт',               '#10B981', 4),
    ('EDUCATION',     'Образование',             '#8B5CF6', 5),
    ('ENTERTAINMENT', 'Развлечения и подписки',  '#EC4899', 6),
    ('HEALTH',        'Здоровье',                '#EF4444', 7),
    ('OTHER',         'Прочее',                  '#64748B', 8);

-- Карты, привязанные к счёту. Полный номер не хранится: только первые 4 и последние 4 цифры.
CREATE TABLE cards (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id     BIGINT                   NOT NULL REFERENCES accounts (id),
    bin4           VARCHAR(4)               NOT NULL,
    last4          VARCHAR(4)               NOT NULL,
    payment_system VARCHAR(16)              NOT NULL,
    holder_name    VARCHAR(100)             NOT NULL,
    expiry_month   INT                      NOT NULL CHECK (expiry_month BETWEEN 1 AND 12),
    expiry_year    INT                      NOT NULL,
    status         VARCHAR(16)              NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_cards_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
);
CREATE INDEX idx_cards_account ON cards (account_id);

-- Автоплатежи: регулярная «Оплата услуг» по расписанию.
CREATE TABLE autopayments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id    BIGINT                   NOT NULL REFERENCES accounts (id),
    card_id       BIGINT REFERENCES cards (id),
    category_id   BIGINT                   NOT NULL REFERENCES categories (id),
    name          VARCHAR(100)             NOT NULL,
    amount_kop    BIGINT                   NOT NULL CHECK (amount_kop > 0),
    period        VARCHAR(16)              NOT NULL,
    start_date    DATE                     NOT NULL,
    next_run_date DATE                     NOT NULL,
    run_count     INT                      NOT NULL DEFAULT 0,
    active        BOOLEAN                  NOT NULL DEFAULT TRUE,
    last_run_at   TIMESTAMP WITH TIME ZONE,
    last_status   VARCHAR(16),
    last_error    VARCHAR(200),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_autopayments_period CHECK (period IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT ck_autopayments_status CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'FAILED'))
);
CREATE INDEX idx_autopayments_due ON autopayments (active, next_run_date);

-- Новые ссылки в журнале операций (заполняются для type = PAYMENT).
ALTER TABLE operations ADD COLUMN category_id BIGINT REFERENCES categories (id);
ALTER TABLE operations ADD COLUMN card_id BIGINT REFERENCES cards (id);
ALTER TABLE operations ADD COLUMN autopayment_id BIGINT REFERENCES autopayments (id);

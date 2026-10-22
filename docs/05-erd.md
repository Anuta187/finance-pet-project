# Модель данных

Схему создают миграции Flyway: `V1__init.sql`, `V2__users_cards_payments.sql` и Java-миграция `V3__Allow_payment_operations`. Hibernate схему не меняет, только сверяет с сущностями. База H2: файл `backend/data/finance.mv.db`, в тестовом профиле — в памяти.

```mermaid
erDiagram
    USERS ||--o{ ACCOUNTS : "владеет"
    ACCOUNTS ||--o{ CARDS : "карты счёта"
    ACCOUNTS ||--o{ OPERATIONS : "списание и зачисление"
    ACCOUNTS ||--o{ AUTOPAYMENTS : "списание"
    CARDS |o--o{ AUTOPAYMENTS : "с карты"
    CARDS |o--o{ OPERATIONS : "оплата картой"
    CATEGORIES ||--o{ OPERATIONS : "категория"
    CATEGORIES ||--o{ AUTOPAYMENTS : "категория"
    AUTOPAYMENTS |o--o{ OPERATIONS : "создал"

    USERS {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR full_name
        VARCHAR role
        BOOLEAN blocked
        TIMESTAMPTZ created_at
    }
    ACCOUNTS {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR name
        VARCHAR currency
        BIGINT balance_kop
        TIMESTAMPTZ created_at
    }
    CARDS {
        BIGINT id PK
        BIGINT account_id FK
        VARCHAR bin4
        VARCHAR last4
        VARCHAR payment_system
        VARCHAR holder_name
        INT expiry_month
        INT expiry_year
        VARCHAR status
        TIMESTAMPTZ created_at
    }
    CATEGORIES {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR name
        VARCHAR color
        INT sort_order
    }
    OPERATIONS {
        BIGINT id PK
        VARCHAR type
        BIGINT from_account_id FK
        BIGINT to_account_id FK
        BIGINT amount_kop
        VARCHAR comment
        BIGINT category_id FK
        BIGINT card_id FK
        BIGINT autopayment_id FK
        TIMESTAMPTZ created_at
    }
    AUTOPAYMENTS {
        BIGINT id PK
        BIGINT account_id FK
        BIGINT card_id FK
        BIGINT category_id FK
        VARCHAR name
        BIGINT amount_kop
        VARCHAR period
        DATE start_date
        DATE next_run_date
        INT run_count
        BOOLEAN active
        TIMESTAMPTZ last_run_at
        VARCHAR last_status
        VARCHAR last_error
        TIMESTAMPTZ created_at
    }
```

## Таблицы

- `users` — пользователи. Email хранится в нижнем регистре, пароль только как BCrypt-хеш. Роль CLIENT или ADMIN.
- `accounts` — счета. Остаток в копейках, в базе стоит проверка, что он не меньше 0.
- `cards` — карты. Хранятся только первые и последние 4 цифры номера, маска собирается при выдаче: `2200 **** **** 1234`. Статус ACTIVE или BLOCKED.
- `categories` — 8 категорий оплаты, заводятся миграцией V2: связь, интернет, ЖКХ, транспорт, образование, развлечения, здоровье и прочее.
- `operations` — журнал операций. Тип OPENING, TRANSFER или PAYMENT. У пополнения нет счёта списания, у платежа нет счёта зачисления.
- `autopayments` — автоплатежи. Период DAILY, WEEKLY или MONTHLY, дата следующего списания и результат последнего запуска.

## Решения

- Деньги хранятся в копейках как целые числа, чтобы не было ошибок округления. API принимает и отдаёт рубли.
- Владелец указан только у счёта. Карты, операции и автоплатежи принадлежат клиенту через счёт.
- При переводе и оплате строки счетов блокируются через `SELECT ... FOR UPDATE`, всегда в порядке возрастания id, чтобы не было дедлоков.
- Текущее время берётся из бина `Clock`, поэтому в тестах его можно сдвигать.

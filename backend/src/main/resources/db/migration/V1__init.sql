-- Счета. Деньги храним в копейках (BIGINT), чтобы избежать ошибок округления.
CREATE TABLE accounts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)              NOT NULL CHECK (CHAR_LENGTH(name) BETWEEN 1 AND 50),
    currency    VARCHAR(3)               NOT NULL DEFAULT 'RUB',
    balance_kop BIGINT                   NOT NULL DEFAULT 0 CHECK (balance_kop >= 0),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Журнал движения денег: OPENING (начальный остаток) и TRANSFER (перевод).
CREATE TABLE operations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(16)              NOT NULL CHECK (type IN ('OPENING', 'TRANSFER')),
    from_account_id BIGINT REFERENCES accounts (id),
    to_account_id   BIGINT                   NOT NULL REFERENCES accounts (id),
    amount_kop      BIGINT                   NOT NULL CHECK (amount_kop > 0),
    comment         VARCHAR(200),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_operations_from ON operations (from_account_id, created_at);
CREATE INDEX idx_operations_to ON operations (to_account_id, created_at);

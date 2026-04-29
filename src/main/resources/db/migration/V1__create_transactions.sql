CREATE TABLE transactions (
    id                   BIGSERIAL PRIMARY KEY,
    transaction_id       UUID          NOT NULL UNIQUE,
    transaction_datetime TIMESTAMPTZ   NOT NULL,
    date                 DATE          NOT NULL,
    account_type         VARCHAR(20)   NOT NULL,
    category             VARCHAR(20)   NOT NULL,
    type                 VARCHAR(40)   NOT NULL,
    amount               NUMERIC(20,6) NOT NULL,
    fee                  NUMERIC(20,6),
    tax                  NUMERIC(20,6),
    currency             VARCHAR(3)    NOT NULL,
    description          TEXT,

    -- AssetInfo
    asset_class          VARCHAR(20),
    asset_name           VARCHAR(255),
    asset_symbol         VARCHAR(50),
    asset_shares         NUMERIC(20,10),
    asset_price          NUMERIC(20,6),

    -- FxInfo
    fx_original_amount   NUMERIC(20,6),
    fx_original_currency VARCHAR(3),
    fx_rate              NUMERIC(20,10),

    -- CounterpartyInfo
    counterparty_name    VARCHAR(255),
    counterparty_iban    VARCHAR(34),
    payment_reference    TEXT,

    -- MccInfo
    mcc_code             VARCHAR(4)
);

CREATE INDEX idx_transactions_date           ON transactions (date);
CREATE INDEX idx_transactions_category       ON transactions (category);
CREATE INDEX idx_transactions_type           ON transactions (type);
CREATE INDEX idx_transactions_transaction_id ON transactions (transaction_id);

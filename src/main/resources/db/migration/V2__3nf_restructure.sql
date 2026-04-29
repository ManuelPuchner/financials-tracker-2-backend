-- Extract assets (symbol determines name and asset_class — transitive dependency removed)
CREATE TABLE assets (
    id          BIGSERIAL    PRIMARY KEY,
    symbol      VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    asset_class VARCHAR(20)  NOT NULL
);

INSERT INTO assets (symbol, name, asset_class)
SELECT DISTINCT
    asset_symbol,
    COALESCE(NULLIF(asset_name, ''), asset_symbol),
    asset_class
FROM transactions
WHERE asset_symbol IS NOT NULL AND asset_symbol != ''
  AND asset_class  IS NOT NULL AND asset_class  != '';

-- Extract counterparties (iban determines name — transitive dependency removed)
CREATE TABLE counterparties (
    id   BIGSERIAL   PRIMARY KEY,
    iban VARCHAR(34) NOT NULL UNIQUE,
    name VARCHAR(255)
);

INSERT INTO counterparties (iban, name)
SELECT DISTINCT
    counterparty_iban,
    NULLIF(counterparty_name, '')
FROM transactions
WHERE counterparty_iban IS NOT NULL AND counterparty_iban != '';

-- Add FK columns and merchant_name to transactions
ALTER TABLE transactions
    ADD COLUMN asset_id        BIGINT REFERENCES assets(id),
    ADD COLUMN counterparty_id BIGINT REFERENCES counterparties(id),
    ADD COLUMN merchant_name   VARCHAR(255);

-- Back-fill FK columns
UPDATE transactions t
SET asset_id = a.id
FROM assets a
WHERE t.asset_symbol = a.symbol;

UPDATE transactions t
SET counterparty_id = c.id
FROM counterparties c
WHERE t.counterparty_iban = c.iban;

-- Save merchant name for card transactions before dropping asset_name
UPDATE transactions
SET merchant_name = NULLIF(asset_name, '')
WHERE type IN ('CARD_TRANSACTION', 'CARD_TRANSACTION_INTERNATIONAL');

-- Drop columns whose data has been normalised into the new tables
ALTER TABLE transactions
    DROP COLUMN asset_class,
    DROP COLUMN asset_name,
    DROP COLUMN asset_symbol,
    DROP COLUMN counterparty_name,
    DROP COLUMN counterparty_iban;

CREATE INDEX idx_transactions_asset_id        ON transactions (asset_id);
CREATE INDEX idx_transactions_counterparty_id ON transactions (counterparty_id);

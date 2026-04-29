-- Source discriminator: identifies which platform/bank the transaction came from
ALTER TABLE transactions ADD COLUMN source VARCHAR(30);
UPDATE transactions SET source = 'TRADE_REPUBLIC' WHERE source IS NULL;
ALTER TABLE transactions ALTER COLUMN source SET NOT NULL;
CREATE INDEX idx_transactions_source ON transactions(source);

-- Sparkasse-specific columns (all nullable; NULL for Trade Republic rows)
ALTER TABLE transactions
    ADD COLUMN own_account_iban  VARCHAR(34),
    ADD COLUMN own_account_name  VARCHAR(255),
    ADD COLUMN sepa_mandate_id   VARCHAR(255),
    ADD COLUMN sepa_creditor_id  VARCHAR(255),
    ADD COLUMN payment_method    VARCHAR(255),
    ADD COLUMN note              TEXT;

-- User-defined categories
CREATE TABLE user_categories (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20),
    icon       VARCHAR(50),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
-- Case-insensitive unique name: "Food" and "food" are the same category
CREATE UNIQUE INDEX uq_user_categories_name_lower ON user_categories (LOWER(name));

-- MCC code -> user category mapping (ON DELETE SET NULL: deleting a category doesn't destroy the MCC record)
ALTER TABLE mcc_codes
    ADD COLUMN user_category_id BIGINT REFERENCES user_categories(id) ON DELETE SET NULL;

-- Transaction -> user category (ON DELETE SET NULL: deleting a category un-tags, doesn't delete transactions)
ALTER TABLE transactions
    ADD COLUMN user_category_id BIGINT REFERENCES user_categories(id) ON DELETE SET NULL;
CREATE INDEX idx_transactions_user_category ON transactions(user_category_id);

-- Case-insensitive search indexes for merchant search endpoint
CREATE INDEX idx_transactions_merchant_name_lower ON transactions (LOWER(merchant_name));
CREATE INDEX idx_counterparties_name_lower ON counterparties (LOWER(name));

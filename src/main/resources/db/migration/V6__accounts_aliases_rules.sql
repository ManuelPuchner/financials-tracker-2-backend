-- 1) accounts
CREATE TABLE accounts (
    id               BIGSERIAL    PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    color            VARCHAR(20),
    icon             VARCHAR(50),
    source           VARCHAR(30)  NOT NULL,
    own_account_iban VARCHAR(34),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
-- NULLS NOT DISTINCT requires PG15+; ensure production runs PG15 before this migration.
CREATE UNIQUE INDEX uq_accounts_source_iban
    ON accounts (source, own_account_iban) NULLS NOT DISTINCT;
CREATE INDEX idx_accounts_source ON accounts (source);

-- 2) transactions.account_id (with backfill from existing source/iban combos)
ALTER TABLE transactions ADD COLUMN account_id BIGINT;

INSERT INTO accounts (name, source, own_account_iban)
SELECT
    CASE
        WHEN source = 'TRADE_REPUBLIC' THEN 'Trade Republic'
        WHEN source = 'SPARKASSE'      THEN 'Sparkasse ' || COALESCE(RIGHT(own_account_iban, 4), '????')
        ELSE source
    END,
    source,
    own_account_iban
FROM transactions
GROUP BY source, own_account_iban;

UPDATE transactions t
SET account_id = a.id
FROM accounts a
WHERE a.source = t.source
  AND a.own_account_iban IS NOT DISTINCT FROM t.own_account_iban;

ALTER TABLE transactions
    ALTER COLUMN account_id SET NOT NULL,
    ADD CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT;
CREATE INDEX idx_transactions_account_id ON transactions (account_id);

-- 3) merchant_aliases
CREATE TABLE merchant_aliases (
    id             BIGSERIAL    PRIMARY KEY,
    pattern        VARCHAR(500) NOT NULL,
    canonical_name VARCHAR(255) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_merchant_aliases_pattern ON merchant_aliases (pattern);

-- 4) sparkasse_rules
CREATE TABLE sparkasse_rules (
    id               BIGSERIAL    PRIMARY KEY,
    pattern          VARCHAR(500) NOT NULL,
    target_field     VARCHAR(20)  NOT NULL,
    user_category_id BIGINT       NOT NULL REFERENCES user_categories(id) ON DELETE CASCADE,
    priority         INTEGER      NOT NULL DEFAULT 100,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sparkasse_rules_target
        CHECK (target_field IN ('PARTNER_NAME', 'REFERENCE', 'BOTH'))
);
CREATE INDEX idx_sparkasse_rules_priority ON sparkasse_rules (priority, id);

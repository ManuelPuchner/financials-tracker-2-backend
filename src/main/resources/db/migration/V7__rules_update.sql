-- Fix: add COUNTERPARTY_NAME to sparkasse_rules.target_field constraint (added in code but not yet in DB)
ALTER TABLE sparkasse_rules DROP CONSTRAINT IF EXISTS chk_sparkasse_rules_target;
ALTER TABLE sparkasse_rules ADD CONSTRAINT chk_sparkasse_rules_target
    CHECK (target_field IN ('PARTNER_NAME', 'COUNTERPARTY_NAME', 'REFERENCE', 'BOTH'));

-- Asset rules: per-asset-class regex rules that assign a UserCategory to investment transactions
CREATE TABLE asset_rules (
    id               BIGSERIAL    PRIMARY KEY,
    pattern          VARCHAR(500) NOT NULL,
    target_field     VARCHAR(20)  NOT NULL,
    asset_class      VARCHAR(20)  NOT NULL,
    user_category_id BIGINT       NOT NULL REFERENCES user_categories(id) ON DELETE CASCADE,
    priority         INTEGER      NOT NULL DEFAULT 100,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_asset_rules_target
        CHECK (target_field IN ('SYMBOL', 'NAME', 'BOTH')),
    CONSTRAINT chk_asset_rules_class
        CHECK (asset_class IN ('STOCK', 'FUND', 'DERIVATIVE', 'CRYPTO'))
);
CREATE INDEX idx_asset_rules_class_priority ON asset_rules (asset_class, priority, id);

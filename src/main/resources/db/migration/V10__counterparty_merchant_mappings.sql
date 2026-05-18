CREATE TABLE counterparty_merchant_mappings (
    id BIGSERIAL PRIMARY KEY,
    counterparty_id BIGINT NOT NULL UNIQUE REFERENCES counterparties(id) ON DELETE CASCADE,
    merchant_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cmm_counterparty_id ON counterparty_merchant_mappings(counterparty_id);

ALTER TABLE transactions ADD COLUMN raw_merchant_name VARCHAR(255);
UPDATE transactions SET raw_merchant_name = merchant_name WHERE merchant_name IS NOT NULL;
CREATE INDEX idx_transactions_raw_merchant_name ON transactions(raw_merchant_name);

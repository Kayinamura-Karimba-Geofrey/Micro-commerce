-- Owner of the payment, used to stop users reading or paying other users' orders.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS user_id BIGINT;

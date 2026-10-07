-- Kafka delivers at least once, so the same order-placed event can arrive twice.
-- One payment per order makes the consumer idempotent. Earlier duplicates can only
-- be PENDING (paying them failed on the ambiguous lookup), so keep the oldest one.
DELETE FROM payments p USING payments d WHERE p.order_id = d.order_id AND p.id > d.id;
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_order_id ON payments (order_id);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    amount DECIMAL(19, 2),
    status VARCHAR(50),
    transaction_id VARCHAR(255),
    payment_date TIMESTAMP
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255),
    user_id BIGINT,
    total_amount DECIMAL(19, 2),
    status VARCHAR(50),
    created_at TIMESTAMP
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT,
    price DECIMAL(19, 2),
    quantity INTEGER,
    order_id BIGINT REFERENCES orders(id)
);
